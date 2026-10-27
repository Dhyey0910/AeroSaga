package com.aerosaga;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionWorkflowTest {

    /** Fake activities: records calls instead of simulating flight. */
    static class FakeActivities implements DroneActivities {
        final List<String> calls = new ArrayList<>();

        @Override public void takeoff(String id) { calls.add("takeoff"); }
        @Override public void navigate(String id, double lat, double lon) { calls.add("navigate"); }

        @Override
        public void dropPackage(String id, boolean forceFail) {
            calls.add("drop");
            if (forceFail) {
                throw ApplicationFailure.newNonRetryableFailure("jammed", "DropFailure");
            }
        }

        @Override public void returnToBase(String id) { calls.add("return"); }
    }

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
    void tearDown() { env.close(); }

    private MissionWorkflow newMission(String droneId) {
        return client.newWorkflowStub(MissionWorkflow.class, WorkflowOptions.newBuilder()
                .setTaskQueue(TemporalConfig.QUEUE)
                .setWorkflowId("mission-" + droneId)
                .build());
    }

    @Test
    void happyPath_runsAllStepsInOrder() {
        MissionWorkflow wf = newMission("d1");
        String result = wf.run(new MissionRequest("d1", 28.7, 77.3, false));

        assertEquals("COMPLETED", result);
        assertEquals(List.of("takeoff", "navigate", "drop", "return"), activities.calls);
    }

    @Test
    void failedDrop_triggersReturnToBaseCompensation() {
        MissionWorkflow wf = newMission("d2");
        String result = wf.run(new MissionRequest("d2", 28.7, 77.3, true));

        assertEquals("COMPENSATED", result);
        assertEquals(List.of("takeoff", "navigate", "drop", "return"), activities.calls);
    }

    @Test
    void finalState_marksEveryStepCompleted() {
        MissionWorkflow wf = newMission("d3");
        wf.run(new MissionRequest("d3", 28.7, 77.3, false));

        MissionState state = client.newWorkflowStub(MissionWorkflow.class, "mission-d3").getState();
        assertEquals("COMPLETED", state.status);
        assertEquals(4, state.steps.size());
        state.steps.forEach(s -> assertEquals("COMPLETED", s.status));
    }

    @Test
    void failedDrop_marksDropStepFailed() {
        MissionWorkflow wf = newMission("d4");
        wf.run(new MissionRequest("d4", 28.7, 77.3, true));

        MissionState state = client.newWorkflowStub(MissionWorkflow.class, "mission-d4").getState();
        assertEquals("FAILED", state.steps.get(2).status);
        assertEquals("PENDING", state.steps.get(3).status);
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
        assertEquals(12, activities.calls.size());
    }
}