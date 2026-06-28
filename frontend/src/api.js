// Thin fetch wrapper around the Spring Boot backend. One function per REST endpoint;
// replaces every sGet/sSet/sList call from the original single-file app.

const BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

async function request(path, { method = "GET", body, adminPassword } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (adminPassword) headers["X-Admin-Password"] = adminPassword;

  let res;
  try {
    res = await fetch(BASE + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined
    });
  } catch (networkErr) {
    throw new Error("Could not reach the server. Check your connection and try again.");
  }

  if (res.status === 204) return null;

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    throw new Error((data && data.error) || `Request failed (${res.status})`);
  }
  return data;
}

export const api = {
  createPoule: (payload) => request("/api/poules", { method: "POST", body: payload }),
  getPoule: (code) => request(`/api/poules/${encodeURIComponent(code)}`),
  updateSettings: (code, adminPassword, payload) =>
    request(`/api/poules/${encodeURIComponent(code)}/settings`, { method: "PUT", body: payload, adminPassword }),
  deletePoule: (code, adminPassword) =>
    request(`/api/poules/${encodeURIComponent(code)}`, { method: "DELETE", adminPassword }),
  checkAdminPassword: (code, adminPassword) =>
    request(`/api/poules/${encodeURIComponent(code)}/auth`, { method: "POST", adminPassword }),

  listRiders: (code) => request(`/api/poules/${encodeURIComponent(code)}/riders`),
  addRider: (code, adminPassword, payload) =>
    request(`/api/poules/${encodeURIComponent(code)}/riders`, { method: "POST", body: payload, adminPassword }),
  updateRider: (code, riderId, adminPassword, payload) =>
    request(`/api/poules/${encodeURIComponent(code)}/riders/${encodeURIComponent(riderId)}`, { method: "PUT", body: payload, adminPassword }),
  deleteRider: (code, riderId, adminPassword) =>
    request(`/api/poules/${encodeURIComponent(code)}/riders/${encodeURIComponent(riderId)}`, { method: "DELETE", adminPassword }),
  bulkReplaceRiders: (code, adminPassword, payload) =>
    request(`/api/poules/${encodeURIComponent(code)}/riders`, { method: "PUT", body: payload, adminPassword }),

  getTeam: (code, username) =>
    request(`/api/poules/${encodeURIComponent(code)}/teams/${encodeURIComponent(username)}`),
  createTeam: (code, payload) =>
    request(`/api/poules/${encodeURIComponent(code)}/teams`, { method: "POST", body: payload }),
  swap: (code, username, payload) =>
    request(`/api/poules/${encodeURIComponent(code)}/teams/${encodeURIComponent(username)}/swap`, { method: "POST", body: payload }),
  leaderboard: (code) => request(`/api/poules/${encodeURIComponent(code)}/leaderboard`),

  saveStageResult: (code, stageNumber, adminPassword, payload) =>
    request(`/api/poules/${encodeURIComponent(code)}/stages/${stageNumber}`, { method: "PUT", body: payload, adminPassword }),
  importStageCsv: (code, stageNumber, adminPassword, csvText) =>
    request(`/api/poules/${encodeURIComponent(code)}/stages/${stageNumber}/import`, { method: "POST", body: { csvText }, adminPassword }),

  exportBackup: (code, adminPassword) =>
    request(`/api/poules/${encodeURIComponent(code)}/backup`, { adminPassword }),
  restoreBackup: (data, newAdminPassword) =>
    request(`/api/restore`, { method: "POST", body: { data, newAdminPassword } })
};
