package com.aerosaga;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class MissionWorkflowImpl implements MissionWorkflow {

    private final DroneActivities act = Workflow.newActivityStub(DroneActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setHeartbeatTimeout(Duration.ofSeconds(15))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                    .build());

    private final MissionState state = new MissionState();
    private boolean aborted = false;

    @Override
    public String run(MissionRequest r) {
        state.droneId = r.droneId;
        state.status = "RUNNING";
        for (String n : new String[]{"TAKEOFF", "NAVIGATE", "DROP_PACKAGE", "RETURN_TO_BASE"})
            state.steps.add(new Step(n, "PENDING"));

        Saga saga = new Saga(new Saga.Options.Builder().setContinueWithError(true).build());
        try {
            step(0, () -> act.takeoff(r.droneId));
            // compensation: if anything fails after takeoff, bring the drone home
            saga.addCompensation(act::returnToBase, r.droneId);
            step(1, () -> act.navigate(r.droneId, r.targetLat, r.targetLon));
            step(2, () -> act.dropPackage(r.droneId, r.failDrop));
            step(3, () -> act.returnToBase(r.droneId));
            state.status = "COMPLETED";
        } catch (Exception e) {
            state.status = "COMPENSATING";
            saga.compensate();
            state.status = aborted ? "ABORTED" : "COMPENSATED";
        }
        return state.status;
    }

    private void step(int i, Runnable body) {
        if (aborted) throw ApplicationFailure.newNonRetryableFailure("Emergency abort", "Abort");
        Step s = state.steps.get(i);
        s.status = "ACTIVE";
        try { body.run(); s.status = "COMPLETED"; }
        catch (RuntimeException e) { s.status = "FAILED"; throw e; }
    }

    @Override public MissionState getState() { return state; }
    @Override public void emergencyAbort() { aborted = true; }
}
