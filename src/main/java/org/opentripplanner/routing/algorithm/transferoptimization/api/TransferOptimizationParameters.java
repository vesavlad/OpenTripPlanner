package org.opentripplanner.routing.algorithm.transferoptimization.api;


import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.transit.raptor.api.path.Path;

/**
 * @see org.opentripplanner.routing.algorithm.transferoptimization package documantation.
 */
public interface TransferOptimizationParameters {

  /**
   * If enabled, all paths will be optimized with respect to the transfer point to minimise
   * the {@link org.opentripplanner.model.transfer.Transfer#priorityCost(Transfer)}.
   */
  boolean optimizeTransferPriority();

  /**
   * This enables the transfer wait time optimization. If not enabled the {@link
   * Path#generalizedCost()} function is used to pick the optimal transfer point.
   */
  boolean optimizeTransferWaitTime();

  /**
   * This is the wait-reluctance used during the Raptor search. It is used by the transfer
   * optimization service to remove the wait-cost from path generalized-cost before a new
   * optimized-transfer-cost is added.
   */
  double waitReluctanceRouting();

  /**
   * This factor is multiplied with the total wait time for a given path. This cost is then
   * <em>subtracted</em> from the generalized-cost to maximise the wait time for a given path with
   * several different transfer alternatives.
   */
  double inverseWaitReluctance();

  /**
   * This defines the maximum cost for the logarithmic function relative to the {@code
   * min-safe-transfer-time (t0)} when wait time goes towards zero(0).
   * <pre>
   * f(0) = n * t0
   * </pre>
   */
  double minSafeWaitTimeFactor();
}
