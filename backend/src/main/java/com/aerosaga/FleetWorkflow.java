package com.aerosaga;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;

@WorkflowInterface
public interface FleetWorkflow {
    @WorkflowMethod String run(List<MissionRequest> missions);
}
