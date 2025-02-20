package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;

class TripTest {

  private static final String ID = "1";
  private static final String SHORT_NAME = "name";
  private static final WheelchairAccessibility WHEELCHAIR_ACCESSIBILITY =
    WheelchairAccessibility.POSSIBLE;
  public static final Route ROUTE = TransitModelForTest.route("routeId").build();
  private static final Direction DIRECTION = Direction.INBOUND;
  public static final String HEAD_SIGN = "head sign";
  private static final BikeAccess BIKE_ACCESS = BikeAccess.ALLOWED;
  private static final TransitMode TRANSIT_MODE = TransitMode.BUS;
  private static final String BLOCK_ID = "blockId";
  private static final String FARE_ID = "fareId";
  private static final TripAlteration TRIP_ALTERATION = TripAlteration.CANCELLATION;
  private static final String NETEX_SUBMODE_NAME = "submode";
  private static final SubMode NETEX_SUBMODE = SubMode.of(NETEX_SUBMODE_NAME);
  private static final String NETEX_INTERNAL_PLANNING_CODE = "internalPlanningCode";
  private static final Operator OPERATOR = Operator
    .of(FeedScopedId.parseId("x:operatorId"))
    .withName("operator name")
    .build();
  private static final FeedScopedId SERVICE_ID = FeedScopedId.parseId("x:serviceId");
  private static final FeedScopedId SHAPE_ID = FeedScopedId.parseId("x:shapeId");
  private static final Trip subject = Trip
    .of(TransitModelForTest.id(ID))
    .withShortName(SHORT_NAME)
    .withRoute(ROUTE)
    .withDirection(DIRECTION)
    .withHeadsign(HEAD_SIGN)
    .withBikesAllowed(BIKE_ACCESS)
    .withMode(TRANSIT_MODE)
    .withGtfsBlockId(BLOCK_ID)
    .withGtfsFareId(FARE_ID)
    .withNetexAlteration(TRIP_ALTERATION)
    .withNetexSubmode(NETEX_SUBMODE_NAME)
    .withNetexInternalPlanningCode(NETEX_INTERNAL_PLANNING_CODE)
    .withOperator(OPERATOR)
    .withServiceId(SERVICE_ID)
    .withShapeId(SHAPE_ID)
    .withWheelchairBoarding(WHEELCHAIR_ACCESSIBILITY)
    .build();

  @Test
  void copy() {
    assertEquals(ID, subject.getId().getId());

    // Make a copy
    var copy = subject.copy().build();

    assertEquals(ID, copy.getId().getId());
    assertEquals(WHEELCHAIR_ACCESSIBILITY, copy.getWheelchairBoarding());
    assertEquals(SHORT_NAME, copy.getShortName());
    assertEquals(ROUTE, copy.getRoute());
    assertEquals(DIRECTION, copy.getDirection());
    assertEquals(HEAD_SIGN, copy.getHeadsign());
    assertEquals(BIKE_ACCESS, copy.getBikesAllowed());
    assertEquals(TRANSIT_MODE, copy.getMode());
    assertEquals(BLOCK_ID, copy.getGtfsBlockId());
    assertEquals(FARE_ID, copy.getGtfsFareId());
    assertEquals(TRIP_ALTERATION, copy.getNetexAlteration());
    assertEquals(NETEX_SUBMODE, copy.getNetexSubMode());
    assertEquals(NETEX_INTERNAL_PLANNING_CODE, copy.getNetexInternalPlanningCode());
    assertEquals(OPERATOR, copy.getOperator());
    assertEquals(SERVICE_ID, copy.getServiceId());
    assertEquals(SHAPE_ID, copy.getShapeId());
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(TransitModelForTest.id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withShortName("X").build()));
    assertFalse(
      subject.sameAs(
        subject.copy().withWheelchairBoarding(WheelchairAccessibility.NOT_POSSIBLE).build()
      )
    );
    assertFalse(
      subject.sameAs(
        subject.copy().withRoute(TransitModelForTest.route("otherRouteId").build()).build()
      )
    );
    assertFalse(subject.sameAs(subject.copy().withDirection(Direction.OUTBOUND).build()));
    assertFalse(subject.sameAs(subject.copy().withHeadsign("X").build()));
    assertFalse(subject.sameAs(subject.copy().withBikesAllowed(BikeAccess.NOT_ALLOWED).build()));
    assertFalse(subject.sameAs(subject.copy().withMode(TransitMode.RAIL).build()));
    assertFalse(subject.sameAs(subject.copy().withGtfsBlockId("X").build()));
    assertFalse(subject.sameAs(subject.copy().withGtfsFareId("X").build()));
    assertFalse(
      subject.sameAs(subject.copy().withNetexAlteration(TripAlteration.REPLACED).build())
    );
    assertFalse(subject.sameAs(subject.copy().withNetexSubmode("X").build()));
    assertFalse(subject.sameAs(subject.copy().withNetexInternalPlanningCode("X").build()));
    assertFalse(
      subject.sameAs(
        subject
          .copy()
          .withOperator(
            Operator
              .of(FeedScopedId.parseId("x:otherOperatorId"))
              .withName("other operator name")
              .build()
          )
          .build()
      )
    );
    assertFalse(
      subject.sameAs(subject.copy().withServiceId(FeedScopedId.parseId("x:otherServiceId")).build())
    );
    assertFalse(
      subject.sameAs(subject.copy().withShapeId(FeedScopedId.parseId("x:otherShapeId")).build())
    );
  }
}
