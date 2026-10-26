package com.aerosaga;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface DroneActivities {
    void takeoff(String droneId);
    void navigate(String droneId, double lat, double lon);
    void dropPackage(String droneId, boolean forceFail);
    void returnToBase(String droneId);
}
