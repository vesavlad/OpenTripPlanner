package org.opentripplanner.transit.model.network;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity2;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.util.geometry.CompactLineStringUtils;
import org.opentripplanner.util.geometry.GeometryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO OTP2 instances of this class are still mutable after construction with a builder, this will be refactored in a subsequent step
/**
 * Represents a group of trips on a route, with the same direction id that all call at the same
 * sequence of stops. For each stop, there is a list of departure times, running times, arrival
 * times, dwell times, and wheelchair accessibility information (one of each of these per trip per
 * stop). Trips are assumed to be non-overtaking, so that an earlier trip never arrives after a
 * later trip.
 * <p>
 * This is called a JOURNEY_PATTERN in the Transmodel vocabulary. However, GTFS calls a Transmodel
 * JOURNEY a "trip", thus TripPattern.
 * <p>
 * The {@code id} is a unique identifier for this trip pattern. For GTFS feeds this is generally
 * generated in the format FeedId:Agency:RouteId:DirectionId:PatternNumber. For NeTEx the
 * JourneyPattern id is used.
 */
public final class TripPattern
  extends TransitEntity2<TripPattern, TripPatternBuilder>
  implements Cloneable, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(TripPattern.class);

  private static final long serialVersionUID = 1;
  private final Route route;
  /**
   * The stop-pattern help us reuse the same stops in several trip-patterns; Hence saving memory.
   * The field should not be accessible outside the class, and all access is done through method
   * delegation, like the {@link #numberOfStops()} and {@link #canBoard(int)} methods.
   */
  private final StopPattern stopPattern;
  private final Timetable scheduledTimetable;
  private String name;
  /**
   * Geometries of each inter-stop segment of the tripPattern.
   */
  private byte[][] hopGeometries;

  /**
   * The original TripPattern this replaces at least for one modified trip.
   */
  private TripPattern originalTripPattern = null;

  /**
   * Has the TripPattern been created by a real-time update.
   */
  private final boolean createdByRealtimeUpdater;

  // TODO MOVE codes INTO Timetable or TripTimes
  private BitSet services;

  public TripPattern(TripPatternBuilder builder) {
    super(builder.getId());
    this.name = builder.getName();
    this.route = builder.getRoute();
    this.stopPattern = requireNonNull(builder.getStopPattern());
    this.createdByRealtimeUpdater = builder.isCreatedByRealtimeUpdate();

    this.scheduledTimetable =
      builder.getScheduledTimetable() != null
        ? builder.getScheduledTimetable()
        : new Timetable(this);
    this.scheduledTimetable.setServiceCodes(builder.getServiceCodes());

    this.services = builder.getServices();
    this.originalTripPattern = builder.getOriginalTripPattern();

    if (builder.getServiceCodes() != null) {
      setServiceCodes(builder.getServiceCodes());
    }
  }

  public static TripPatternBuilder of(@Nonnull FeedScopedId id) {
    return new TripPatternBuilder(id);
  }

  /**
   * Static method that creates unique human-readable names for a collection of TableTripPatterns.
   * Perhaps this should be in TripPattern, and apply to Frequency patterns as well. TODO: resolve
   * this question: can a frequency and table pattern have the same stoppattern? If so should they
   * have the same "unique" name?
   * <p>
   * The names should be dataset unique, not just route-unique?
   * <p>
   * A TripPattern groups all trips visiting a particular pattern of stops on a particular route.
   * GFTS Route names are intended for very general customer information, but sometimes there is a
   * need to know where a particular trip actually goes. For example, the New York City N train has
   * at least four different variants: express (over the Manhattan bridge) and local (via lower
   * Manhattan and the tunnel), in two directions (to Astoria or to Coney Island). During
   * construction, a fifth variant sometimes appears: trains use the D line to Coney Island after
   * 59th St (or from Coney Island to 59th in the opposite direction).
   * <p>
   * TripPattern names are machine-generated on a best-effort basis. They are guaranteed to be
   * unique (among TripPatterns for a single Route) but not stable across graph builds, especially
   * when different versions of GTFS inputs are used. For instance, if a variant is the only variant
   * of the N that ends at Coney Island, the name will be "N to Coney Island". But if multiple
   * variants end at Coney Island (but have different stops elsewhere), that name would not be
   * chosen. OTP also tries start and intermediate stations ("from Coney Island", or "via
   * Whitehall", or even combinations ("from Coney Island via Whitehall"). But if there is no way to
   * create a unique name from start/end/intermediate stops, then the best we can do is to create a
   * "like [trip id]" name, which at least tells you where in the GTFS you can find a related trip.
   */
  // TODO: pass in a transit index that contains a Multimap<Route, TripPattern> and derive all TableTripPatterns
  // TODO: combine from/to and via in a single name. this could be accomplished by grouping the trips by destination,
  // then disambiguating in groups of size greater than 1.
  /*
   * Another possible approach: for each route, determine the necessity of each field (which
   * combination will create unique names). from, to, via, express. Then concatenate all necessary
   * fields. Express should really be determined from number of stops and/or run time of trips.
   */
  public static void generateUniqueNames(Collection<TripPattern> tableTripPatterns) {
    LOG.info("Generating unique names for stop patterns on each route.");

    /* Group TripPatterns by Route */
    Multimap<Route, TripPattern> patternsByRoute = ArrayListMultimap.create();
    for (TripPattern ttp : tableTripPatterns) {
      patternsByRoute.put(ttp.route, ttp);
    }

    /* Iterate over all routes, giving the patterns within each route unique names. */
    for (Route route : patternsByRoute.keySet()) {
      Collection<TripPattern> routeTripPatterns = patternsByRoute.get(route);
      String routeName = route.getName();

      /* Simplest case: there's only one route variant, so we'll just give it the route's name. */
      if (routeTripPatterns.size() == 1) {
        routeTripPatterns.iterator().next().setName(routeName);
        continue;
      }

      /* Do the patterns within this Route have a unique start, end, or via Stop? */
      Multimap<String, TripPattern> signs = ArrayListMultimap.create(); // prefer headsigns
      Multimap<StopLocation, TripPattern> starts = ArrayListMultimap.create();
      Multimap<StopLocation, TripPattern> ends = ArrayListMultimap.create();
      Multimap<StopLocation, TripPattern> vias = ArrayListMultimap.create();

      for (TripPattern pattern : routeTripPatterns) {
        StopLocation start = pattern.firstStop();
        StopLocation end = pattern.lastStop();
        String headsign = pattern.getTripHeadsign();
        if (headsign != null) {
          signs.put(headsign, pattern);
        }
        starts.put(start, pattern);
        ends.put(end, pattern);
        for (StopLocation stop : pattern.getStops()) {
          vias.put(stop, pattern);
        }
      }
      PATTERN:for (TripPattern pattern : routeTripPatterns) {
        StringBuilder sb = new StringBuilder(routeName);
        String headsign = pattern.getTripHeadsign();
        if (headsign != null && signs.get(headsign).size() == 1) {
          pattern.setName(sb.append(" ").append(headsign).toString());
          continue;
        }

        /* First try to name with destination. */
        var end = pattern.lastStop();
        sb.append(" to ").append(stopNameAndId(end));
        if (ends.get(end).size() == 1) {
          pattern.setName(sb.toString());
          continue; // only pattern with this last stop
        }

        /* Then try to name with origin. */
        var start = pattern.firstStop();
        sb.append(" from ").append(stopNameAndId(start));
        if (starts.get(start).size() == 1) {
          pattern.setName((sb.toString()));
          continue; // only pattern with this first stop
        }

        /* Check whether (end, start) is unique. */
        Collection<TripPattern> tripPatterns = starts.get(start);
        Set<TripPattern> remainingPatterns = new HashSet<>(tripPatterns);
        remainingPatterns.retainAll(ends.get(end)); // set intersection
        if (remainingPatterns.size() == 1) {
          pattern.setName((sb.toString()));
          continue;
        }

        /* Still not unique; try (end, start, via) for each via. */
        for (var via : pattern.getStops()) {
          if (via.equals(start) || via.equals(end)) continue;
          Set<TripPattern> intersection = new HashSet<>(remainingPatterns);
          intersection.retainAll(vias.get(via));
          if (intersection.size() == 1) {
            sb.append(" via ").append(stopNameAndId(via));
            pattern.setName((sb.toString()));
            continue PATTERN;
          }
        }

        /* Still not unique; check for express. */
        if (remainingPatterns.size() == 2) {
          // There are exactly two patterns sharing this start/end.
          // The current one must be a subset of the other, because it has no unique via.
          // Therefore we call it the express.
          sb.append(" express");
        } else {
          // The final fallback: reference a specific trip ID.
          Optional
            .ofNullable(pattern.scheduledTimetable.getRepresentativeTripTimes())
            .map(TripTimes::getTrip)
            .ifPresent(value -> sb.append(" like trip ").append(value.getId()));
        }
        pattern.setName((sb.toString()));
      } // END foreach PATTERN
    } // END foreach ROUTE

    if (LOG.isDebugEnabled()) {
      LOG.debug("Done generating unique names for stop patterns on each route.");
      for (Route route : patternsByRoute.keySet()) {
        Collection<TripPattern> routeTripPatterns = patternsByRoute.get(route);
        LOG.debug("Named {} patterns in route {}", routeTripPatterns.size(), route.getName());
        for (TripPattern pattern : routeTripPatterns) {
          LOG.debug("    {} ({} stops)", pattern.name, pattern.stopPattern.getSize());
        }
      }
    }
  }

  /** The human-readable, unique name for this trip pattern. */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * The GTFS Route of all trips in this pattern.
   */
  public Route getRoute() {
    return route;
  }

  /**
   * Convenience method to get the route traverse mode, the mode for all trips in this pattern.
   */
  public TransitMode getMode() {
    return route.getMode();
  }

  public LineString getHopGeometry(int stopPosInPattern) {
    if (hopGeometries != null) {
      return CompactLineStringUtils.uncompactLineString(hopGeometries[stopPosInPattern], false);
    } else {
      return GeometryUtils
        .getGeometryFactory()
        .createLineString(
          new Coordinate[] {
            coordinate(stopPattern.getStop(stopPosInPattern)),
            coordinate(stopPattern.getStop(stopPosInPattern + 1)),
          }
        );
    }
  }

  public StopPattern getStopPattern() {
    return stopPattern;
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  public void setHopGeometries(LineString[] hopGeometries) {
    this.hopGeometries = new byte[hopGeometries.length][];

    for (int i = 0; i < hopGeometries.length; i++) {
      setHopGeometry(i, hopGeometries[i]);
    }
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  public void setHopGeometry(int i, LineString hopGeometry) {
    this.hopGeometries[i] = CompactLineStringUtils.compactLineString(hopGeometry, false);
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  /**
   * This will copy the geometry from another TripPattern to this one. It checks if each hop is
   * between the same stops before copying that hop geometry. If the stops are different but lie
   * within same station, old geometry will be used with overwrite on first and last point (to match
   * new stop places). Otherwise, it will default to straight lines between hops.
   *
   * @param other TripPattern to copy geometry from
   */
  public void setHopGeometriesFromPattern(TripPattern other) {
    this.hopGeometries = new byte[numberOfStops() - 1][];

    // This accounts for the new TripPattern provided by a real-time update and the one that is
    // being replaced having a different number of stops. In that case the geometry will be
    // preserved up until the first mismatching stop, and a straight line will be used for
    // all segments after that.
    int sizeOfShortestPattern = Math.min(numberOfStops(), other.numberOfStops());

    for (int i = 0; i < sizeOfShortestPattern - 1; i++) {
      LineString hopGeometry = other.getHopGeometry(i);

      if (hopGeometry != null && sameStops(other, i)) {
        // Copy hop geometry from previous pattern
        this.setHopGeometry(i, other.getHopGeometry(i));
      } else if (hopGeometry != null && sameStations(other, i)) {
        // Use old geometry but patch first and last point with new stops
        var newStart = new Coordinate(
          this.getStop(i).getCoordinate().longitude(),
          this.getStop(i).getCoordinate().latitude()
        );

        var newEnd = new Coordinate(
          this.getStop(i + 1).getCoordinate().longitude(),
          this.getStop(i + 1).getCoordinate().latitude()
        );

        Coordinate[] coordinates = other.getHopGeometry(i).getCoordinates().clone();
        coordinates[0].setCoordinate(newStart);
        coordinates[coordinates.length - 1].setCoordinate(newEnd);

        this.setHopGeometry(i, GeometryUtils.getGeometryFactory().createLineString(coordinates));
      } else {
        // Create new straight-line geometry for hop
        this.setHopGeometry(
            i,
            GeometryUtils
              .getGeometryFactory()
              .createLineString(
                new Coordinate[] {
                  coordinate(stopPattern.getStop(i)),
                  coordinate(stopPattern.getStop(i + 1)),
                }
              )
          );
      }
    }
  }

  public LineString getGeometry() {
    if (hopGeometries == null || hopGeometries.length == 0) {
      return null;
    }

    List<LineString> lineStrings = new ArrayList<>();
    for (int i = 0; i < hopGeometries.length; i++) {
      lineStrings.add(getHopGeometry(i));
    }
    return GeometryUtils.concatenateLineStrings(lineStrings);
  }

  public int numHopGeometries() {
    return hopGeometries.length;
  }

  public int numberOfStops() {
    return stopPattern.getSize();
  }

  public StopLocation getStop(int stopPosInPattern) {
    return stopPattern.getStop(stopPosInPattern);
  }

  public StopLocation firstStop() {
    return getStop(0);
  }

  public StopLocation lastStop() {
    return getStop(stopPattern.getSize() - 1);
  }

  /** Read only list of stops */
  public List<StopLocation> getStops() {
    return stopPattern.getStops();
  }

  /**
   * Find the first stop position in pattern matching the given {@code stop}. The search start at
   * position {@code 0}. Return a negative number if not found. Use
   * {@link #findAlightStopPositionInPattern(StopLocation)} or
   * {@link #findBoardingStopPositionInPattern(StopLocation)} if possible.
   */
  public int findStopPosition(StopLocation stop) {
    return stopPattern.findStopPosition(stop);
  }

  /**
   * Find the first stop position in pattern matching the given {@code station} where it is allowed
   * to board. The search start at position {@code 0}. Return a negative number if not found.
   */
  public int findBoardingStopPositionInPattern(Station station) {
    return stopPattern.findBoardingPosition(station);
  }

  /**
   * Find the first stop position in pattern matching the given {@code station} where it is allowed
   * to alight. The search start at position {@code 1}. Return a negative number if not found.
   */
  public int findAlightStopPositionInPattern(Station station) {
    return stopPattern.findAlightPosition(station);
  }

  /**
   * Find the first stop position in pattern matching the given {@code stop} where it is allowed to
   * board. The search start at position {@code 0}. Return a negative number if not found.
   */
  public int findBoardingStopPositionInPattern(StopLocation stop) {
    return stopPattern.findBoardingPosition(stop);
  }

  /**
   * Find the first stop position in pattern matching the given {@code stop} where it is allowed to
   * alight. The search start at position {@code 1}. Return a negative number if not found.
   */
  public int findAlightStopPositionInPattern(StopLocation stop) {
    return stopPattern.findAlightPosition(stop);
  }

  /** Returns whether passengers can alight at a given stop */
  public boolean canAlight(int stopIndex) {
    return stopPattern.canAlight(stopIndex);
  }

  /** Returns whether passengers can board at a given stop */
  public boolean canBoard(int stopIndex) {
    return stopPattern.canBoard(stopIndex);
  }

  /**
   * Returns whether passengers can board at a given stop. This is an inefficient method iterating
   * over the stops, do not use it in routing.
   */
  public boolean canBoard(StopLocation stop) {
    return stopPattern.canBoard(stop);
  }

  /** Returns whether a given stop is wheelchair-accessible. */
  public boolean wheelchairAccessible(int stopIndex) {
    return (
      stopPattern.getStop(stopIndex).getWheelchairAccessibility() ==
      WheelchairAccessibility.POSSIBLE
    );
  }

  public PickDrop getAlightType(int stopIndex) {
    return stopPattern.getDropoff(stopIndex);
  }

  public PickDrop getBoardType(int stopIndex) {
    return stopPattern.getPickup(stopIndex);
  }

  public boolean isBoardAndAlightAt(int stopIndex, PickDrop value) {
    return getBoardType(stopIndex).is(value) && getAlightType(stopIndex).is(value);
  }

  /* METHODS THAT DELEGATE TO THE SCHEDULED TIMETABLE */

  // TODO: These should probably be deprecated. That would require grabbing the scheduled timetable,
  // and would avoid mistakes where real-time updates are accidentally not taken into account.

  public boolean stopPatternIsEqual(TripPattern other) {
    return stopPattern.equals(other.stopPattern);
  }

  public Trip getTrip(int tripIndex) {
    return scheduledTimetable.getTripTimes(tripIndex).getTrip();
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  /**
   * Add the given tripTimes to this pattern's scheduled timetable, recording the corresponding trip
   * as one of the scheduled trips on this pattern.
   */
  public void add(TripTimes tt) {
    // Only scheduled trips (added at graph build time, rather than directly to the timetable
    // via updates) are in this list.
    scheduledTimetable.addTripTimes(tt);

    // Check that all trips added to this pattern are on the initially declared route.
    // Identity equality is valid on GTFS entity objects.
    if (this.route != tt.getTrip().getRoute()) {
      LOG.warn(
        "The trip {} is on route {} but its stop pattern is on route {}.",
        tt.getTrip(),
        tt.getTrip().getRoute(),
        route
      );
    }
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  /**
   * Add the given FrequencyEntry to this pattern's scheduled timetable, recording the corresponding
   * trip as one of the scheduled trips on this pattern.
   * TODO possible improvements: combine freq entries and TripTimes. Do not keep trips list in TripPattern
   * since it is redundant.
   */
  public void add(FrequencyEntry freq) {
    scheduledTimetable.addFrequencyEntry(freq);
    if (this.getRoute() != freq.tripTimes.getTrip().getRoute()) {
      LOG.warn(
        "The trip {} is on a different route than its stop pattern, which is on {}.",
        freq.tripTimes.getTrip(),
        route
      );
    }
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  /**
   * Remove all trips matching the given predicate.
   *
   * @param removeTrip it the predicate returns true
   */
  public void removeTrips(Predicate<Trip> removeTrip) {
    scheduledTimetable.getTripTimes().removeIf(tt -> removeTrip.test(tt.getTrip()));
  }

  /**
   * Checks that this is TripPattern is based of the provided TripPattern and contains same stops
   * (but not necessarily with same pickup and dropoff values).
   */
  public boolean isModifiedFromTripPatternWithEqualStops(TripPattern other) {
    return (
      originalTripPattern != null &&
      originalTripPattern.equals(other) &&
      getStopPattern().stopsEqual(other.getStopPattern())
    );
  }

  /**
   * The direction for all the trips in this pattern.
   */
  public Direction getDirection() {
    return scheduledTimetable.getDirection();
  }

  /**
   * This pattern may have multiple Timetable objects, but they should all contain TripTimes for the
   * same trips, in the same order (that of the scheduled Timetable). An exception to this rule may
   * arise if unscheduled trips are added to a Timetable. For that case we need to search for
   * trips/TripIds in the Timetable rather than the enclosing TripPattern.
   */
  public Stream<Trip> scheduledTripsAsStream() {
    var trips = scheduledTimetable.getTripTimes().stream().map(TripTimes::getTrip);
    var freqTrips = scheduledTimetable
      .getFrequencyEntries()
      .stream()
      .map(e -> e.tripTimes.getTrip());
    return Stream.concat(trips, freqTrips).distinct();
  }

  /**
   * This is the "original" timetable holding the scheduled stop times from GTFS, with no realtime
   * updates applied. If realtime stoptime updates are applied, next/previous departure searches
   * will be conducted using a different, updated timetable in a snapshot.
   */
  public Timetable getScheduledTimetable() {
    return scheduledTimetable;
  }

  /**
   * Has the TripPattern been created by a real-time update.
   */
  public boolean isCreatedByRealtimeUpdater() {
    return createdByRealtimeUpdater;
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  /**
   * A bit of a strange place to set service codes all at once when TripTimes are already added, but
   * we need a reference to the Graph or at least the codes map. This could also be placed in the
   * hop factory itself.
   */
  public void setServiceCodes(Map<FeedScopedId, Integer> serviceCodes) {
    this.services = new BitSet();
    scheduledTripsAsStream()
      .forEach(trip -> {
        FeedScopedId serviceId = trip.getServiceId();
        if (serviceCodes.containsKey(serviceId)) {
          services.set(serviceCodes.get(serviceId));
        } else {
          LOG.warn("Service " + serviceId + " not found in service codes not found.");
        }
      });
    scheduledTimetable.setServiceCodes(serviceCodes);
  }

  // TODO OTP2 this method modifies the state, it will be refactored in a subsequent step
  /**
   * Sets service code for pattern if it's not already set
   *
   * @param serviceCode service code that needs to be set
   */
  public void setServiceCode(int serviceCode) {
    if (!getServices().get(serviceCode)) {
      final BitSet services = (BitSet) getServices().clone();
      services.set(serviceCode);
      this.services = services;
    }
  }

  /**
   * A set of serviceIds with at least one trip in this pattern. Trips in a pattern are no longer
   * necessarily running on the same service ID.
   *
   * @return bitset of service codes
   */
  public BitSet getServices() {
    return services;
  }

  public TripPattern getOriginalTripPattern() {
    return originalTripPattern;
  }

  public String getTripHeadsign() {
    var tripTimes = scheduledTimetable.getRepresentativeTripTimes();
    if (tripTimes == null) {
      return null;
    }
    return tripTimes.getTrip().getHeadsign();
  }

  public String getStopHeadsign(int stopIndex) {
    var tripTimes = scheduledTimetable.getRepresentativeTripTimes();
    if (tripTimes == null) {
      return null;
    }
    return tripTimes.getHeadsign(stopIndex);
  }

  public boolean matchesModeOrSubMode(TransitMode mode, SubMode transportSubmode) {
    return getMode().equals(mode) || route.getNetexSubmode().equals(transportSubmode);
  }

  /**
   * In most cases we want to use identity equality for Trips. However, in some cases we want a way
   * to consistently identify trips across versions of a GTFS feed, when the feed publisher cannot
   * ensure stable trip IDs. Therefore we define some additional hash functions. Hash collisions are
   * theoretically possible, so these identifiers should only be used to detect when two trips are
   * the same with a high degree of probability. An example application is avoiding double-booking
   * of a particular bus trip for school field trips. Using Murmur hash function. see
   * http://programmers.stackexchange.com/a/145633 for comparison.
   *
   * @param trip a trip object within this pattern, or null to hash the pattern itself independent
   *             any specific trip.
   * @return the semantic hash of a Trip in this pattern as a printable String.
   * <p>
   * TODO deal with frequency-based trips
   */
  public String semanticHashString(Trip trip) {
    HashFunction murmur = Hashing.murmur3_32();
    BaseEncoding encoder = BaseEncoding.base64Url().omitPadding();
    StringBuilder sb = new StringBuilder(50);
    sb.append(encoder.encode(stopPattern.semanticHash(murmur).asBytes()));
    if (trip != null) {
      TripTimes tripTimes = scheduledTimetable.getTripTimes(trip);
      if (tripTimes == null) {
        return null;
      }
      sb.append(':');
      sb.append(encoder.encode(tripTimes.semanticHash(murmur).asBytes()));
    }
    return sb.toString();
  }

  public TripPattern clone() {
    try {
      return (TripPattern) super.clone();
    } catch (CloneNotSupportedException e) {
      /* cannot happen */
      throw new RuntimeException(e);
    }
  }

  /**
   * Get the feed id this trip pattern belongs to.
   *
   * @return feed id for this trip pattern
   */
  public String getFeedId() {
    // The feed id is the same as the agency id on the route, this allows us to obtain it from there.
    return route.getId().getFeedId();
  }

  private static String stopNameAndId(StopLocation stop) {
    return stop.getName() + " (" + stop.getId().toString() + ")";
  }

  private static Coordinate coordinate(StopLocation s) {
    return new Coordinate(s.getLon(), s.getLat());
  }

  /**
   * Check if given stop and next stop on this trip pattern and other are equal.
   *
   * @param other Other instance of trip pattern with list of stops. May not be null.
   * @param index Given index for stop
   * @return true if stop and next stop are equal on bouth trip patterns, else false
   */
  private boolean sameStops(TripPattern other, int index) {
    var otherOrigin = other.getStop(index);
    var otherDestination = other.getStop(index + 1);
    var origin = getStop(index);
    var destination = getStop(index + 1);

    return origin.equals(otherOrigin) && destination.equals(otherDestination);
  }

  /**
   * Check if Station is equal on given stop and next stop for this trip pattern and other.
   *
   * @param other Other instance of trip pattern with list of stops. May not be null.
   * @param index Given index for stop
   * @return true if the stops have the same stations, else false. If any station is null then
   * false.
   */
  private boolean sameStations(TripPattern other, int index) {
    var otherOrigin = other.getStop(index).getParentStation();
    var otherDestination = other.getStop(index + 1).getParentStation();
    var origin = getStop(index).getParentStation();
    var destionation = getStop(index + 1).getParentStation();

    var sameOrigin = Optional
      .ofNullable(origin)
      .map(o -> o.equals(otherOrigin))
      .orElse(getStop(index).equals(other.getStop(index)));

    var sameDestination = Optional
      .ofNullable(destionation)
      .map(o -> o.equals(otherDestination))
      .orElse(getStop(index + 1).equals(other.getStop(index + 1)));

    return sameOrigin && sameDestination;
  }

  @Override
  public boolean sameAs(@Nonnull TripPattern other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(this.route, other.route) &&
      Objects.equals(this.name, other.name) &&
      Objects.equals(this.stopPattern, other.stopPattern) &&
      Objects.equals(this.scheduledTimetable, other.scheduledTimetable)
    );
  }

  @Override
  public TripPatternBuilder copy() {
    return new TripPatternBuilder(this);
  }
}
