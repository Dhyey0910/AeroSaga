package com.aerosaga;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface DroneActivities {
    void takeoff(String droneId);

    /** Flies one leg of the route: moves 1/legsRemaining of the way to the target. */
    void navigateLeg(String droneId, double lat, double lon, int legsRemaining);

    void dropPackage(String droneId, boolean forceFail);

    void returnToBase(String droneId);
}
