package org.opentripplanner.routing.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class TraverseModeSetTest {

    @Test
    public void testCarMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.CAR);
        
        assertTrue(modeSet.getCar());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getBicycle());
        assertFalse(modeSet.getTrolleyBus());
        assertFalse(modeSet.getMonorail());
    }

    @Test
    public void testWalkMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.WALK);
        
        assertTrue(modeSet.getWalk());
        assertFalse(modeSet.getCar());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getBicycle());
        assertFalse(modeSet.getTrolleyBus());
        assertFalse(modeSet.getMonorail());
    }
    
    @Test
    public void testBikeMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.BICYCLE);

        assertTrue(modeSet.getBicycle());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getCar());
        assertFalse(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getTrolleyBus());
        assertFalse(modeSet.getMonorail());
    }


    @Test
    public void testTrolleyBusMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.TROLLEYBUS);

        assertFalse(modeSet.getBicycle());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getCar());
        assertTrue(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getWalk());
        assertTrue(modeSet.getTrolleyBus());
    }

    @Test
    public void testMonorailMode() {
        TraverseModeSet modeSet = new TraverseModeSet(TraverseMode.MONORAIL);

        assertFalse(modeSet.getBicycle());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getCar());
        assertTrue(modeSet.isTransit());
        assertFalse(modeSet.getRail());
        assertFalse(modeSet.getTram());
        assertFalse(modeSet.getSubway());
        assertFalse(modeSet.getFunicular());
        assertFalse(modeSet.getGondola());
        assertFalse(modeSet.getWalk());
        assertFalse(modeSet.getTrolleyBus());
        assertTrue(modeSet.getMonorail());
    }
}
