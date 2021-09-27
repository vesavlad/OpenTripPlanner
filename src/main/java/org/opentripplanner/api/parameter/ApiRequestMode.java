package org.opentripplanner.api.parameter;

import org.opentripplanner.model.TransitMode;

public enum ApiRequestMode {
    WALK, BICYCLE, CAR,
    TRAM, SUBWAY, RAIL, BUS, COACH, FERRY,
    CABLE_CAR, GONDOLA, FUNICULAR, TROLLEYBUS, MONORAIL, SCHOOL_BUS,
    TRANSIT, AIRPLANE, FLEX;

    public static ApiRequestMode fromTransitMode(TransitMode transitMode) {
        switch (transitMode) {
            case RAIL:
                return RAIL;
            case COACH:
                return COACH;
            case SUBWAY:
                return SUBWAY;
            case BUS:
                return BUS;
            case TRAM:
                return TRAM;
            case FERRY:
                return FERRY;
            case AIRPLANE:
                return AIRPLANE;
            case CABLE_CAR:
                return CABLE_CAR;
            case GONDOLA:
                return GONDOLA;
            case FUNICULAR:
                return FUNICULAR;
            case TROLLEYBUS:
                return TROLLEYBUS;
            case MONORAIL:
                return MONORAIL;
            case SCHOOL_BUS:
                return SCHOOL_BUS;
            default:
                throw new IllegalArgumentException("Can't convert to ApiRequestMode: " + transitMode);
        }
    }
}
