/**
 * Smart Bug Tracker — API Client
 * Handles all REST API communication with the backend.
 * Manages JWT token lifecycle and provides global error handling.
 */
const API = {
  BASE: '/api',
  token: null,

  /** Initialize token from localStorage on load */
  init() {
    const saved = localStorage.getItem('jwt_token');
    if (saved) this.token = saved;
  },

  /** Store JWT token in memory and localStorage */
  setToken(token) {
    this.token = token;
    if (token) {
      localStorage.setItem('jwt_token', token);
    } else {
      localStorage.removeItem('jwt_token');
    }
  },

  /** Clear all auth data — used by logout */
  clearAuth() {
    this.token = null;
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_id');
    localStorage.removeItem('user_name');
    localStorage.removeItem('user_role');
  },

  /** Check if user is currently authenticated */
  isAuthenticated() {
    return !!this.token;
  },

  /**
   * POST /api/auth/login — Authenticate and store JWT + user info.
   */
  async login(username, password) {
    const res = await this.request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });
    if (res && res.token) {
      this.setToken(res.token);
      localStorage.setItem('user_id', res.userId.toString());
      localStorage.setItem('user_name', res.username);
      localStorage.setItem('user_role', res.role);
    }
    return res;
  },

  /**
   * Logout — clear all auth data.
   */
  logout() {
    this.clearAuth();
  },

  /**
   * GET /api/users/me — Fetch current user profile from JWT.
   * Used to verify token is still valid on page load.
   */
  async getCurrentUser() {
    return this.request('/users/me');
  },

  /**
   * Core request method — adds JWT header and handles errors.
   * On 401, triggers automatic logout and redirect to login.
   */
  async request(endpoint, options = {}) {
    const url = `${this.BASE}${endpoint}`;
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }
    const config = {
      headers,
      ...options,
    };
    try {
      const res = await fetch(url, config);
      if (res.status === 204) return null;

      // Handle 401 — token expired or invalid
      if (res.status === 401) {
        this.clearAuth();
        // Trigger login page if App is available
        if (typeof App !== 'undefined' && App.showLoginPage) {
          App.showLoginPage();
          App.toast('Session expired. Please sign in again.', 'error');
        }
        throw new Error('Session expired');
      }

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
  createIssue(data) {
    return this.request('/issues', { method: 'POST', body: JSON.stringify(data) });
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
  acceptAiTriage(id, acceptPriority = true, acceptSummary = false) {
    return this.request(`/issues/${id}/accept-triage`, { method: 'PATCH', body: JSON.stringify({ acceptPriority, acceptSummary }) });
  },
  ignoreDuplicate(id) {
    return this.request(`/issues/${id}/ignore-duplicate`, { method: 'PATCH' });
  },
  ignoreAiTriage(id) {
    return this.request(`/issues/${id}/ignore-triage`, { method: 'PATCH' });
  },
  deleteIssue(id) {
    return this.request(`/issues/${id}`, { method: 'DELETE' });
  },
  markAsDuplicate(issueId, originalIssueId) {
    return this.request(`/issues/${issueId}/mark-duplicate`, {
      method: 'PATCH', body: JSON.stringify({ originalIssueId })
    });
  },
  getMatchingDevelopers(issueId) {
    return this.request(`/issues/${issueId}/matching-developers`);
  },

  // --- Comments ---
  getComments(issueId) { return this.request(`/issues/${issueId}/comments`); },
  addComment(issueId, content) {
    return this.request(`/issues/${issueId}/comments`, {
      method: 'POST', body: JSON.stringify({ content })
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

// Initialize token on script load
API.init();
