package com.aerosaga;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Settings read from application.properties (prefix "aero"). */
@Component
@ConfigurationProperties(prefix = "aero")
public class AeroProperties {
    private double baseLat = 28.6139;
    private double baseLon = 77.2090;
    private double batteryFull = 100;
    private double batteryLowThreshold = 20;
    private double batteryLowStart = 25;
    private double batteryDrainPerStep = 0.3;
    private int lowBatteryDemoEvery = 4;

    public double getBaseLat() { return baseLat; }
    public void setBaseLat(double baseLat) { this.baseLat = baseLat; }

    public double getBaseLon() { return baseLon; }
    public void setBaseLon(double baseLon) { this.baseLon = baseLon; }

    public double getBatteryFull() { return batteryFull; }
    public void setBatteryFull(double batteryFull) { this.batteryFull = batteryFull; }

    public double getBatteryLowThreshold() { return batteryLowThreshold; }
    public void setBatteryLowThreshold(double batteryLowThreshold) { this.batteryLowThreshold = batteryLowThreshold; }

    public double getBatteryLowStart() { return batteryLowStart; }
    public void setBatteryLowStart(double batteryLowStart) { this.batteryLowStart = batteryLowStart; }

    public double getBatteryDrainPerStep() { return batteryDrainPerStep; }
    public void setBatteryDrainPerStep(double batteryDrainPerStep) { this.batteryDrainPerStep = batteryDrainPerStep; }

    public int getLowBatteryDemoEvery() { return lowBatteryDemoEvery; }
    public void setLowBatteryDemoEvery(int lowBatteryDemoEvery) { this.lowBatteryDemoEvery = lowBatteryDemoEvery; }
}