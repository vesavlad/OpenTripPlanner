/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.ext.fares.model;

import java.io.Serializable;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.util.lang.ToStringBuilder;

public final class FareRule implements Serializable {

  private static final long serialVersionUID = 1L;

  private FareAttribute fare;

  private Route route;

  private String originId;

  private String destinationId;

  private String containsId;

  public FareAttribute getFare() {
    return fare;
  }

  public void setFare(FareAttribute fare) {
    this.fare = fare;
  }

  public Route getRoute() {
    return route;
  }

  public void setRoute(Route route) {
    this.route = route;
  }

  public String getOriginId() {
    return originId;
  }

  public void setOriginId(String originId) {
    this.originId = originId;
  }

  public String getDestinationId() {
    return destinationId;
  }

  public void setDestinationId(String destinationId) {
    this.destinationId = destinationId;
  }

  public String getContainsId() {
    return containsId;
  }

  public void setContainsId(String containsId) {
    this.containsId = containsId;
  }

  public String toString() {
    return ToStringBuilder
      .of(FareRule.class)
      .addObjOp("route", route, TransitEntity::getId)
      .addObj("originId", originId)
      .addObj("containsId", containsId)
      .addObj("destinationId", destinationId)
      .toString();
  }
}
