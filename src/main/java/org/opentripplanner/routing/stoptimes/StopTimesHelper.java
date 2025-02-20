package org.opentripplanner.routing.stoptimes;

import static org.opentripplanner.routing.stoptimes.ArrivalDeparture.ARRIVALS;
import static org.opentripplanner.routing.stoptimes.ArrivalDeparture.DEPARTURES;
import static org.opentripplanner.util.time.TimeUtils.ONE_DAY_SECONDS;

import com.google.common.collect.MinMaxPriorityQueue;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.time.ServiceDateUtils;

public class StopTimesHelper {

  /**
   * Fetch upcoming vehicle departures from a stop. It goes though all patterns passing the stop for
   * the previous, current and next service date. It uses a priority queue to keep track of the next
   * departures. The queue is shared between all dates, as services from the previous service date
   * can visit the stop later than the current service date's services. This happens eg. with
   * sleeper trains.
   * <p>
   * TODO: Add frequency based trips
   *
   * @param stop                  Stop object to perform the search for
   * @param startTime             Start time for the search. Seconds from UNIX epoch
   * @param timeRange             Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures    Number of departures to fetch per pattern
   * @param arrivalDeparture      Filter by arrivals, departures, or both
   * @param includeCancelledTrips If true, cancelled trips will also be included in result
   */
  public static List<StopTimesInPattern> stopTimesForStop(
    TransitService transitService,
    TimetableSnapshot timetableSnapshot,
    StopLocation stop,
    long startTime, // TODO: Migrate to instant
    int timeRange, // TODO: Migrate to duration
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancelledTrips
  ) {
    if (startTime == 0) {
      startTime = Instant.now().getEpochSecond();
    }
    List<StopTimesInPattern> result = new ArrayList<>();

    ZoneId zoneId = transitService.getTransitLayer().getTransitDataZoneId();
    LocalDate date = Instant.ofEpochSecond(startTime).atZone(zoneId).toLocalDate();

    // Number of days requested + the following day
    int numberOfDays = timeRange / ONE_DAY_SECONDS + 1;

    List<LocalDate> dates = new ArrayList<>();

    // Yesterday, today, number of requested days, following day
    for (int i = -1; i <= numberOfDays; i++) {
      dates.add(date.plusDays(i));
    }

    // Fetch all patterns, including those from realtime sources
    Collection<TripPattern> patterns = transitService.getPatternsForStop(stop, timetableSnapshot);

    for (TripPattern pattern : patterns) {
      Queue<TripTimeOnDate> pq = listTripTimeShortsForPatternAtStop(
        transitService,
        timetableSnapshot,
        stop,
        pattern,
        startTime,
        timeRange,
        numberOfDepartures,
        arrivalDeparture,
        includeCancelledTrips,
        false,
        dates
      );

      result.addAll(getStopTimesInPattern(pattern, pq));
    }

    return result;
  }

  /**
   * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when
   * creating complete stop timetables for a single day.
   *
   * @param stop        Stop object to perform the search for
   * @param serviceDate Return all departures for the specified date
   */
  public static List<StopTimesInPattern> stopTimesForStop(
    TransitService transitService,
    StopLocation stop,
    LocalDate serviceDate,
    ArrivalDeparture arrivalDeparture
  ) {
    List<StopTimesInPattern> ret = new ArrayList<>();

    Collection<TripPattern> patternsForStop = transitService.getPatternsForStop(stop, true);
    for (TripPattern pattern : patternsForStop) {
      StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
      Timetable tt;
      TimetableSnapshot timetableSnapshot = transitService.getTimetableSnapshot();
      if (timetableSnapshot != null) {
        tt = timetableSnapshot.resolve(pattern, serviceDate);
      } else {
        tt = pattern.getScheduledTimetable();
      }
      var servicesRunning = transitService.getServicesRunningForDate(serviceDate);
      Instant midnight = ServiceDateUtils
        .asStartOfService(serviceDate, transitService.getTimeZone())
        .toInstant();
      int sidx = 0;
      for (var currStop : pattern.getStops()) {
        if (currStop == stop) {
          if (skipByPickUpDropOff(pattern, arrivalDeparture, sidx)) continue;
          for (TripTimes t : tt.getTripTimes()) {
            if (!servicesRunning.contains(t.getServiceCode())) {
              continue;
            }
            stopTimes.times.add(new TripTimeOnDate(t, sidx, pattern, serviceDate, midnight));
          }
        }
        sidx++;
      }
      ret.add(stopTimes);
    }
    return ret;
  }

  /**
   * Fetch upcoming vehicle departures from a stop for a single pattern, passing the stop for the
   * previous, current and next service date. It uses a priority queue to keep track of the next
   * departures. The queue is shared between all dates, as services from the previous service date
   * can visit the stop later than the current service date's services.
   * <p>
   * TODO: Add frequency based trips
   *
   * @param stop               Stop object to perform the search for
   * @param pattern            Pattern object to perform the search for
   * @param startTime          Start time for the search. Seconds from UNIX epoch
   * @param timeRange          Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures Number of departures to fetch per pattern
   * @param arrivalDeparture   Filter by arrivals, departures, or both.
   */
  public static List<TripTimeOnDate> stopTimesForPatternAtStop(
    TransitService transitService,
    TimetableSnapshot timetableSnapshot,
    StopLocation stop,
    TripPattern pattern,
    long startTime, // TODO: Migrate to instant
    int timeRange, // TODO: Migrate to duration
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture
  ) {
    if (startTime == 0) {
      startTime = System.currentTimeMillis() / 1000;
    }
    LocalDate date = Instant
      .ofEpochSecond(startTime)
      .atZone(transitService.getTimeZone())
      .toLocalDate();
    List<LocalDate> serviceDates = List.of(date.minusDays(1), date, date.plusDays(1));

    Queue<TripTimeOnDate> pq = listTripTimeShortsForPatternAtStop(
      transitService,
      timetableSnapshot,
      stop,
      pattern,
      startTime,
      timeRange,
      numberOfDepartures,
      arrivalDeparture,
      false,
      true,
      serviceDates
    );

    return new ArrayList<>(pq);
  }

  private static List<StopTimesInPattern> getStopTimesInPattern(
    TripPattern pattern,
    Queue<TripTimeOnDate> pq
  ) {
    List<StopTimesInPattern> result = new ArrayList<>();
    if (!pq.isEmpty()) {
      StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
      while (!pq.isEmpty()) {
        stopTimes.times.add(0, pq.poll());
      }
      result.add(stopTimes);
    }
    return result;
  }

  private static Queue<TripTimeOnDate> listTripTimeShortsForPatternAtStop(
    TransitService transitService,
    TimetableSnapshot timetableSnapshot,
    StopLocation stop,
    TripPattern pattern,
    long startTime, //TODO: Change to Instant
    int timeRange, // TODO: Migrate to duration
    int numberOfDepartures,
    ArrivalDeparture arrivalDeparture,
    boolean includeCancellations,
    boolean includeReplaced,
    Collection<LocalDate> serviceDates
  ) {
    // The bounded priority Q is used to keep a sorted short list of trip times. We can not
    // relay on the trip times to be in order because of real-time updates. This code can
    // probably be optimized, and the trip search in the Raptor search does almost the same
    // thing. This is no part of a routing request, but is a used frequently in some
    // operation like Entur for "departure boards" (apps, widgets, screens on platforms, and
    // hotel lobbies). Setting the numberOfDepartures and timeRange to a big number for a
    // transit hub could result in a DOS attack, but there are probably other more effective
    // ways to do it.
    //
    // The {@link MinMaxPriorityQueue} is marked beta, but we do not have a god alternative.
    MinMaxPriorityQueue<TripTimeOnDate> pq = MinMaxPriorityQueue
      .orderedBy(
        Comparator.comparing((TripTimeOnDate tts) ->
          tts.getServiceDayMidnight() + tts.getRealtimeDeparture()
        )
      )
      .maximumSize(numberOfDepartures)
      .create();

    // Loop through all possible days
    for (LocalDate serviceDate : serviceDates) {
      Timetable timetable;
      if (timetableSnapshot != null) {
        timetable = timetableSnapshot.resolve(pattern, serviceDate);
      } else {
        timetable = pattern.getScheduledTimetable();
      }

      ZonedDateTime midnight = ServiceDateUtils.asStartOfService(
        serviceDate,
        transitService.getTimeZone()
      );
      int secondsSinceMidnight = ServiceDateUtils.secondsSinceStartOfService(
        midnight,
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(startTime), transitService.getTimeZone())
      );
      var servicesRunning = transitService.getServicesRunningForDate(serviceDate);

      int stopIndex = 0;
      for (var currStop : pattern.getStops()) {
        if (currStop == stop) {
          if (skipByPickUpDropOff(pattern, arrivalDeparture, stopIndex)) {
            continue;
          }
          if (skipByStopCancellation(pattern, includeCancellations, stopIndex)) {
            continue;
          }

          for (TripTimes tripTimes : timetable.getTripTimes()) {
            if (!servicesRunning.contains(tripTimes.getServiceCode())) {
              continue;
            }
            if (skipByTripCancellation(tripTimes, includeCancellations)) {
              continue;
            }
            if (
              !includeReplaced &&
              isReplacedByAnotherPattern(
                tripTimes.getTrip(),
                serviceDate,
                pattern,
                timetableSnapshot
              )
            ) {
              continue;
            }

            boolean departureTimeInRange =
              tripTimes.getDepartureTime(stopIndex) >= secondsSinceMidnight &&
              tripTimes.getDepartureTime(stopIndex) <= secondsSinceMidnight + timeRange;

            boolean arrivalTimeInRange =
              tripTimes.getArrivalTime(stopIndex) >= secondsSinceMidnight &&
              tripTimes.getArrivalTime(stopIndex) <= secondsSinceMidnight + timeRange;

            // ARRIVAL: Arrival time has to be within range
            // DEPARTURES: Departure time has to be within range
            // BOTH: Either arrival time or departure time has to be within range
            if (
              (arrivalDeparture != ARRIVALS && departureTimeInRange) ||
              (arrivalDeparture != DEPARTURES && arrivalTimeInRange)
            ) {
              pq.add(
                new TripTimeOnDate(tripTimes, stopIndex, pattern, serviceDate, midnight.toInstant())
              );
            }
          }
          // TODO Add back support for frequency entries
        }
        stopIndex++;
      }
    }
    return pq;
  }

  private static boolean isReplacedByAnotherPattern(
    Trip trip,
    LocalDate serviceDate,
    TripPattern pattern,
    TimetableSnapshot snapshot
  ) {
    if (snapshot == null) {
      return false;
    }
    final TripPattern replacement = snapshot.getLastAddedTripPattern(trip.getId(), serviceDate);
    return replacement != null && !replacement.equals(pattern);
  }

  public static boolean skipByTripCancellation(TripTimes tripTimes, boolean includeCancellations) {
    return (
      (tripTimes.isCanceled() || tripTimes.getTrip().getNetexAlteration().isCanceledOrReplaced()) &&
      !includeCancellations
    );
  }

  private static boolean skipByPickUpDropOff(
    TripPattern pattern,
    ArrivalDeparture arrivalDeparture,
    int stopIndex
  ) {
    boolean noPickup = pattern.getBoardType(stopIndex).is(PickDrop.NONE);
    boolean noDropoff = pattern.getAlightType(stopIndex).is(PickDrop.NONE);

    if (noPickup && noDropoff) {
      return true;
    }
    if (noPickup && arrivalDeparture == DEPARTURES) {
      return true;
    }
    if (noDropoff && arrivalDeparture == ARRIVALS) {
      return true;
    }
    return false;
  }

  private static boolean skipByStopCancellation(
    TripPattern pattern,
    boolean includeCancelledTrips,
    int stopIndex
  ) {
    boolean pickupCancelled = pattern.getBoardType(stopIndex).is(PickDrop.CANCELLED);
    boolean dropOffCancelled = pattern.getAlightType(stopIndex).is(PickDrop.CANCELLED);

    return (pickupCancelled || dropOffCancelled) && !includeCancelledTrips;
  }
}
