package com.aerosaga;

import io.temporal.workflow.Async;
import io.temporal.workflow.ChildWorkflowOptions;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.util.ArrayList;
import java.util.List;

/** Orchestrates many drones in parallel using child workflows + Promise.allOf. */
public class FleetWorkflowImpl implements FleetWorkflow {
    @Override
    public String run(List<MissionRequest> missions) {
        List<Promise<String>> results = new ArrayList<>();
        for (MissionRequest m : missions) {
            MissionWorkflow child = Workflow.newChildWorkflowStub(MissionWorkflow.class,
                    ChildWorkflowOptions.newBuilder().setWorkflowId("mission-" + m.droneId).build());
            results.add(Async.function(child::run, m));
        }
        Promise.allOf(results).get();
        return "FLEET_DONE (" + missions.size() + " drones)";
    }
}
