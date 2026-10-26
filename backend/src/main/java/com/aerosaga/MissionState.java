package com.aerosaga;

import java.util.ArrayList;
import java.util.List;

public class MissionState {
    public String droneId;
    public String status = "PENDING"; // RUNNING | COMPLETED | COMPENSATING | COMPENSATED | ABORTED
    public List<Step> steps = new ArrayList<>();
}
