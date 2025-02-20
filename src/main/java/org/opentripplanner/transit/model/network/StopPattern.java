package org.opentripplanner.transit.model.network;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.site.FlexStopLocation;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * This class represents what is called a JourneyPattern in Transmodel: the sequence of stops at
 * which a trip (GTFS) or vehicle journey (Transmodel) calls, irrespective of the day on which
 * service runs.
 * <p>
 * An important detail: Routes in GTFS are not a structurally important element, they just serve as
 * user-facing information. It is possible for the same journey pattern to appear in more than one
 * route.
 * <p>
 * OTP already has several classes that represent this same thing: A TripPattern in the context of
 * routing. It represents all trips with the same stop pattern A ScheduledStopPattern in the GTFS
 * loading process. A RouteVariant in the TransitIndex, which has a unique human-readable name and
 * belongs to a particular route.
 * <p>
 * We would like to combine all these different classes into one.
 * <p>
 * Any two trips with the same stops in the same order, and that operate on the same days, can be
 * combined using a TripPattern to simplify the graph. This saves memory and reduces search
 * complexity since we only consider the trip that departs soonest for each pattern. Field
 * calendarId has been removed. See issue #1320.
 * <p>
 * A StopPattern is very closely related to a TripPattern -- it essentially serves as the unique key
 * for a TripPattern. Should the route be included in the StopPattern?
 */
public final class StopPattern implements Serializable {

  private static final long serialVersionUID = 20140101L;
  public static final int NOT_FOUND = -1;

  private final StopLocation[] stops;
  private final PickDrop[] pickups;
  private final PickDrop[] dropoffs;

  private StopPattern(int size) {
    stops = new StopLocation[size];
    pickups = new PickDrop[size];
    dropoffs = new PickDrop[size];
  }

  private StopPattern(StopLocation[] stops, PickDrop[] pickups, PickDrop[] dropoffs) {
    this.stops = stops;
    this.pickups = pickups;
    this.dropoffs = dropoffs;
  }

  /** Assumes that stopTimes are already sorted by time. */
  public StopPattern(Collection<StopTime> stopTimes) {
    this(stopTimes.size());
    int size = stopTimes.size();
    if (size == 0) return;
    Iterator<StopTime> stopTimeIterator = stopTimes.iterator();

    for (int i = 0; i < size; ++i) {
      StopTime stopTime = stopTimeIterator.next();
      stops[i] = stopTime.getStop();
      // should these just be booleans? anything but 1 means pick/drop is allowed.
      // pick/drop messages could be stored in individual trips
      pickups[i] = computePickDrop(stopTime.getStop(), stopTime.getPickupType());
      dropoffs[i] = computePickDrop(stopTime.getStop(), stopTime.getDropOffType());
    }
  }

  /**
   * For creating StopTimes without StopTime, for example for unit testing.
   */
  public static StopPatternBuilder create(int length) {
    return new StopPatternBuilder(new StopPattern(length));
  }

  public StopPatternBuilder mutate() {
    return new StopPatternBuilder(this);
  }

  public int hashCode() {
    int hash = stops.length;
    hash += Arrays.hashCode(this.stops);
    hash *= 31;
    hash += Arrays.hashCode(this.pickups);
    hash *= 31;
    hash += Arrays.hashCode(this.dropoffs);
    return hash;
  }

  public boolean equals(Object other) {
    if (other instanceof StopPattern) {
      StopPattern that = (StopPattern) other;
      return (
        Arrays.equals(this.stops, that.stops) &&
        Arrays.equals(this.pickups, that.pickups) &&
        Arrays.equals(this.dropoffs, that.dropoffs)
      );
    } else {
      return false;
    }
  }

  /**
   * Checks that stops equal without taking into account if pickup or dropoff is allowed.
   */
  public boolean stopsEqual(Object other) {
    if (other instanceof StopPattern that) {
      return Arrays.equals(this.stops, that.stops);
    } else {
      return false;
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("StopPattern: ");
    for (int i = 0, j = stops.length; i < j; ++i) {
      sb.append(String.format("%s_%s%s ", stops[i].getCode(), pickups[i], dropoffs[i]));
    }
    return sb.toString();
  }

  int getSize() {
    return stops.length;
  }

  /** Find the given stop position in the sequence, return -1 if not found. */
  int findStopPosition(StopLocation stop) {
    for (int i = 0; i < stops.length; ++i) {
      if (stops[i] == stop) {
        return i;
      }
    }
    return -1;
  }

  int findBoardingPosition(StopLocation stop) {
    return findStopPosition(0, stops.length - 1, s -> s == stop);
  }

  int findAlightPosition(StopLocation stop) {
    return findStopPosition(1, stops.length, s -> s == stop);
  }

  int findBoardingPosition(Station station) {
    return findStopPosition(0, stops.length - 1, station::includes);
  }

  int findAlightPosition(Station station) {
    return findStopPosition(1, stops.length, station::includes);
  }

  /**
   * In most cases we want to use identity equality for StopPatterns. There is a single StopPattern
   * instance for each semantic StopPattern, and we don't want to calculate complicated hashes or
   * equality values during normal execution. However, in some cases we want a way to consistently
   * identify trips across versions of a GTFS feed, when the feed publisher cannot ensure stable
   * trip IDs. Therefore we define some additional hash functions.
   */
  HashCode semanticHash(HashFunction hashFunction) {
    Hasher hasher = hashFunction.newHasher();
    int size = stops.length;
    for (StopLocation stop : stops) {
      // Truncate the lat and lon to 6 decimal places in case they move slightly between
      // feed versions
      hasher.putLong((long) (stop.getLat() * 1000000));
      hasher.putLong((long) (stop.getLon() * 1000000));
    }
    // Use hops rather than stops because drop-off at stop 0 and pick-up at last stop are
    // not important and have changed between OTP versions.
    for (int hop = 0; hop < size - 1; hop++) {
      hasher.putInt(pickups[hop].ordinal());
      hasher.putInt(dropoffs[hop + 1].ordinal());
    }
    return hasher.hash();
  }

  /** Get a copy of the internal collection of stops. */
  List<StopLocation> getStops() {
    return List.of(stops);
  }

  StopLocation getStop(int stopPosInPattern) {
    return stops[stopPosInPattern];
  }

  PickDrop getPickup(int stopPosInPattern) {
    return pickups[stopPosInPattern];
  }

  PickDrop getDropoff(int stopPosInPattern) {
    return dropoffs[stopPosInPattern];
  }

  /** Returns whether passengers can alight at a given stop */
  boolean canAlight(int stopPosInPattern) {
    return dropoffs[stopPosInPattern].isRoutable();
  }

  /** Returns whether passengers can board at a given stop */
  boolean canBoard(int stopPosInPattern) {
    return pickups[stopPosInPattern].isRoutable();
  }

  /**
   * Returns whether passengers can board at a given stop. This is an inefficient method iterating
   * over the stops, do not use it in routing.
   */
  boolean canBoard(StopLocation stop) {
    // We skip the last stop, not allowed for boarding
    for (int i = 0; i < stops.length - 1; ++i) {
      if (stop == stops[i] && canBoard(i)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Raptor should not be allowed to board or alight flex stops because they have fake coordinates
   * (centroids) and might not have times.
   */
  private static PickDrop computePickDrop(StopLocation stop, PickDrop pickDrop) {
    if (stop instanceof FlexStopLocation) {
      return PickDrop.NONE;
    } else {
      return pickDrop;
    }
  }

  /**
   * Find the given stop position in the sequence according to match Predicate, return -1 if not
   * found.
   */
  private int findStopPosition(
    final int start,
    final int end,
    final Predicate<StopLocation> match
  ) {
    for (int i = start; i < end; ++i) {
      if (match.test(stops[i])) {
        return i;
      }
    }
    return -1;
  }

  public static class StopPatternBuilder {

    public final StopLocation[] stops;
    public final PickDrop[] pickups;
    public final PickDrop[] dropoffs;

    public StopPatternBuilder(StopPattern original) {
      stops = Arrays.copyOf(original.stops, original.stops.length);
      pickups = Arrays.copyOf(original.pickups, original.pickups.length);
      dropoffs = Arrays.copyOf(original.dropoffs, original.dropoffs.length);
    }

    /**
     * Sets pickup and dropoff at given stop indices as CANCELLED.
     *
     * @return StopPatternBuilder
     */
    public StopPatternBuilder cancelStops(List<Integer> cancelledStopIndices) {
      cancelledStopIndices.forEach(index -> {
        pickups[index] = PickDrop.CANCELLED;
        dropoffs[index] = PickDrop.CANCELLED;
      });
      return this;
    }

    public StopPattern build() {
      return new StopPattern(stops, pickups, dropoffs);
    }
  }
}
