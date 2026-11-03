package com.aerosaga;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class MissionWorkflowImpl implements MissionWorkflow {
    private static final Logger log = Workflow.getLogger(MissionWorkflowImpl.class);

    private final DroneActivities act = Workflow.newActivityStub(DroneActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(5))
                    .setHeartbeatTimeout(Duration.ofSeconds(15))
                    .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
                    .build());

    private final MissionState state = new MissionState();
    private boolean aborted = false;
    private boolean offline = false;

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
        waitForConnection();
        if (aborted) throw ApplicationFailure.newNonRetryableFailure("Emergency abort", "Abort");
        Step s = state.steps.get(i);
        s.status = "ACTIVE";
        try { body.run(); s.status = "COMPLETED"; }
        catch (RuntimeException e) { s.status = "FAILED"; throw e; }
    }

    /** Durable pause: the workflow sleeps (for minutes or hours) until the drone reconnects or an abort arrives. */
    private void waitForConnection() {
        if (!offline) return;
        log.info("Drone {} is offline, workflow paused", state.droneId);
        state.status = "OFFLINE";
        Workflow.await(() -> !offline || aborted);
        if (!aborted) {
            log.info("Drone {} reconnected, resuming mission", state.droneId);
            state.status = "RUNNING";
        }
    }

    @Override public MissionState getState() { return state; }
    @Override public void emergencyAbort() { aborted = true; }
    @Override public void connectionLost() { offline = true; }
    @Override public void connectionRestored() { offline = false; }
}