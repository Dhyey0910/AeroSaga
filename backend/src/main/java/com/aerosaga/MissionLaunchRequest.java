package com.aerosaga;

import java.util.ArrayList;
import java.util.List;

/** Body of POST /api/missions (launch one drone). */
public class MissionLaunchRequest {
    public String droneId;
    public Double targetLat;
    public Double targetLon;
    public boolean failDrop;

    public MissionLaunchRequest() {}

    /** Returns a list of problems; an empty list means the request is valid. */
    public List<String> validate(double baseLat, double baseLon, double maxOffsetDegrees) {
        List<String> errors = new ArrayList<>();

        if (droneId == null || !droneId.matches("drone-\\d{1,3}")) {
            errors.add("droneId must look like 'drone-7' (drone- followed by 1 to 3 digits)");
        }
        if (targetLat == null || targetLon == null) {
            errors.add("targetLat and targetLon are required");
            return errors;
        }
        if (targetLat < -90 || targetLat > 90) {
            errors.add("targetLat must be between -90 and 90");
        }
        if (targetLon < -180 || targetLon > 180) {
            errors.add("targetLon must be between -180 and 180");
        }
        if (Math.abs(targetLat - baseLat) > maxOffsetDegrees || Math.abs(targetLon - baseLon) > maxOffsetDegrees) {
            errors.add("target is too far from base (max " + maxOffsetDegrees + " degrees in each direction)");
        }
        return errors;
    }

    public MissionRequest toMissionRequest() {
        return new MissionRequest(droneId, targetLat, targetLon, failDrop);
    }
}