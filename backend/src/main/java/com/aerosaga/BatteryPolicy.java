package com.aerosaga;

import org.springframework.stereotype.Component;

/** Battery rules for the simulated drones. All numbers come from application.properties. */
@Component
public class BatteryPolicy {
    private final AeroProperties props;

    public BatteryPolicy(AeroProperties props) {
        this.props = props;
    }

    /** Every Nth drone (drone-4, drone-8, ...) starts low so the low-battery path can be demoed. */
    public double initialBattery(String droneId) {
        return isDemoLowBatteryDrone(droneId) ? props.getBatteryLowStart() : props.getBatteryFull();
    }

    public boolean isLow(double battery) {
        return battery < props.getBatteryLowThreshold();
    }

    public double drain(double battery) {
        return Math.max(0, battery - props.getBatteryDrainPerStep());
    }

    private boolean isDemoLowBatteryDrone(String droneId) {
        int every = props.getLowBatteryDemoEvery();
        if (every <= 0) {
            return false;
        }
        try {
            int number = Integer.parseInt(droneId.substring(droneId.lastIndexOf('-') + 1));
            return number % every == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}