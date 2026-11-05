package com.aerosaga;

import io.temporal.client.WorkflowClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Simulates a storm: the drone drops offline, its mission workflow pauses, then resumes on reconnect. */
@RestController
@RequestMapping("/api/missions/{droneId}")
@CrossOrigin
public class StormController {
    private final WorkflowClient client;

    public StormController(WorkflowClient client) {
        this.client = client;
    }

    @PostMapping("/storm")
    public Map<String, String> storm(@PathVariable String droneId) {
        workflow(droneId).connectionLost();
        return Map.of("droneId", droneId, "connection", "LOST");
    }

    @PostMapping("/reconnect")
    public Map<String, String> reconnect(@PathVariable String droneId) {
        workflow(droneId).connectionRestored();
        return Map.of("droneId", droneId, "connection", "RESTORED");
    }

    private MissionWorkflow workflow(String droneId) {
        return client.newWorkflowStub(MissionWorkflow.class, "mission-" + droneId);
    }
}
