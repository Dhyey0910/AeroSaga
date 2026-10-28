package com.aerosaga;

import java.time.Instant;

public class TelemetryPoint {
    public String droneId;
    public double lat;
    public double lon;
    public double alt;
    public double battery;
    public long timestamp;

    public TelemetryPoint() {}

    public TelemetryPoint(String droneId, double lat, double lon, double alt, double battery) {
        this.droneId = droneId;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
        this.battery = battery;
        this.timestamp = Instant.now().toEpochMilli();
    }
}