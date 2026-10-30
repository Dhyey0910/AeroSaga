package com.aerosaga;

import org.springframework.stereotype.Component;

/** Battery rules for the simulated drones. */
@Component
public class BatteryPolicy {
    public static final double FULL = 100;
    public static final double LOW_THRESHOLD = 20;
    public static final double LOW_START = 25;
    public static final double DRAIN_PER_STEP = 0.3;

    /** Every 4th drone (drone-4, drone-8, ...) starts low so the low-battery path can be demoed. */
    public double initialBattery(String droneId) {
        return isDemoLowBatteryDrone(droneId) ? LOW_START : FULL;
    }

    public boolean isLow(double battery) {
        return battery < LOW_THRESHOLD;
    }

    public double drain(double battery) {
        return Math.max(0, battery - DRAIN_PER_STEP);
    }

    private boolean isDemoLowBatteryDrone(String droneId) {
        try {
            int number = Integer.parseInt(droneId.substring(droneId.lastIndexOf('-') + 1));
            return number % 4 == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}