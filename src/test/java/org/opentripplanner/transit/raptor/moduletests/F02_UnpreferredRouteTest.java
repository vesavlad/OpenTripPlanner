package org.opentripplanner.transit.raptor.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RouteCostCalculator.DEFAULT_ROUTE_RELUCTANCE;
import static org.opentripplanner.transit.raptor._data.api.PathUtils.pathsToString;
import static org.opentripplanner.transit.raptor._data.transit.TestRoute.route;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor._data.transit.TestTripSchedule.schedule;

import java.util.Set;
import java.util.function.DoubleFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.RequestFunctions;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;

/**
 * FEATURE UNDER TEST
 * <p>
 * On transit options with identical cost, raptor should drop the unpreferred one which is modeled
 * by route penalty.
 */
public class F02_UnpreferredRouteTest implements RaptorTestConstants {

  private static final String EXPECTED =
    "Walk 30s ~ A ~ BUS %s 0:01 0:02:40 ~ B ~ Walk 20s " + "[0:00:30 0:03 2m30s 0tx $%d]";
  private static final FeedScopedId ROUTE_ID_1 = TransitModelForTest.id("1");
  private static final FeedScopedId ROUTE_ID_2 = TransitModelForTest.id("2");
  private static final DoubleFunction<Double> UNPREFER_COST = RequestFunctions.createLinearFunction(
    30000,
    DEFAULT_ROUTE_RELUCTANCE
  );
  private final TestTransitData data = new TestTransitData();
  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder = new RaptorRequestBuilder<>();
  private final RaptorService<TestTripSchedule> raptorService = new RaptorService<>(
    RaptorConfig.defaultConfigForTest()
  );

  @BeforeEach
  public void setup() {
    // Given 2 identical routes R1 and R2
    data.withRoute(
      route(pattern("R1", STOP_A, STOP_B))
        .withTimetable(schedule("00:01, 00:02:40").routeId(ROUTE_ID_1))
    );
    data.withRoute(
      route(pattern("R2", STOP_A, STOP_B))
        .withTimetable(schedule("00:01, 00:02:40").routeId(ROUTE_ID_2))
    );

    requestBuilder
      .searchParams()
      .addAccessPaths(walk(STOP_A, D30s))
      .addEgressPaths(walk(STOP_B, D20s))
      .earliestDepartureTime(T00_00)
      .latestArrivalTime(T00_10)
      .timetableEnabled(true);

    requestBuilder.profile(RaptorProfile.MULTI_CRITERIA);

    ModuleTestDebugLogging.setupDebugLogging(data, requestBuilder);
  }

  @Test
  public void unpreferR1() {
    unpreferRoute(ROUTE_ID_1);

    var request = requestBuilder.build();
    var response = raptorService.route(request, data);

    // Verify R1 is preferred and the cost is correct
    assertEquals(expected("R2", 800), pathsToString(response));
  }

  @Test
  public void unpreferR2() {
    unpreferRoute(ROUTE_ID_2);

    var request = requestBuilder.build();
    var response = raptorService.route(request, data);

    assertEquals(expected("R1", 800), pathsToString(response));
  }

  private void unpreferRoute(FeedScopedId routeId) {
    data.mcCostParamsBuilder().unpreferredRoutes(Set.of(routeId));
    data.mcCostParamsBuilder().unpreferredCost(UNPREFER_COST);
  }

  private static String expected(String route, int cost) {
    return String.format(EXPECTED, route, cost);
  }
}
