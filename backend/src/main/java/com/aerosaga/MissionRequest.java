package com.aerosaga;

public class MissionRequest {
    public String droneId;
    public double targetLat;
    public double targetLon;
    public boolean failDrop;   // demo switch: makes the Drop Package activity fail

    public MissionRequest() {}
    public MissionRequest(String droneId, double targetLat, double targetLon, boolean failDrop) {
        this.droneId = droneId; this.targetLat = targetLat; this.targetLon = targetLon; this.failDrop = failDrop;
    }
}
