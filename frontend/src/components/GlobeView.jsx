import React, { useEffect, useRef } from 'react';

const Cesium = window.Cesium;
const BASE = { lon: 77.209, lat: 28.6139 };
const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/telemetry';

// marker color per mission status
export const STATUS_COLORS = {
  PENDING: '#60a5fa',
  RUNNING: '#3b82f6',
  OFFLINE: '#f59e0b',
  COMPLETED: '#22c55e',
  COMPENSATING: '#f97316',
  COMPENSATED: '#ef4444',
  ABORTED: '#ef4444',
};

/** 3D globe: one marker per drone, moved by live WebSocket telemetry, colored by workflow status. */
export default function GlobeView({ states, selected }) {
  const container = useRef(null);
  const viewerRef = useRef(null);
  const entities = useRef({});

  // create the globe and open the telemetry WebSocket once
  useEffect(() => {
    const viewer = new Cesium.Viewer(container.current, {
      baseLayer: new Cesium.ImageryLayer(
        new Cesium.OpenStreetMapImageryProvider({ url: 'https://tile.openstreetmap.org/' })
      ),
      baseLayerPicker: false,
      geocoder: false,
      homeButton: false,
      animation: false,
      timeline: false,
    });
    viewer.camera.setView({
      destination: Cesium.Cartesian3.fromDegrees(BASE.lon, BASE.lat, 80000),
    });
    viewerRef.current = viewer;

    const ws = new WebSocket(WS_URL);
    ws.onmessage = (e) => {
      const t = JSON.parse(e.data);
      const position = Cesium.Cartesian3.fromDegrees(t.lon, t.lat, t.alt);
      const existing = entities.current[t.droneId];
      if (existing) {
        existing.position = position;
        return;
      }
      entities.current[t.droneId] = viewer.entities.add({
        position,
        point: {
          pixelSize: 9,
          color: Cesium.Color.fromCssColorString(STATUS_COLORS.RUNNING),
          outlineColor: Cesium.Color.WHITE,
          outlineWidth: 2,
        },
        label: {
          text: t.droneId,
          font: '11px sans-serif',
          pixelOffset: new Cesium.Cartesian2(0, -16),
          fillColor: Cesium.Color.WHITE,
          showBackground: true,
          backgroundColor: Cesium.Color.fromCssColorString('#1d4ed8'),
          // hide labels when zoomed out so 50+ drones stay readable and fast
          distanceDisplayCondition: new Cesium.DistanceDisplayCondition(0, 120000),
        },
      });
    };

    return () => {
      ws.close();
      viewer.destroy();
      entities.current = {};
    };
  }, []);

  // color markers by workflow status, drop markers of drones that no longer exist
  useEffect(() => {
    const viewer = viewerRef.current;
    if (!viewer) return;
    const known = new Set(states.map((s) => s.droneId));
    states.forEach((s) => {
      const ent = entities.current[s.droneId];
      if (ent) {
        ent.point.color = Cesium.Color.fromCssColorString(STATUS_COLORS[s.status] || STATUS_COLORS.RUNNING);
      }
    });
    if (states.length > 0) {
      Object.keys(entities.current).forEach((id) => {
        if (!known.has(id)) {
          viewer.entities.remove(entities.current[id]);
          delete entities.current[id];
        }
      });
    }
  }, [states]);

  // follow the drone picked in the side panel
  useEffect(() => {
    const viewer = viewerRef.current;
    if (viewer) viewer.trackedEntity = selected ? entities.current[selected] : undefined;
  }, [selected]);

  return <div className="map" ref={container} />;
}
