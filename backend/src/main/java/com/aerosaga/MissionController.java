package com.aerosaga;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class MissionController {
    private final WorkflowClient client;
    private final Set<String> drones = ConcurrentHashMap.newKeySet();
    private final Random rnd = new Random();

    public MissionController(WorkflowClient client) { this.client = client; }

    /** Launch a fleet of N drones. failOne=true makes drone-1's drop fail (shows compensation). */
    @PostMapping("/fleet")
    public Map<String, Object> startFleet(@RequestParam(defaultValue = "5") int count,
                                          @RequestParam(defaultValue = "false") boolean failOne) {
        List<MissionRequest> missions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String id = "drone-" + i;
            drones.add(id);
            missions.add(new MissionRequest(id,
                    DroneActivitiesImpl.BASE_LAT + (rnd.nextDouble() - 0.5) * 0.3,
                    DroneActivitiesImpl.BASE_LON + (rnd.nextDouble() - 0.5) * 0.3,
                    failOne && i == 1));
        }
        FleetWorkflow fleet = client.newWorkflowStub(FleetWorkflow.class, WorkflowOptions.newBuilder()
                .setTaskQueue(TemporalConfig.QUEUE).setWorkflowId("fleet-" + System.currentTimeMillis()).build());
        WorkflowClient.start(fleet::run, missions);
        return Map.of("started", count);
    }

    /** Polled by the UI: current DAG state of every drone (Temporal query). */
    @GetMapping("/states")
    public List<MissionState> states() {
        List<MissionState> out = new ArrayList<>();
        for (String id : new TreeSet<>(drones)) {
            try { out.add(client.newWorkflowStub(MissionWorkflow.class, "mission-" + id).getState()); }
            catch (Exception ignored) { /* child not started yet */ }
        }
        return out;
    }

    /** Emergency abort button (Temporal signal). */
    @PostMapping("/missions/{droneId}/abort")
    public Map<String, String> abort(@PathVariable String droneId) {
        client.newWorkflowStub(MissionWorkflow.class, "mission-" + droneId).emergencyAbort();
        return Map.of("signalled", droneId);
    }
}
