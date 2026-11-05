// Thin wrapper around the backend REST API (proxied to :8080 by vite.config.js)

async function call(url, options) {
  const res = await fetch(url, options);
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    const message =
      body.message || (body.errors && body.errors.join(', ')) || body.error || res.statusText;
    throw new Error(message);
  }
  return body;
}

export const getStates = () => call('/api/states');
export const launchFleet = (count, failOne) =>
  call(`/api/fleet?count=${count}&failOne=${failOne}`, { method: 'POST' });
export const abortDrone = (id) => call(`/api/missions/${id}/abort`, { method: 'POST' });
export const abortAll = () => call('/api/fleet/abort', { method: 'POST' });
export const stormDrone = (id) => call(`/api/missions/${id}/storm`, { method: 'POST' });
export const reconnectDrone = (id) => call(`/api/missions/${id}/reconnect`, { method: 'POST' });
