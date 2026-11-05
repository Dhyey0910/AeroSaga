# API Reference

Base URL: `http://localhost:8080`. Errors are JSON: `{ status, error, message, path, timestamp }`.

## Missions

| Method | Path | Description |
|---|---|---|
| POST | `/api/fleet?count=5&failOne=false` | Launch a fleet (1 to 50 drones). 409 if a fleet is still flying. |
| POST | `/api/missions` | Launch one drone. Body: `{ "droneId": "drone-7", "targetLat": 28.7, "targetLon": 77.3, "failDrop": false }`. 400 on invalid input, 409 if already flying. |
| GET | `/api/states` | Workflow state of every drone (Temporal query). |
| GET | `/api/missions/{droneId}` | Workflow state of one drone. |

## Signals

| Method | Path | Description |
|---|---|---|
| POST | `/api/missions/{droneId}/abort` | Emergency abort (compensates by returning to base). |
| POST | `/api/fleet/abort` | Emergency abort for every drone still flying. |
| POST | `/api/missions/{droneId}/storm` | Drone goes offline, workflow pauses. |
| POST | `/api/missions/{droneId}/reconnect` | Drone reconnects, workflow resumes. |

## Telemetry

| Method | Path | Description |
|---|---|---|
| GET | `/api/telemetry/{droneId}?limit=50` | GPS history (newest last). 404 if none. |
| GET | `/api/telemetry/{droneId}/latest` | Latest GPS ping. |
| GET | `/api/stats` | Known drones and connected WebSocket clients. |
| WS | `/ws/telemetry` | Live pings: `{ droneId, lat, lon, alt, battery }`. |

## Mission state

```json
{
  "droneId": "drone-1",
  "status": "RUNNING",
  "battery": 87.4,
  "steps": [
    { "name": "TAKEOFF", "status": "COMPLETED" },
    { "name": "NAVIGATE", "status": "ACTIVE" },
    { "name": "DROP_PACKAGE", "status": "PENDING" },
    { "name": "RETURN_TO_BASE", "status": "PENDING" }
  ]
}
```

Status values: `PENDING`, `RUNNING`, `OFFLINE`, `COMPLETED`, `COMPENSATING`, `COMPENSATED`, `ABORTED`.
