package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleFunction;
import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * This class define how to calculate the cost when cost is part of the multi-criteria pareto
 * function.
 */
public class McCostParams {

  public static final double DEFAULT_TRANSIT_RELUCTANCE = 1.0;

  public static final McCostParams DEFAULTS = new McCostParams();

  private final int boardCost;
  private final int transferCost;
  private final double[] transitReluctanceFactors;
  private final double waitReluctanceFactor;
  private final WheelchairAccessibilityRequest accessibilityRequest;
  private final Set<FeedScopedId> unpreferredRoutes;
  private final DoubleFunction<Double> unpreferredCost;

  /**
   * Default constructor defines default values. These defaults are overridden by defaults in the
   * {@link RoutingRequest}.
   */
  private McCostParams() {
    this.boardCost = 600;
    this.transferCost = 0;
    this.transitReluctanceFactors = null;
    this.waitReluctanceFactor = 1.0;
    this.accessibilityRequest = WheelchairAccessibilityRequest.DEFAULT;
    this.unpreferredRoutes = Set.of();
    this.unpreferredCost = RequestFunctions.createLinearFunction(0.0, DEFAULT_TRANSIT_RELUCTANCE);
  }

  McCostParams(McCostParamsBuilder builder) {
    this.boardCost = builder.boardCost();
    this.transferCost = builder.transferCost();
    this.transitReluctanceFactors = builder.transitReluctanceFactors();
    this.waitReluctanceFactor = builder.waitReluctanceFactor();
    this.accessibilityRequest = builder.wheelchairAccessibility();
    this.unpreferredRoutes = builder.unpreferredRoutes();
    this.unpreferredCost = builder.unpreferredCost();
  }

  public int boardCost() {
    return boardCost;
  }

  public int transferCost() {
    return transferCost;
  }

  /**
   * The normal transit reluctance is 1.0 - this is the baseline for all other costs. This parameter
   * is used to set a specific reluctance (other than 1.0) to some trips. For example most people
   * like TRAINS over other type of public transport, so it is possible to set the reluctance for
   * RAIL to e.g. 0.9 to give it a small advantage. The OTP domain is responsible for the mapping
   * between this arrays of reluctance values and the index in the {@link
   * RaptorTripSchedule#transitReluctanceFactorIndex()}. Raptor is agnostic to the meaning of the
   * index. But, it MUST match the the {@link RaptorTripSchedule#transitReluctanceFactorIndex()}.
   * <p>
   * If {@code null} is returned the default reluctance 1.0 is used.
   */
  @Nullable
  public double[] transitReluctanceFactors() {
    return transitReluctanceFactors;
  }

  public double waitReluctanceFactor() {
    return waitReluctanceFactor;
  }

  public WheelchairAccessibilityRequest accessibilityRequirements() {
    return accessibilityRequest;
  }

  public Set<FeedScopedId> unpreferredRoutes() {
    return unpreferredRoutes;
  }

  public DoubleFunction<Double> unnpreferredCost() {
    return unpreferredCost;
  }

  @Override
  public int hashCode() {
    return Objects.hash(boardCost, transferCost, waitReluctanceFactor);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    McCostParams that = (McCostParams) o;
    return (
      boardCost == that.boardCost &&
      transferCost == that.transferCost &&
      Double.compare(that.waitReluctanceFactor, waitReluctanceFactor) == 0
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(McCostParams.class)
      .addNum("boardCost", boardCost, 0)
      .addNum("transferCost", transferCost, 0)
      .addNum("waitReluctanceFactor", waitReluctanceFactor, 1.0)
      .addDoubles("transitReluctanceFactors", transitReluctanceFactors, 1.0)
      .addNum("routePenaltiesSize", unpreferredRoutes.size(), 0)
      .toString();
  }
}
