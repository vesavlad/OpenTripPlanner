package org.opentripplanner.ext.fares.impl;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.fares.model.FareRulesData;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeattleFareServiceFactory extends DefaultFareServiceFactory {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(SeattleFareServiceFactory.class);

  @Override
  public FareService makeFareService() {
    SeattleFareServiceImpl fareService = new SeattleFareServiceImpl();
    fareService.addFareRules(FareType.regular, regularFareRules.values());
    fareService.addFareRules(FareType.youth, regularFareRules.values());
    fareService.addFareRules(FareType.senior, regularFareRules.values());

    return fareService;
  }

  @Override
  public void processGtfs(FareRulesData fareRuleService, OtpTransitService transitService) {
    // Add custom extension: trips may have a fare ID specified in KCM GTFS.
    // Need to ensure that we are scoped to feed when adding trips to FareRuleSet,
    // since fare IDs may not be unique across feeds and trip agency IDsqq
    // may not match fare attribute agency IDs (which are feed IDs).

    Map<FeedScopedId, FareRuleSet> feedFareRules = new HashMap<>();
    fillFareRules(fareRuleService.fareAttributes(), fareRuleService.fareRules(), feedFareRules);

    regularFareRules.putAll(feedFareRules);

    Map<String, FareRuleSet> feedFareRulesById = new HashMap<>();

    for (FareRuleSet rule : regularFareRules.values()) {
      String id = rule.getFareAttribute().getId().getId();
      feedFareRulesById.put(id, rule);
    }

    for (Trip trip : transitService.getAllTrips()) {
      String fareId = trip.getGtfsFareId();
      FareRuleSet rule = feedFareRulesById.get(fareId);
      if (rule != null) rule.addTrip(trip.getId());
    }
  }

  @Override
  public void configure(JsonNode config) {
    // No config for the moment
  }
}
