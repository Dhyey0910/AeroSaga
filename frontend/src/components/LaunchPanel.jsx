import React, { useState } from 'react';

export default function LaunchPanel({ onLaunch, onAbortAll, message }) {
  const [count, setCount] = useState(5);
  const [failOne, setFailOne] = useState(false);

  const safeCount = Math.min(50, Math.max(1, Number(count) || 1));

  return (
    <div className="launch">
      <div className="controls">
        <label>
          Drones
          <input type="number" min="1" max="50" value={count} onChange={(e) => setCount(e.target.value)} />
        </label>
        <label className="chk">
          <input type="checkbox" checked={failOne} onChange={(e) => setFailOne(e.target.checked)} />
          Fail drone-1 drop
        </label>
      </div>
      <div className="controls">
        <button onClick={() => onLaunch(safeCount, failOne)}>Launch Fleet</button>
        <button className="ghost" onClick={() => onLaunch(50, false)}>Launch 50</button>
        <button className="danger" onClick={onAbortAll}>Abort All</button>
      </div>
      <p className="hint">Every 4th drone starts with low battery and triggers the return-to-base compensation.</p>
      {message && <p className="error">{message}</p>}
    </div>
  );
}
