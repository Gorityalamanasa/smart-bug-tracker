/**
 * Smart Bug Tracker — API Client
 * Handles all REST API communication with the backend.
 */
const API = {
  BASE: '/api',
  actingUserId: null,

  /** Set the current acting user ID (sent as header for RBAC) */
  setActingUser(userId) {
    this.actingUserId = userId;
  },

  async request(endpoint, options = {}) {
    const url = `${this.BASE}${endpoint}`;
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (this.actingUserId) {
      headers['X-Acting-User-Id'] = this.actingUserId.toString();
    }
    const config = {
      headers,
      ...options,
    };
    try {
      const res = await fetch(url, config);
      if (res.status === 204) return null;
      if (!res.ok) {
        const err = await res.json().catch(() => ({ error: res.statusText }));
        throw new Error(err.error || `HTTP ${res.status}`);
      }
      return res.json();
    } catch (e) {
      console.error(`API Error [${options.method || 'GET'} ${url}]:`, e);
      throw e;
    }
  },

  // --- Issues ---
  getIssues(params = {}) {
    const q = new URLSearchParams(params).toString();
    return this.request(`/issues${q ? '?' + q : ''}`);
  },
  getIssue(id) { return this.request(`/issues/${id}`); },
  createIssue(data, reporterId) {
    const q = reporterId ? `?reporterId=${reporterId}` : '';
    return this.request(`/issues${q}`, { method: 'POST', body: JSON.stringify(data) });
  },
  updateIssue(id, data) {
    return this.request(`/issues/${id}`, { method: 'PUT', body: JSON.stringify(data) });
  },
  changeStatus(id, status) {
    return this.request(`/issues/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) });
  },
  assignIssue(id, assigneeId) {
    return this.request(`/issues/${id}/assign`, { method: 'PATCH', body: JSON.stringify({ assigneeId }) });
  },
  deleteIssue(id) {
    return this.request(`/issues/${id}`, { method: 'DELETE' });
  },

  // --- Comments ---
  getComments(issueId) { return this.request(`/issues/${issueId}/comments`); },
  addComment(issueId, authorId, content) {
    return this.request(`/issues/${issueId}/comments`, {
      method: 'POST', body: JSON.stringify({ authorId, content })
    });
  },

  // --- Users ---
  getUsers(role) {
    const q = role ? `?role=${role}` : '';
    return this.request(`/users${q}`);
  },
  getUser(id) { return this.request(`/users/${id}`); },

  // --- Dashboard ---
  getStats() { return this.request('/dashboard/stats'); },
  getHealth() { return this.request('/health'); },
};
