package org.opentripplanner.standalone.api;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.opentripplanner.inspector.TileRendererManager;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;

/**
 * The purpose of this class is to allow APIs (HTTP Resources) to access the OTP Server Context.
 * By using an interface, and not injecting each service class we avoid giving the resources access
 * to the server implementation. The context is injected by Jersey. An alternative to injecting this
 * interface is to inject each individual component in the context - hence reducing the dependencies
 * further. But there is not a "real" need for this. For example, we do not have unit tests on the
 * Resources. If we in the future would decide to write unit tests for the APIs, then we could
 * eliminate this interface and just inject the components. See the bind method in OTPServer.
 * <p>
 * The OTP Server and the implementation of this class is responsible to make the necessary
 * instances for request scoped objects. Hence; the user of this can assume that:
 * <ol>
 *   <li>Calling the methods of this interface will return the same object every time.</li>
 *   <li>
 *     If the returned component is mutable, then it will be a unique copy for each HTTP request.
 *   </li>
 * </ol>
 * <p>
 * This class is not THREAD-SAFE, each HTTP request gets its own copy, but if there are multiple
 * threads witch accesses this context within the HTTP Request, then the caller is responsible
 * for the synchronization. Only request scoped components need to be synchronized - they are
 * potentially lazy initialized.
 */
public interface OtpServerContext {
  /**
   * A RoutingRequest containing default parameters that will be cloned when handling each request.
   */
  @HttpRequestScoped
  RoutingRequest defaultRoutingRequest();

  /**
   * Return the default routing request locale(without cloning the request).
   */
  Locale defaultLocale();

  RouterConfig routerConfig();

  RaptorConfig<TripSchedule> raptorConfig();

  Graph graph();

  @HttpRequestScoped
  TransitService transitService();

  /**
   * Get a request-scoped {@link RoutingService} valid for one HTTP request. It guarantees that
   * the data and services used are consistent and operate on the same transit snapshot. Any
   * realtime update that happens during the request will not affect the returned service and will
   * not be visible to the request.
   */
  @HttpRequestScoped
  RoutingService routingService();

  MeterRegistry meterRegistry();

  /**
   * Separate logger for incoming requests. This should be handled with a Logback logger rather than
   * something simple like a PrintStream because requests come in multi-threaded.
   */
  Logger requestLogger();

  /** Inspector/debug services */
  TileRendererManager tileRendererManager();

  /**
   * Callback witch is injected into the {@code DirectStreetRouter}, used to visualize the
   * search.
   */
  TraverseVisitor traverseVisitor();
}
