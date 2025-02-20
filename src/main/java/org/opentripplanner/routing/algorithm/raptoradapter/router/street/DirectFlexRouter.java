package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerContext;

public class DirectFlexRouter {

  public static List<Itinerary> route(
    OtpServerContext serverContext,
    RoutingRequest request,
    AdditionalSearchDays additionalSearchDays
  ) {
    if (!StreetMode.FLEXIBLE.equals(request.modes.directMode)) {
      return Collections.emptyList();
    }
    RoutingRequest directRequest = request.getStreetSearchRequest(request.modes.directMode);
    try (
      var temporaryVertices = new TemporaryVerticesContainer(serverContext.graph(), directRequest)
    ) {
      RoutingContext routingContext = new RoutingContext(
        directRequest,
        serverContext.graph(),
        temporaryVertices
      );

      // Prepare access/egress transfers
      Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(
        routingContext,
        serverContext.transitService(),
        StreetMode.WALK,
        false
      );
      Collection<NearbyStop> egressStops = AccessEgressRouter.streetSearch(
        routingContext,
        serverContext.transitService(),
        StreetMode.WALK,
        true
      );

      FlexRouter flexRouter = new FlexRouter(
        serverContext.graph(),
        serverContext.transitService(),
        serverContext.routerConfig().flexParameters(request),
        directRequest.getDateTime(),
        directRequest.arriveBy,
        additionalSearchDays.additionalSearchDaysInPast(),
        additionalSearchDays.additionalSearchDaysInFuture(),
        accessStops,
        egressStops
      );

      return new ArrayList<>(flexRouter.createFlexOnlyItineraries());
    }
  }
}
