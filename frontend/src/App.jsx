import React, { useEffect, useRef, useState } from 'react';

const Cesium = window.Cesium;
const BASE = { lon: 77.209, lat: 28.6139 };

export default function App() {
  const mapRef = useRef(null);
  const viewerRef = useRef(null);
  const entities = useRef({});
  const [states, setStates] = useState([]);
  const [count, setCount] = useState(5);
  const [failOne, setFailOne] = useState(false);

  // 3D globe + live telemetry over WebSocket
  useEffect(() => {
    const viewer = new Cesium.Viewer(mapRef.current, {
      baseLayer: new Cesium.ImageryLayer(
        new Cesium.OpenStreetMapImageryProvider({ url: 'https://tile.openstreetmap.org/' })
      ),
      baseLayerPicker: false, geocoder: false, homeButton: false, animation: false, timeline: false,
    });
    viewer.camera.flyTo({ destination: Cesium.Cartesian3.fromDegrees(BASE.lon, BASE.lat, 60000) });
    viewerRef.current = viewer;

    const ws = new WebSocket('ws://localhost:8080/ws/telemetry');
    ws.onmessage = (e) => {
      const t = JSON.parse(e.data);
      const pos = Cesium.Cartesian3.fromDegrees(t.lon, t.lat, t.alt);
      let ent = entities.current[t.droneId];
      if (!ent) {
        ent = viewer.entities.add({
          position: pos,
          point: { pixelSize: 10, color: Cesium.Color.DODGERBLUE, outlineColor: Cesium.Color.WHITE, outlineWidth: 2 },
          label: { text: t.droneId, font: '12px sans-serif', pixelOffset: new Cesium.Cartesian2(0, -18),
                   fillColor: Cesium.Color.WHITE, showBackground: true, backgroundColor: Cesium.Color.fromCssColorString('#1d4ed8') },
        });
        entities.current[t.droneId] = ent;
      } else {
        ent.position = pos;
      }
    };
    return () => { ws.close(); viewer.destroy(); };
  }, []);

  // poll workflow state (Temporal query) once per second
  useEffect(() => {
    const id = setInterval(() => {
      fetch('/api/states').then((r) => r.json()).then(setStates).catch(() => {});
    }, 1000);
    return () => clearInterval(id);
  }, []);

  const launch = () => fetch(`/api/fleet?count=${count}&failOne=${failOne}`, { method: 'POST' });
  const abort = (id) => fetch(`/api/missions/${id}/abort`, { method: 'POST' });

  return (
    <div className="layout">
      <div className="map" ref={mapRef} />
      <aside className="panel">
        <h1>AeroSaga</h1>
        <p className="sub">Temporal drone mission control</p>

        <div className="controls">
          <label>Drones <input type="number" min="1" max="50" value={count} onChange={(e) => setCount(e.target.value)} /></label>
          <label className="chk"><input type="checkbox" checked={failOne} onChange={(e) => setFailOne(e.target.checked)} /> Fail drone-1 drop</label>
          <button onClick={launch}>Launch Fleet</button>
        </div>

        {states.map((s) => (
          <div className="card" key={s.droneId}>
            <div className="row">
              <b>{s.droneId}</b>
              <span className={`badge ${s.status}`}>{s.status}</span>
            </div>
            <div className="dag">
              {s.steps.map((st, i) => (
                <React.Fragment key={st.name}>
                  <span className={`node ${st.status}`}>{st.name.replace('_', ' ')}</span>
                  {i < s.steps.length - 1 && <span className="arrow">→</span>}
                </React.Fragment>
              ))}
            </div>
            {s.status === 'RUNNING' && <button className="abort" onClick={() => abort(s.droneId)}>Emergency Abort</button>}
          </div>
        ))}
      </aside>
    </div>
  );
}
