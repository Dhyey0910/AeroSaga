# Demo Script

Use this order for the final review. Keep the Temporal Web UI (http://localhost:8233) open in a second tab.

1. **Start everything**: `docker compose up`, backend, frontend.
2. **Fleet launch**: click *Launch Fleet* (5 drones). Point out the globe, the live markers, and the mission steps on each card.
3. **Compensation**: launch with *Fail drone-1 drop* ticked. Show drone-1 ending as `COMPENSATED`, and the same events in the Temporal UI history.
4. **Low battery**: show drone-4 failing mid-flight and returning to base.
5. **Storm**: click *Simulate Storm* on a drone, show `OFFLINE`, wait, then click *Reconnect* and show it resuming.
6. **Resiliency**: stop the backend while drones are flying, show the workflows still `Running` in the Temporal UI, restart the backend, show the missions continuing.
7. **Emergency abort**: abort one drone, show it returning to base.
8. **Scale**: launch 50 drones and show the globe stays smooth.

## Screenshot checklist

- [ ] Globe with several drones flying
- [ ] Side panel with mission steps in different states
- [ ] `COMPENSATED` drone (failed drop)
- [ ] `OFFLINE` drone during the storm, then resumed
- [ ] Temporal Web UI: running workflows after the backend was stopped
- [ ] Temporal Web UI: event history of one mission
- [ ] 50-drone view
- [ ] `mvn test` output with all tests green
