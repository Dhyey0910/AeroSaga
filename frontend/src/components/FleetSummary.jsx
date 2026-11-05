import React from 'react';

const GROUPS = [
  { label: 'Flying', statuses: ['PENDING', 'RUNNING'], cls: 'RUNNING' },
  { label: 'Offline', statuses: ['OFFLINE'], cls: 'OFFLINE' },
  { label: 'Done', statuses: ['COMPLETED'], cls: 'COMPLETED' },
  { label: 'Recovered', statuses: ['COMPENSATING', 'COMPENSATED', 'ABORTED'], cls: 'COMPENSATED' },
];

/** Counts of drones per state, a quick overview for big fleets. */
export default function FleetSummary({ states }) {
  return (
    <div className="summary">
      {GROUPS.map((g) => (
        <div key={g.label} className={`pill ${g.cls}`}>
          <b>{states.filter((s) => g.statuses.includes(s.status)).length}</b>
          <span>{g.label}</span>
        </div>
      ))}
    </div>
  );
}
