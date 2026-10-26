package com.aerosaga;

import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simulated drone: each activity "flies" and streams GPS pings through the WebSocket gateway. */
@Component
public class DroneActivitiesImpl implements DroneActivities {
    public static final double BASE_LAT = 28.6139, BASE_LON = 77.2090;

    private final TelemetryGateway gateway;
    // droneId -> [lat, lon, alt, battery]
    private final Map<String, double[]> positions = new ConcurrentHashMap<>();

    public DroneActivitiesImpl(TelemetryGateway gateway) { this.gateway = gateway; }

    @Override
    public void takeoff(String id) { fly(id, pos(id)[0], pos(id)[1], 100, 6); }

    @Override
    public void navigate(String id, double lat, double lon) { fly(id, lat, lon, 100, 30); }

    @Override
    public void dropPackage(String id, boolean forceFail) {
        sleep(1500);
        if (forceFail) {
            // non-retryable -> workflow goes straight to compensation
            throw ApplicationFailure.newNonRetryableFailure("Package release mechanism jammed", "DropFailure");
        }
    }

    @Override
    public void returnToBase(String id) {
        fly(id, BASE_LAT, BASE_LON, 100, 30);
        fly(id, BASE_LAT, BASE_LON, 0, 6);
    }

    private double[] pos(String id) {
        return positions.computeIfAbsent(id, k -> new double[]{BASE_LAT, BASE_LON, 0, 100});
    }

    private void fly(String id, double tLat, double tLon, double tAlt, int steps) {
        double[] p = pos(id);
        double dLat = (tLat - p[0]) / steps, dLon = (tLon - p[1]) / steps, dAlt = (tAlt - p[2]) / steps;
        for (int i = 1; i <= steps; i++) {
            p[0] += dLat; p[1] += dLon; p[2] += dAlt; p[3] = Math.max(0, p[3] - 0.3);
            gateway.broadcast(id, p[0], p[1], p[2], p[3]);
            Activity.getExecutionContext().heartbeat(i);
            sleep(500);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw Activity.wrap(e); }
    }
}
