package com.aerosaga;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MissionWorkflow {
    @WorkflowMethod String run(MissionRequest request);

    @QueryMethod MissionState getState();     // live DAG for the UI

    @SignalMethod void emergencyAbort();      // human interrupt
    @SignalMethod void connectionLost();      // drone went offline (storm)
    @SignalMethod void connectionRestored();  // drone reconnected
}
