package org.opentripplanner.ext.fares.impl;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.ext.fares.model.FareAttribute;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareComponent;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Holds information for doing the graph search on fares */
class FareSearch {

  // Cell [i,j] holds the best (lowest) cost for a trip from rides[i] to rides[j]
  float[][] resultTable;

  // Cell [i,j] holds the index of the ride to pass through for the best cost
  // This is used for reconstructing which rides are grouped together
  int[][] next;

  // Cell [i,j] holds the id of the fare that corresponds to the relevant cost
  // we can't just use FareAndId for resultTable because you need to sum them
  FeedScopedId[][] fareIds;

  // Cell [i] holds the index of the last ride that ride[i] has a fare to
  // If it's -1, the ride does not have fares to anywhere
  int[] endOfComponent;

  FareSearch(int size) {
    resultTable = new float[size][size];
    next = new int[size][size];
    fareIds = new FeedScopedId[size][size];
    endOfComponent = new int[size];
    Arrays.fill(endOfComponent, -1);
  }
}

/** Holds fare and corresponding fareId */
class FareAndId {

  float fare;
  FeedScopedId fareId;

  FareAndId(float fare, FeedScopedId fareId) {
    this.fare = fare;
    this.fareId = fareId;
  }
}

/**
 * This fare service module handles the cases that GTFS handles within a single feed. It cannot
 * necessarily handle multi-feed graphs, because a rule-less fare attribute might be applied to
 * rides on routes in another feed, for example. For more interesting fare structures like New
 * York's MTA, or cities with multiple feeds and inter-feed transfer rules, you get to implement
 * your own FareService. See this thread on gtfs-changes explaining the proper interpretation of
 * fares.txt:
 * http://groups.google.com/group/gtfs-changes/browse_thread/thread/8a4a48ae1e742517/4f81b826cb732f3b
 */
public class DefaultFareServiceImpl implements FareService {

  private static final long serialVersionUID = 20120229L;

  private static final Logger LOG = LoggerFactory.getLogger(DefaultFareServiceImpl.class);

  /** For each fare type (regular, student, etc...) the collection of rules that apply. */
  protected Map<FareType, Collection<FareRuleSet>> fareRulesPerType;

  public DefaultFareServiceImpl() {
    fareRulesPerType = new HashMap<>();
  }

  public void addFareRules(FareType fareType, Collection<FareRuleSet> fareRules) {
    fareRulesPerType.put(fareType, new ArrayList<>(fareRules));
  }

  @Override
  public Fare getCost(Itinerary itinerary) {
    var fareLegs = itinerary
      .getLegs()
      .stream()
      .filter(l -> l instanceof ScheduledTransitLeg || l instanceof FlexibleTransitLeg)
      .map(Leg.class::cast)
      .toList();

    if (shouldCombineInterlinedLegs()) {
      fareLegs = combineInterlinedLegs(fareLegs);
    }

    // If there are no rides, there's no fare.
    if (fareLegs.isEmpty()) {
      return null;
    }

    Fare fare = new Fare();
    boolean hasFare = false;
    for (Map.Entry<FareType, Collection<FareRuleSet>> kv : fareRulesPerType.entrySet()) {
      FareType fareType = kv.getKey();
      Collection<FareRuleSet> fareRules = kv.getValue();
      // Get the currency from the first fareAttribute, assuming that all tickets use the same currency.
      Currency currency = null;
      if (fareRules.size() > 0) {
        currency =
          Currency.getInstance(fareRules.iterator().next().getFareAttribute().getCurrencyType());
      }
      hasFare = populateFare(fare, currency, fareType, fareLegs, fareRules);
    }
    return hasFare ? fare : null;
  }

  protected static Money getMoney(Currency currency, float cost) {
    int fractionDigits = 2;
    if (currency != null) {
      fractionDigits = currency.getDefaultFractionDigits();
    }
    int cents = (int) Math.round(cost * Math.pow(10, fractionDigits));
    return new Money(currency, cents);
  }

  protected float addFares(List<Leg> ride0, List<Leg> ride1, float cost0, float cost1) {
    return cost0 + cost1;
  }

  protected float getLowestCost(
    FareType fareType,
    List<Leg> rides,
    Collection<FareRuleSet> fareRules
  ) {
    FareSearch r = performSearch(fareType, rides, fareRules);
    return r.resultTable[0][rides.size() - 1];
  }

  /**
   * Builds the Fare object for the given currency, fareType and fareRules.
   * <p>
   * Besides calculating the lowest fare, we also break down the fare and which routes correspond to
   * which components. Note that even if we cannot get a lowest fare (if some rides don't have fare
   * rules), there will still be a breakdown for those parts which have fares.
   * <p>
   * As an example, given the rides A-B and B-C. Where A-B and B-C have fares of 10 each, 2 fare
   * detail objects are added, one with fare 10 for A-B and one with fare 10 for B-C.
   * <p>
   * If we add the rule for A-C with a fare of 15, we will get 1 fare detail object with fare 15,
   * which lists both A-B and B-C as routes involved.
   * <p>
   * If our only rule were A-B with a fare of 10, we would have no lowest fare, but we will still
   * have one fare detail with fare 10 for the route A-B. B-C will not just not be listed at all.
   */
  protected boolean populateFare(
    Fare fare,
    Currency currency,
    FareType fareType,
    List<Leg> legs,
    Collection<FareRuleSet> fareRules
  ) {
    FareSearch r = performSearch(fareType, legs, fareRules);

    List<FareComponent> details = new ArrayList<>();
    int count = 0;
    int start = 0;
    int end = legs.size() - 1;
    while (start <= end) {
      // skip parts where no fare is present, we want to return something
      // even if not all legs have fares
      while (start <= end && r.endOfComponent[start] < 0) {
        ++start;
      }
      if (start > end) {
        break;
      }

      int via = r.next[start][r.endOfComponent[start]];
      float cost = r.resultTable[start][via];
      FeedScopedId fareId = r.fareIds[start][via];
      FareComponent detail = new FareComponent(fareId, getMoney(currency, cost));
      for (int i = start; i <= via; ++i) {
        detail.addRoute(legs.get(i).getRoute().getId());
      }
      details.add(detail);
      ++count;
      start = via + 1;
    }

    fare.addFare(fareType, getMoney(currency, r.resultTable[0][legs.size() - 1]));
    fare.addFareDetails(fareType, details);
    return count > 0;
  }

  protected float calculateCost(
    FareType fareType,
    List<Leg> rides,
    Collection<FareRuleSet> fareRules
  ) {
    return getBestFareAndId(fareType, rides, fareRules).fare;
  }

  protected boolean shouldCombineInterlinedLegs() {
    return true;
  }

  private static List<Leg> combineInterlinedLegs(List<Leg> fareLegs) {
    var result = new ArrayList<Leg>();
    for (var leg : fareLegs) {
      if (leg.isInterlinedWithPreviousLeg() && leg instanceof ScheduledTransitLeg stl) {
        var previousLeg = (ScheduledTransitLeg) result.get(result.size() - 1);
        var combinedLeg = new ScheduledTransitLeg(
          previousLeg.getTripTimes(),
          previousLeg.getTripPattern(),
          0,
          0,
          previousLeg.getStartTime(),
          stl.getEndTime(),
          stl.getServiceDate(),
          stl.getZoneId(),
          null,
          null,
          0,
          null
        );
        result.add(result.size() - 1, combinedLeg);
      } else {
        result.add(leg);
      }
    }
    return result;
  }

  private FareSearch performSearch(
    FareType fareType,
    List<Leg> rides,
    Collection<FareRuleSet> fareRules
  ) {
    FareSearch r = new FareSearch(rides.size());

    // Dynamic algorithm to calculate fare cost.
    // This is a modified Floyd-Warshall algorithm, a key thing to remember is that
    // rides are already edges, so when comparing "via" routes, i -> k is connected
    // to k+1 -> j.
    for (int i = 0; i < rides.size(); i++) {
      // each diagonal
      for (int j = 0; j < rides.size() - i; j++) {
        FareAndId best = getBestFareAndId(fareType, rides.subList(j, j + i + 1), fareRules);
        float cost = best.fare;
        if (cost < 0) {
          LOG.error("negative cost for a ride sequence");
          cost = Float.POSITIVE_INFINITY;
        }
        if (cost < Float.POSITIVE_INFINITY) {
          r.endOfComponent[j] = j + i;
          r.next[j][j + i] = j + i;
        }
        r.resultTable[j][j + i] = cost;
        r.fareIds[j][j + i] = best.fareId;
        for (int k = 0; k < i; k++) {
          float via = addFares(
            rides.subList(j, j + k + 1),
            rides.subList(j + k + 1, j + i + 1),
            r.resultTable[j][j + k],
            r.resultTable[j + k + 1][j + i]
          );
          if (r.resultTable[j][j + i] > via) {
            r.resultTable[j][j + i] = via;
            r.endOfComponent[j] = j + i;
            r.next[j][j + i] = r.next[j][j + k];
          }
        }
      }
    }
    return r;
  }

  private FareAndId getBestFareAndId(
    FareType fareType,
    List<Leg> legs,
    Collection<FareRuleSet> fareRules
  ) {
    Set<String> zones = new HashSet<>();
    Set<FeedScopedId> routes = new HashSet<>();
    Set<FeedScopedId> trips = new HashSet<>();
    int transfersUsed = -1;

    var firstRide = legs.get(0);
    ZonedDateTime startTime = firstRide.getStartTime();
    String startZone = firstRide.getFrom().stop.getFirstZoneAsString();
    String endZone = firstRide.getTo().stop.getFirstZoneAsString();
    // stops don't really have an agency id, they have the per-feed default id
    String feedId = firstRide.getFrom().stop.getId().getFeedId();
    ZonedDateTime lastRideStartTime = firstRide.getStartTime();
    ZonedDateTime lastRideEndTime = firstRide.getEndTime();
    for (var leg : legs) {
      if (!leg.getFrom().stop.getId().getFeedId().equals(feedId)) {
        LOG.debug("skipped multi-feed ride sequence {}", legs);
        return new FareAndId(Float.POSITIVE_INFINITY, null);
      }
      lastRideStartTime = leg.getStartTime();
      lastRideEndTime = leg.getEndTime();
      endZone = leg.getTo().stop.getFirstZoneAsString();
      routes.add(leg.getRoute().getId());
      zones.addAll(leg.getFareZones().stream().map(z -> z.getId().getId()).toList());
      trips.add(leg.getTrip().getId());
      transfersUsed += 1;
    }

    FareAttribute bestAttribute = null;
    float bestFare = Float.POSITIVE_INFINITY;
    Duration tripTime = Duration.between(startTime, lastRideStartTime);
    Duration journeyTime = Duration.between(startTime, lastRideEndTime);

    // find the best fare that matches this set of rides
    for (FareRuleSet ruleSet : fareRules) {
      FareAttribute attribute = ruleSet.getFareAttribute();
      // fares also don't really have an agency id, they will have the per-feed default id
      // check only if the fare is not mapped to an agency
      if (!attribute.getId().getFeedId().equals(feedId)) continue;

      if (ruleSet.matches(startZone, endZone, zones, routes, trips)) {
        // TODO Maybe move the code below in FareRuleSet::matches() ?
        if (attribute.isTransfersSet() && attribute.getTransfers() < transfersUsed) {
          continue;
        }
        // assume transfers are evaluated at boarding time,
        // as trimet does
        if (
          attribute.isTransferDurationSet() &&
          tripTime.getSeconds() > attribute.getTransferDuration()
        ) {
          continue;
        }
        if (
          attribute.isJourneyDurationSet() &&
          journeyTime.getSeconds() > attribute.getJourneyDuration()
        ) {
          continue;
        }
        float newFare = getFarePrice(attribute, fareType);
        if (newFare < bestFare) {
          bestAttribute = attribute;
          bestFare = newFare;
        }
      }
    }
    LOG.debug("{} best for {}", bestAttribute, legs);
    if (bestFare == Float.POSITIVE_INFINITY) {
      LOG.debug("No fare for a ride sequence: {}", legs);
    }
    return new FareAndId(bestFare, bestAttribute == null ? null : bestAttribute.getId());
  }

  private float getFarePrice(FareAttribute fare, FareType type) {
    switch (type) {
      case senior:
        if (fare.getSeniorPrice() >= 0) {
          return fare.getSeniorPrice();
        }
        break;
      case youth:
        if (fare.getYouthPrice() >= 0) {
          return fare.getYouthPrice();
        }
        break;
      case regular:
      default:
        break;
    }
    return fare.getPrice();
  }
}
