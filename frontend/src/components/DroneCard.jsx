import React from 'react';

/** One drone: status, battery, the mission steps drawn as a small DAG, and the action buttons. */
export default function DroneCard({ state, selected, onSelect, onStorm, onReconnect, onAbort }) {
  const flying = state.status === 'RUNNING';
  const offline = state.status === 'OFFLINE';
  const battery = state.battery >= 0 ? Math.round(state.battery) : null;

  return (
    <div className={`card ${selected ? 'selected' : ''}`}>
      <div className="row" onClick={onSelect} title="Click to follow this drone on the globe">
        <b>{state.droneId}</b>
        <span className={`badge ${state.status}`}>{state.status}</span>
      </div>

      {battery !== null && (
        <div className="battery" title={`Battery ${battery}%`}>
          <div className={`level ${battery < 20 ? 'low' : ''}`} style={{ width: `${battery}%` }} />
          <span>{battery}%</span>
        </div>
      )}

      <div className="dag">
        {state.steps.map((st, i) => (
          <React.Fragment key={st.name}>
            <span className={`node ${st.status}`}>{st.name.replaceAll('_', ' ')}</span>
            {i < state.steps.length - 1 && <span className="arrow">→</span>}
          </React.Fragment>
        ))}
      </div>

      {(flying || offline) && (
        <div className="actions">
          {flying && <button className="storm" onClick={onStorm}>Simulate Storm</button>}
          {offline && <button onClick={onReconnect}>Reconnect</button>}
          <button className="abort" onClick={onAbort}>Emergency Abort</button>
        </div>
      )}
    </div>
  );
}
