package com.aerosaga;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class MissionController {
    private static final Logger log = LoggerFactory.getLogger(MissionController.class);
    private static final double MAX_TARGET_OFFSET_DEGREES = 1.0;
    private static final int MAX_FLEET = 50;
    private static final Set<String> ACTIVE = Set.of("PENDING", "RUNNING", "OFFLINE", "COMPENSATING");

    private final WorkflowClient client;
    private final TelemetryStore telemetryStore;
    private final TelemetryGateway gateway;
    private final AeroProperties props;
    private final Set<String> drones = ConcurrentHashMap.newKeySet();
    private final Random rnd = new Random();

    public MissionController(WorkflowClient client, TelemetryStore telemetryStore,
                             TelemetryGateway gateway, AeroProperties props) {
        this.client = client;
        this.telemetryStore = telemetryStore;
        this.gateway = gateway;
        this.props = props;
    }

    /**
     * After a backend restart the in-memory drone list is empty, but the workflows are still alive in Temporal.
     * Ask Temporal which missions are running so the UI picks them up again.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverRunningMissions() {
        try {
            client.listExecutions("WorkflowType='MissionWorkflow' AND ExecutionStatus='Running'").forEach(m -> {
                String workflowId = m.getExecution().getWorkflowId();
                if (workflowId.startsWith("mission-")) {
                    drones.add(workflowId.substring("mission-".length()));
                }
            });
            log.info("Recovered {} running missions from Temporal", drones.size());
        } catch (Exception e) {
            log.warn("Could not list running missions (is Temporal up?): {}", e.getMessage());
        }
    }

    /** Launch a fleet of N drones. failOne=true makes drone-1's drop fail (shows compensation). */
    @PostMapping("/fleet")
    public ResponseEntity<Map<String, Object>> startFleet(@RequestParam(defaultValue = "5") int count,
                                                          @RequestParam(defaultValue = "false") boolean failOne) {
        if (count < 1 || count > MAX_FLEET) {
            return ResponseEntity.badRequest().body(Map.of("error", "count must be between 1 and " + MAX_FLEET));
        }
        if (!activeStates().isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                    "A fleet is still flying. Wait for it to finish or abort it first."));
        }

        drones.clear();
        telemetryStore.clear();
        List<MissionRequest> missions = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String id = "drone-" + i;
            drones.add(id);
            missions.add(new MissionRequest(id,
                    props.getBaseLat() + (rnd.nextDouble() - 0.5) * 0.3,
                    props.getBaseLon() + (rnd.nextDouble() - 0.5) * 0.3,
                    failOne && i == 1));
        }
        FleetWorkflow fleet = client.newWorkflowStub(FleetWorkflow.class, WorkflowOptions.newBuilder()
                .setTaskQueue(TemporalConfig.QUEUE).setWorkflowId("fleet-" + System.currentTimeMillis()).build());
        WorkflowClient.start(fleet::run, missions);
        return ResponseEntity.ok(Map.of("started", count));
    }

    /** Launch ONE drone to a chosen target. Returns 400 for bad input and 409 if it is already flying. */
    @PostMapping("/missions")
    public ResponseEntity<Map<String, Object>> startMission(@RequestBody MissionLaunchRequest body) {
        List<String> errors = body.validate(props.getBaseLat(), props.getBaseLon(), MAX_TARGET_OFFSET_DEGREES);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errors", errors));
        }

        MissionWorkflow workflow = client.newWorkflowStub(MissionWorkflow.class, WorkflowOptions.newBuilder()
                .setTaskQueue(TemporalConfig.QUEUE)
                .setWorkflowId("mission-" + body.droneId)
                .build());
        try {
            WorkflowClient.start(workflow::run, body.toMissionRequest());
        } catch (WorkflowExecutionAlreadyStarted e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", body.droneId + " is already on a mission"));
        }
        drones.add(body.droneId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("started", body.droneId, "workflowId", "mission-" + body.droneId));
    }

    /** Polled by the UI: current DAG state of every drone (Temporal query). */
    @GetMapping("/states")
    public List<MissionState> states() {
        return drones.parallelStream()
                .map(this::queryState)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(s -> droneNumber(s.droneId)))
                .collect(Collectors.toList());
    }

    @GetMapping("/missions/{droneId}")
    public MissionState state(@PathVariable String droneId) {
        return client.newWorkflowStub(MissionWorkflow.class, "mission-" + droneId).getState();
    }

    /** Emergency abort button (Temporal signal). */
    @PostMapping("/missions/{droneId}/abort")
    public Map<String, String> abort(@PathVariable String droneId) {
        client.newWorkflowStub(MissionWorkflow.class, "mission-" + droneId).emergencyAbort();
        return Map.of("signalled", droneId);
    }

    /** Emergency abort for every drone that is still flying. */
    @PostMapping("/fleet/abort")
    public Map<String, Object> abortAll() {
        List<MissionState> active = activeStates();
        for (MissionState s : active) {
            try {
                client.newWorkflowStub(MissionWorkflow.class, "mission-" + s.droneId).emergencyAbort();
            } catch (Exception e) {
                log.warn("Abort failed for {}: {}", s.droneId, e.getMessage());
            }
        }
        return Map.of("signalled", active.size());
    }

    /** GPS history of one drone (newest last). */
    @GetMapping("/telemetry/{droneId}")
    public ResponseEntity<List<TelemetryPoint>> telemetryHistory(@PathVariable String droneId,
                                                                 @RequestParam(defaultValue = "50") int limit) {
        List<TelemetryPoint> points = telemetryStore.history(droneId, limit);
        if (points.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(points);
    }

    /** Latest GPS ping of one drone. */
    @GetMapping("/telemetry/{droneId}/latest")
    public ResponseEntity<TelemetryPoint> latestTelemetry(@PathVariable String droneId) {
        TelemetryPoint point = telemetryStore.latest(droneId);
        return point == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(point);
    }

    /** Small info endpoint for the dashboard. */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("knownDrones", drones.size());
        out.put("webSocketClients", gateway.connectedClients());
        return out;
    }

    private List<MissionState> activeStates() {
        return states().stream().filter(s -> ACTIVE.contains(s.status)).collect(Collectors.toList());
    }

    private MissionState queryState(String droneId) {
        try {
            MissionState s = client.newWorkflowStub(MissionWorkflow.class, "mission-" + droneId).getState();
            if (s.droneId == null) {
                s.droneId = droneId;
            }
            TelemetryPoint p = telemetryStore.latest(droneId);
            s.battery = p == null ? -1 : p.battery;
            return s;
        } catch (Exception e) {
            return null; // workflow not started yet
        }
    }

    private static int droneNumber(String droneId) {
        try {
            return Integer.parseInt(droneId.substring(droneId.lastIndexOf('-') + 1));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
}
