package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.transit.model._data.TransitModelForTest.FEED_ID;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;

class HighestFareInFreeTransferWindowFareServiceTest implements PlanTestConstants {

  static Agency agency = Agency
    .of(id("agency"))
    .withName("Houston")
    .withTimezone("America/Chicago")
    .build();

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("createTestCases")
  public void canCalculateFare(
    String testCaseName, // used to create parameterized test name
    FareService fareService,
    Itinerary i,
    float expectedFare
  ) {
    Assertions.assertEquals(
      Money.usDollars(Math.round(expectedFare * 100)),
      fareService.getCost(i).getFare(Fare.FareType.regular)
    );
  }

  private static List<Arguments> createTestCases() {
    List<Arguments> args = new LinkedList<>();

    // create routes
    Route routeA = route("A", "Route with one dollar fare");
    Route routeB = route("B", "Route with one dollar fare");
    Route routeC = route("C", "Route with two dollar fare");

    // create fare attributes and rules
    List<FareRuleSet> defaultFareRules = new LinkedList<>();

    // $1 fares
    float oneDollar = 1.0f;
    FareAttribute oneDollarFareAttribute = new FareAttribute(
      new FeedScopedId(FEED_ID, "oneDollarAttribute")
    );
    oneDollarFareAttribute.setCurrencyType("USD");
    oneDollarFareAttribute.setPrice(oneDollar);
    FareRuleSet oneDollarRouteBasedFares = new FareRuleSet(oneDollarFareAttribute);
    oneDollarRouteBasedFares.addRoute(routeA.getId());
    oneDollarRouteBasedFares.addRoute(routeB.getId());
    defaultFareRules.add(oneDollarRouteBasedFares);

    // $2 fares
    float twoDollars = 2.0f;
    FareAttribute twoDollarFareAttribute = new FareAttribute(
      new FeedScopedId(FEED_ID, "twoDollarAttribute")
    );
    twoDollarFareAttribute.setCurrencyType("USD");
    twoDollarFareAttribute.setPrice(twoDollars);

    FareRuleSet twoDollarRouteBasedFares = new FareRuleSet(twoDollarFareAttribute);
    twoDollarRouteBasedFares.addRoute(routeC.getId());
    defaultFareRules.add(twoDollarRouteBasedFares);

    FareService defaultFareService = new HighestFareInFreeTransferWindowFareService(
      defaultFareRules,
      Duration.ofMinutes(150),
      false
    );

    // a single transit leg should calculate default fare
    Itinerary singleTransitLegPath = newItinerary(A, T11_06)
      .bus(routeA, 1, T11_06, T11_12, B)
      .build();

    args.add(
      Arguments.of("Single transit leg", defaultFareService, singleTransitLegPath, oneDollar)
    );

    // a two transit leg itinerary with same route costs within free-transfer window should calculate default fare
    Itinerary twoTransitLegPath = newItinerary(A, 30)
      .bus(routeA, 1, 30, 60, B)
      .bus(routeB, 2, 90, 120, C)
      .build();

    args.add(
      Arguments.of(
        "Two transit legs in free transfer window",
        defaultFareService,
        twoTransitLegPath,
        oneDollar
      )
    );

    // a two transit leg itinerary where the first route costs more than the second route, but both are within
    // the free transfer window should calculate the first route cost
    Itinerary twoTransitLegFirstRouteCostsTwoDollarsPath = newItinerary(A, 30)
      .bus(routeC, 1, 30, 60, B)
      .bus(routeB, 2, 90, 120, C)
      .build();

    args.add(
      Arguments.of(
        "Two transit legs in free transfer window, first leg more expensive",
        defaultFareService,
        twoTransitLegFirstRouteCostsTwoDollarsPath,
        twoDollars
      )
    );

    // a two transit leg itinerary where the second route costs more than the first route, but both are within
    // the free transfer window should calculate the second route cost
    Itinerary twoTransitLegSecondRouteCostsTwoDollarsPath = newItinerary(A, 30)
      .bus(routeB, 1, 30, 60, B)
      .bus(routeC, 2, 90, 120, C)
      .build();
    args.add(
      Arguments.of(
        "Two transit legs in free transfer window, second leg more expensive",
        defaultFareService,
        twoTransitLegSecondRouteCostsTwoDollarsPath,
        twoDollars
      )
    );

    // a two transit leg itinerary with same route costs but where the second leg begins after the free transfer
    // window should calculate double the default fare
    Itinerary twoTransitLegSecondRouteHappensAfterFreeTransferWindowPath = newItinerary(A, 30)
      .bus(routeA, 1, 30, 60, B)
      .bus(routeB, 2, 10000, 10120, C)
      .build();
    args.add(
      Arguments.of(
        "Two transit legs, second leg starts outside free transfer window",
        defaultFareService,
        twoTransitLegSecondRouteHappensAfterFreeTransferWindowPath,
        oneDollar + oneDollar
      )
    );

    // a three transit leg itinerary with same route costs but where the second leg begins after the free transfer
    // window, but the third leg is within the second free transfer window should calculate double the default fare
    Itinerary threeTransitLegSecondRouteHappensAfterFreeTransferWindowPath = newItinerary(A, 30)
      .bus(routeA, 1, 30, 60, B)
      .bus(routeB, 2, 10000, 10120, C)
      .bus(routeA, 3, 10150, 10180, D)
      .build();
    args.add(
      Arguments.of(
        "Three transit legs, second leg starts outside free transfer window, third leg within second free transfer window",
        defaultFareService,
        threeTransitLegSecondRouteHappensAfterFreeTransferWindowPath,
        oneDollar + oneDollar
      )
    );

    // a three transit leg itinerary with same route costs but where each leg begins after the free transfer window,
    // should calculate triple the default fare
    Itinerary threeTransitLegAllOutsideFreeTransferWindowPath = newItinerary(A, 30)
      .bus(routeA, 1, 30, 60, B)
      .bus(routeB, 2, 10000, 10120, C)
      .bus(routeA, 3, 20000, 20180, D)
      .build();
    args.add(
      Arguments.of(
        "Three transit legs, all starting outside free transfer window",
        defaultFareService,
        threeTransitLegAllOutsideFreeTransferWindowPath,
        oneDollar + oneDollar + oneDollar
      )
    );

    // a two transit leg itinerary with an interlined transfer where the second route costs more than the first
    // route, but both are within the free transfer window should calculate the first route cost
    Itinerary twoTransitLegInterlinedSecondRouteCostsTwoDollarsPath = newItinerary(A, 30)
      .bus(routeB, 1, 30, 60, B)
      .staySeatedBus(routeC, 2, 90, 120, C)
      .build();
    args.add(
      Arguments.of(
        "Two interlined transit legs, second leg more expensive should calculate first leg fare with default config",
        defaultFareService,
        twoTransitLegInterlinedSecondRouteCostsTwoDollarsPath,
        oneDollar
      )
    );

    // a two transit leg itinerary with an interlined transfer where the second route costs more than the first
    // route, but both are within the free transfer window and the fare service is configured with the
    // analyzeInterlinedTransfers set to true should calculate the second route cost
    FareService fareServiceWithAnalyzedInterlinedTransfersConfig = new HighestFareInFreeTransferWindowFareService(
      defaultFareRules,
      Duration.ofMinutes(150),
      true
    );

    args.add(
      Arguments.of(
        "Two interlined transit legs, second leg more expensive should calculate second leg fare with alternate config",
        fareServiceWithAnalyzedInterlinedTransfersConfig,
        twoTransitLegInterlinedSecondRouteCostsTwoDollarsPath,
        twoDollars
      )
    );

    return args;
  }

  private static Route route(String id, String name) {
    return Route.of(id(id)).withLongName(name).withAgency(agency).withMode(TransitMode.BUS).build();
  }
}
