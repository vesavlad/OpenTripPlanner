package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.service.TransitService;

public class FlexAccessEgressRouter {

  private FlexAccessEgressRouter() {}

  public static Collection<FlexAccessEgress> routeAccessEgress(
    RoutingContext routingContext,
    TransitService transitService,
    AdditionalSearchDays searchDays,
    FlexParameters params,
    boolean isEgress
  ) {
    Collection<NearbyStop> accessStops = !isEgress
      ? AccessEgressRouter.streetSearch(routingContext, transitService, StreetMode.WALK, false)
      : List.of();

    Collection<NearbyStop> egressStops = isEgress
      ? AccessEgressRouter.streetSearch(routingContext, transitService, StreetMode.WALK, true)
      : List.of();

    FlexRouter flexRouter = new FlexRouter(
      routingContext.graph,
      transitService,
      params,
      routingContext.opt.getDateTime(),
      routingContext.opt.arriveBy,
      searchDays.additionalSearchDaysInPast(),
      searchDays.additionalSearchDaysInFuture(),
      accessStops,
      egressStops
    );

    return isEgress ? flexRouter.createFlexEgresses() : flexRouter.createFlexAccesses();
  }
}
