package com.aerosaga;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionLaunchRequestTest {
    private static final double BASE_LAT = 28.6139, BASE_LON = 77.2090;

    private MissionLaunchRequest request(String id, Double lat, Double lon) {
        MissionLaunchRequest r = new MissionLaunchRequest();
        r.droneId = id;
        r.targetLat = lat;
        r.targetLon = lon;
        return r;
    }

    @Test
    void validRequestHasNoErrors() {
        assertTrue(request("drone-7", 28.7, 77.3).validate(BASE_LAT, BASE_LON, 1.0).isEmpty());
    }

    @Test
    void badDroneIdIsRejected() {
        assertEquals(1, request("plane-1", 28.7, 77.3).validate(BASE_LAT, BASE_LON, 1.0).size());
    }

    @Test
    void missingTargetIsRejected() {
        assertEquals(1, request("drone-1", null, 77.3).validate(BASE_LAT, BASE_LON, 1.0).size());
    }

    @Test
    void targetTooFarFromBaseIsRejected() {
        assertEquals(1, request("drone-1", 35.0, 77.3).validate(BASE_LAT, BASE_LON, 1.0).size());
    }

    @Test
    void outOfRangeCoordinatesAreRejected() {
        assertTrue(request("drone-1", 95.0, 190.0).validate(BASE_LAT, BASE_LON, 1.0).size() >= 2);
    }
}
