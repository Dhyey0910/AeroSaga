package com.aerosaga;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionWorkflowTest {

    /** Fake activities: record calls instead of simulating a flight. */
    static class FakeActivities implements DroneActivities {
        final List<String> calls = new CopyOnWriteArrayList<>();
        volatile Runnable onTakeoff = () -> { };

        @Override
        public void takeoff(String id) {
            calls.add("takeoff");
            onTakeoff.run();
        }

        @Override
        public void navigateLeg(String id, double lat, double lon, int legsRemaining) {
            calls.add("navigate");
        }

        @Override
        public void dropPackage(String id, boolean forceFail) {
            calls.add("drop");
            if (forceFail) {
                throw ApplicationFailure.newNonRetryableFailure("jammed", "DropFailure");
            }
        }

        @Override
        public void returnToBase(String id) {
            calls.add("return");
        }
    }

    private static final List<String> HAPPY_CALLS = List.of(
            "takeoff", "navigate", "navigate", "navigate", "navigate", "navigate", "drop", "return");

    private TestWorkflowEnvironment env;
    private WorkflowClient client;
    private FakeActivities activities;

    @BeforeEach
    void setUp() {
        env = TestWorkflowEnvironment.newInstance();
        Worker worker = env.newWorker(TemporalConfig.QUEUE);
        worker.registerWorkflowImplementationTypes(MissionWorkflowImpl.class, FleetWorkflowImpl.class);
        activities = new FakeActivities();
        worker.registerActivitiesImplementations(activities);
        env.start();
        client = env.getWorkflowClient();
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    private MissionWorkflow newMission(String droneId) {
        return client.newWorkflowStub(MissionWorkflow.class, WorkflowOptions.newBuilder()
                .setTaskQueue(TemporalConfig.QUEUE)
                .setWorkflowId("mission-" + droneId)
                .build());
    }

    private MissionWorkflow existing(String droneId) {
        return client.newWorkflowStub(MissionWorkflow.class, "mission-" + droneId);
    }

    @Test
    void happyPath_runsAllStepsInOrder() {
        String result = newMission("d1").run(new MissionRequest("d1", 28.7, 77.3, false));

        assertEquals("COMPLETED", result);
        assertEquals(HAPPY_CALLS, activities.calls);
    }

    @Test
    void failedDrop_triggersReturnToBaseCompensation() {
        String result = newMission("d2").run(new MissionRequest("d2", 28.7, 77.3, true));

        assertEquals("COMPENSATED", result);
        // same calls as the happy path, but the last "return" is the compensation
        assertEquals(HAPPY_CALLS, activities.calls);
    }

    @Test
    void finalState_marksEveryStepCompleted() {
        newMission("d3").run(new MissionRequest("d3", 28.7, 77.3, false));

        MissionState state = existing("d3").getState();
        assertEquals("COMPLETED", state.status);
        assertEquals(4, state.steps.size());
        state.steps.forEach(s -> assertEquals("COMPLETED", s.status));
    }

    @Test
    void failedDrop_marksDropStepFailed() {
        newMission("d4").run(new MissionRequest("d4", 28.7, 77.3, true));

        MissionState state = existing("d4").getState();
        assertEquals("COMPENSATED", state.status);
        assertEquals("FAILED", state.steps.get(2).status);
        assertEquals("PENDING", state.steps.get(3).status);
    }

    @Test
    void emergencyAbort_stopsMissionAndReturnsToBase() {
        // the abort signal arrives while the drone is taking off
        activities.onTakeoff = () -> existing("a1").emergencyAbort();

        String result = newMission("a1").run(new MissionRequest("a1", 28.7, 77.3, false));

        assertEquals("ABORTED", result);
        assertEquals(List.of("takeoff", "return"), activities.calls);
    }

    @Test
    void offlineDrone_pausesThenResumesWhereItLeftOff() throws Exception {
        // the storm hits while the drone is taking off
        activities.onTakeoff = () -> existing("s1").connectionLost();

        WorkflowClient.start(newMission("s1")::run, new MissionRequest("s1", 28.7, 77.3, false));

        waitForStatus("s1", "OFFLINE");
        assertEquals(List.of("takeoff"), activities.calls); // paused, nothing else has run

        existing("s1").connectionRestored();
        String result = client.newUntypedWorkflowStub("mission-s1").getResult(String.class);

        assertEquals("COMPLETED", result);
        assertEquals(HAPPY_CALLS, activities.calls);
    }

    @Test
    void fleetWorkflow_runsAllDronesInParallel() {
        FleetWorkflow fleet = client.newWorkflowStub(FleetWorkflow.class, WorkflowOptions.newBuilder()
                .setTaskQueue(TemporalConfig.QUEUE)
                .setWorkflowId("fleet-test")
                .build());

        String result = fleet.run(List.of(
                new MissionRequest("f1", 28.7, 77.3, false),
                new MissionRequest("f2", 28.6, 77.1, false),
                new MissionRequest("f3", 28.5, 77.2, true)));

        assertEquals("FLEET_DONE (3 drones)", result);
        assertEquals(3 * HAPPY_CALLS.size(), activities.calls.size());
    }

    private void waitForStatus(String droneId, String expected) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            try {
                if (expected.equals(existing(droneId).getState().status)) {
                    return;
                }
            } catch (RuntimeException ignored) {
                // workflow task not processed yet
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Mission " + droneId + " never reached status " + expected);
    }
}
