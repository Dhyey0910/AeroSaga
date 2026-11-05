import React, { useCallback, useEffect, useState } from 'react';
import GlobeView from './components/GlobeView.jsx';
import FleetSummary from './components/FleetSummary.jsx';
import LaunchPanel from './components/LaunchPanel.jsx';
import DroneCard from './components/DroneCard.jsx';
import * as api from './api.js';

export default function App() {
  const [states, setStates] = useState([]);
  const [selected, setSelected] = useState(null);
  const [message, setMessage] = useState('');

  // poll the workflow state (Temporal query) once per second
  useEffect(() => {
    const tick = () => api.getStates().then(setStates).catch(() => {});
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
  }, []);

  // run an API action and show any error in the panel
  const run = useCallback(async (action) => {
    try {
      setMessage('');
      await action();
    } catch (e) {
      setMessage(e.message);
    }
  }, []);

  return (
    <div className="layout">
      <GlobeView states={states} selected={selected} />
      <aside className="panel">
        <h1>AeroSaga</h1>
        <p className="sub">Temporal drone mission control</p>

        <LaunchPanel
          onLaunch={(count, failOne) => run(() => api.launchFleet(count, failOne))}
          onAbortAll={() => run(api.abortAll)}
          message={message}
        />
        <FleetSummary states={states} />

        {states.length === 0 && <p className="empty">No drones yet. Launch a fleet.</p>}
        {states.map((s) => (
          <DroneCard
            key={s.droneId}
            state={s}
            selected={selected === s.droneId}
            onSelect={() => setSelected(selected === s.droneId ? null : s.droneId)}
            onStorm={() => run(() => api.stormDrone(s.droneId))}
            onReconnect={() => run(() => api.reconnectDrone(s.droneId))}
            onAbort={() => run(() => api.abortDrone(s.droneId))}
          />
        ))}
      </aside>
    </div>
  );
}
