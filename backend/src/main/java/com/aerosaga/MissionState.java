package com.aerosaga;

import java.util.ArrayList;
import java.util.List;

/** Returned by the workflow query. status: PENDING | RUNNING | OFFLINE | COMPLETED | COMPENSATING | COMPENSATED | ABORTED */
public class MissionState {
    public String droneId;
    public String status = "PENDING";
    public List<Step> steps = new ArrayList<>();
    public double battery = -1; // filled in by the controller from latest telemetry, -1 = unknown
}
