package com.aerosaga;

import io.temporal.workflow.*;

@WorkflowInterface
public interface MissionWorkflow {
    @WorkflowMethod String run(MissionRequest request);
    @QueryMethod MissionState getState();     // live DAG for the UI
    @SignalMethod void emergencyAbort();      // human interrupt
}
