package org.opentripplanner.transit.model.site;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.NonLocalizedString;

class GroupOfStationsTest {

  private static final String ID = "1";
  private static final I18NString NAME = new NonLocalizedString("name");

  private static final Station STATION = TransitModelForTest.station("1:station").build();

  private static final StopLocation STOP_LOCATION = TransitModelForTest.stopForTest(
    "1:stop",
    1d,
    1d,
    STATION
  );

  private static final GroupOfStations subject = GroupOfStations
    .of(TransitModelForTest.id(ID))
    .withName(NAME)
    .addChildStation(STATION)
    .build();

  @Test
  void copy() {
    assertEquals(ID, subject.getId().getId());

    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().withName(NAME).build();

    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().withName(new NonLocalizedString("v2")).build();

    // The two objects are not the same instance, but are equal(same id)
    assertNotSame(subject, copy);
    assertEquals(subject, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals(STOP_LOCATION, copy.getChildStops().iterator().next());
    assertEquals("v2", copy.getName().toString());
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(TransitModelForTest.id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withName(new NonLocalizedString("X")).build()));
    assertFalse(
      subject.sameAs(
        subject.copy().addChildStation(TransitModelForTest.station("2:station").build()).build()
      )
    );
  }
}
