package org.opentripplanner.transit.raptor.rangeraptor.standard.debug;

import java.util.Collection;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.TransitArrival;
import org.opentripplanner.transit.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.transit.raptor.rangeraptor.internalapi.RoundProvider;
import org.opentripplanner.transit.raptor.rangeraptor.standard.internalapi.StopArrivalsState;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view.StopsCursor;

/**
 * The responsibility of this class is to wrap a {@link StopArrivalsState} and notify the {@link
 * org.opentripplanner.transit.raptor.rangeraptor.standard.debug.StateDebugger} about all stop
 * arrival events.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class DebugStopArrivalsState<T extends RaptorTripSchedule>
  implements StopArrivalsState<T> {

  private final StopArrivalsState<T> delegate;
  private final StateDebugger<T> debug;

  /**
   * Create a Standard range raptor state for the given context
   */
  public DebugStopArrivalsState(
    RoundProvider roundProvider,
    DebugHandlerFactory<T> dFactory,
    StopsCursor<T> stopsCursor,
    StopArrivalsState<T> delegate
  ) {
    this.debug = new StateDebugger<>(stopsCursor, roundProvider, dFactory);
    this.delegate = delegate;
  }

  @Override
  public void setAccessTime(int arrivalTime, RaptorTransfer access, boolean bestTime) {
    delegate.setAccessTime(arrivalTime, access, bestTime);
    debug.acceptAccessPath(access.stop(), access);
  }

  @Override
  public void rejectAccessTime(int arrivalTime, RaptorTransfer access) {
    debug.rejectAccessPath(access, arrivalTime);
    delegate.rejectAccessTime(arrivalTime, access);
  }

  @Override
  public int bestTimePreviousRound(int stop) {
    return delegate.bestTimePreviousRound(stop);
  }

  @Override
  public void setNewBestTransitTime(
    int stop,
    int alightTime,
    T trip,
    int boardStop,
    int boardTime,
    boolean newBestOverall
  ) {
    debug.dropOldStateAndAcceptNewOnBoardArrival(
      stop,
      newBestOverall,
      () ->
        delegate.setNewBestTransitTime(stop, alightTime, trip, boardStop, boardTime, newBestOverall)
    );
  }

  @Override
  public void rejectNewBestTransitTime(
    int stop,
    int alightTime,
    T trip,
    int boardStop,
    int boardTime
  ) {
    debug.rejectTransit(stop, alightTime, trip, boardStop, boardTime);
    delegate.rejectNewBestTransitTime(stop, alightTime, trip, boardStop, boardTime);
  }

  @Override
  public void setNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transfer) {
    debug.dropOldStateAndAcceptNewOnStreetArrival(
      transfer.stop(),
      () -> delegate.setNewBestTransferTime(fromStop, arrivalTime, transfer)
    );
  }

  @Override
  public void rejectNewBestTransferTime(int fromStop, int arrivalTime, RaptorTransfer transfer) {
    debug.rejectTransfer(fromStop, transfer, transfer.stop(), arrivalTime);
    delegate.rejectNewBestTransferTime(fromStop, arrivalTime, transfer);
  }

  @Override
  public TransitArrival<T> previousTransit(int boardStopIndex) {
    return delegate.previousTransit(boardStopIndex);
  }

  @Override
  public Collection<Path<T>> extractPaths() {
    return delegate.extractPaths();
  }

  @Override
  public int calculateMinNumberOfTransfers(int stopIndex) {
    return delegate.calculateMinNumberOfTransfers(stopIndex);
  }
}
