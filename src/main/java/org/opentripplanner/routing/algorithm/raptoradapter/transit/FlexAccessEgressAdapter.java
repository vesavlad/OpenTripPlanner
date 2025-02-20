package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.transit.service.StopModelIndex;

/**
 * This class is used to adapt the FlexAccessEgress into a time-dependent multi-leg AccessEgress.
 */
public class FlexAccessEgressAdapter extends AccessEgress {

  private final FlexAccessEgress flexAccessEgress;

  public FlexAccessEgressAdapter(
    FlexAccessEgress flexAccessEgress,
    boolean isEgress,
    StopModelIndex stopIndex
  ) {
    super(
      stopIndex.indexOf(flexAccessEgress.stop),
      isEgress ? flexAccessEgress.lastState.reverse() : flexAccessEgress.lastState
    );
    this.flexAccessEgress = flexAccessEgress;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    return flexAccessEgress.earliestDepartureTime(requestedDepartureTime);
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    return flexAccessEgress.latestArrivalTime(requestedArrivalTime);
  }

  @Override
  public int numberOfRides() {
    // We only support one flex leg at the moment
    return 1;
  }

  @Override
  public boolean stopReachedOnBoard() {
    return flexAccessEgress.directToStop;
  }

  @Override
  public boolean hasOpeningHours() {
    // TODO OTP2: THIS SHOULD BE IMPLEMENTED SO WE CAN FILTER FLEX ACCESS AND EGRESS
    //            IN ROUTING, IT IS SET TO TRUE NOW TO ASSUME ALL FLEX HAS OPENING HOURS
    return true;
  }

  @Override
  public String toString() {
    return asString();
  }
}
