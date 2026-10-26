# AeroSaga - Temporal.io Drone Mission Control

Spring Boot (Temporal Java SDK + WebSockets) backend and React + CesiumJS frontend.

## Run (3 terminals)
1. Temporal:   `docker compose up`            (Web UI: http://localhost:8233)
2. Backend:    `cd backend && mvn spring-boot:run`     (Java 17+, port 8080)
3. Frontend:   `cd frontend && npm install && npm run dev` (http://localhost:5173)

Click **Launch Fleet** in the side panel.

## What maps to the plan
| Plan item | Where |
|---|---|
| Temporal cluster via Docker | docker-compose.yml |
| Workflow + Activity (Takeoff, Navigate, Drop, Return) | MissionWorkflowImpl, DroneActivitiesImpl |
| Promise.allOf multi-drone orchestration | FleetWorkflowImpl |
| @QueryMethod state | MissionWorkflow.getState() |
| Compensating transactions (Saga) | MissionWorkflowImpl (tick "Fail drone-1 drop") |
| @SignalMethod Emergency Abort | MissionWorkflow.emergencyAbort() + UI button |
| WebSocket telemetry | TelemetryGateway |
| 3D globe, live markers, DAG panel | frontend/src/App.jsx |

## Resiliency demo (mid-project review)
1. Launch a fleet.
2. Stop the Spring Boot process (Ctrl+C) mid-flight, then start it again.
3. Workflows resume where they stopped (see them in the Temporal UI at :8233).
