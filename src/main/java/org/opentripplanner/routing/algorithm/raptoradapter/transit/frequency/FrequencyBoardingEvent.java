package org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency;

import java.time.LocalDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultTripSchedule;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

/**
 * Represents a result of a {@link TripFrequencyBoardSearch}, with materialized {@link TripTimes}
 */
final class FrequencyBoardingEvent<T extends DefaultTripSchedule>
  extends FrequencyBoardOrAlightEvent<T> {

  public FrequencyBoardingEvent(
    RaptorTripPattern raptorTripPattern,
    TripTimes tripTimes,
    TripPattern pattern,
    int stopPositionInPattern,
    int departureTime,
    int headway,
    int offset,
    LocalDate serviceDate
  ) {
    super(
      raptorTripPattern,
      tripTimes,
      pattern,
      stopPositionInPattern,
      departureTime,
      offset,
      headway,
      serviceDate
    );
  }

  // Add headway here to report a late enough time to account for uncertainty
  @Override
  public int arrival(int stopPosInPattern) {
    return tripTimes.getArrivalTime(stopPosInPattern) + headway + offset;
  }

  @Override
  public int departure(int stopPosInPattern) {
    return tripTimes.getDepartureTime(stopPosInPattern) + offset;
  }
}
