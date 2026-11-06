package com.aerosaga;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * One drone mission: Takeoff -> Navigate (5 legs) -> Drop -> Return.
 * - Saga: any failure after takeoff triggers the "return to base" compensation.
 * - Signals: emergencyAbort, connectionLost, connectionRestored.
 * - Query: getState (used by the UI).
 */
public class MissionWorkflowImpl implements MissionWorkflow {
    private static final Logger log = Workflow.getLogger(MissionWorkflowImpl.class);
    private static final int NAVIGATE_LEGS = 5;

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
        state.status = offline ? "OFFLINE" : "RUNNING";
        for (String n : new String[]{"TAKEOFF", "NAVIGATE", "DROP_PACKAGE", "RETURN_TO_BASE"}) {
            state.steps.add(new Step(n, "PENDING"));
        }

        Saga saga = new Saga(new Saga.Options.Builder().setContinueWithError(true).build());
        try {
            step(0, () -> act.takeoff(r.droneId));
            // compensation: if anything fails after takeoff, bring the drone home
            saga.addCompensation(act::returnToBase, r.droneId);
            step(1, () -> {
                for (int leg = 0; leg < NAVIGATE_LEGS; leg++) {
                    checkpoint(); // pauses here while the drone is offline, stops here on abort
                    act.navigateLeg(r.droneId, r.targetLat, r.targetLon, NAVIGATE_LEGS - leg);
                }
            });
            step(2, () -> act.dropPackage(r.droneId, r.failDrop));
            step(3, () -> act.returnToBase(r.droneId));
            state.status = "COMPLETED";
        } catch (Exception e) {
            log.warn("Mission {} failed, running compensation: {}", r.droneId, e.getMessage());
            state.status = "COMPENSATING";
            saga.compensate();
            state.status = aborted ? "ABORTED" : "COMPENSATED";
        }
        return state.status;
    }

    private void step(int i, Runnable body) {
        checkpoint();
        Step s = state.steps.get(i);
        s.status = "ACTIVE";
        try {
            body.run();
            s.status = "COMPLETED";
        } catch (RuntimeException e) {
            s.status = "FAILED";
            throw e;
        }
    }

    /**
     * Durable pause: while the drone is offline the workflow just waits, for seconds or for hours.
     * Its state lives in Temporal, so even a backend restart does not lose it.
     */
    private void checkpoint() {
        if (offline && !aborted) {
            log.info("Drone {} is offline, workflow paused", state.droneId);
            Workflow.await(() -> !offline || aborted);
            if (!aborted) {
                log.info("Drone {} reconnected, resuming mission", state.droneId);
            }
        }
        if (aborted) {
            throw ApplicationFailure.newNonRetryableFailure("Emergency abort", "Abort");
        }
    }

    @Override
    public MissionState getState() {
        return state;
    }

    @Override
    public void emergencyAbort() {
        aborted = true;
    }

    @Override
    public void connectionLost() {
        offline = true;
        if ("RUNNING".equals(state.status)) {
            state.status = "OFFLINE";
        }
    }

    @Override
    public void connectionRestored() {
        offline = false;
        if ("OFFLINE".equals(state.status)) {
            state.status = "RUNNING";
        }
    }
}
