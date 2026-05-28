/**
 * Smart Bug Tracker — Application Logic
 * Handles UI rendering, navigation, and user interactions.
 * Includes Role-Based Access Control (RBAC).
 */
const App = {
  currentView: 'dashboard',
  currentIssue: null,
  users: [],
  currentUserId: 1,
  currentUserRole: 'ADMIN',

  /**
   * RBAC Permission Rules:
   * ADMIN:     Full access — edit, delete, assign, change status on any issue
   * DEVELOPER: Edit (own/assigned), change status (assigned), NO delete, NO assign
   * TESTER:    Change status (verify/reopen only), NO edit, NO delete, NO assign
   */

  async init() {
    this.users = await API.getUsers();
    this.renderUserSelector();
    // Set initial role from first user
    const initialUser = this.users.find(u => u.id === this.currentUserId);
    if (initialUser) this.currentUserRole = initialUser.role;
    API.setActingUser(this.currentUserId);
    this.bindNav();
    this.navigate('dashboard');
  },

  // --- Toast ---
  toast(msg, type = 'info') {
    const c = document.getElementById('toast-container');
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    t.textContent = msg;
    c.appendChild(t);
    setTimeout(() => t.remove(), 3000);
  },

  // --- Navigation ---
  bindNav() {
    document.querySelectorAll('.nav-btn').forEach(btn => {
      btn.addEventListener('click', () => this.navigate(btn.dataset.view));
    });
  },

  navigate(view) {
    this.currentView = view;
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.view === view));
    const main = document.getElementById('main-content');
    if (view === 'dashboard') this.renderDashboard(main);
    else if (view === 'issues') this.renderIssueList(main);
    else if (view === 'create') this.renderCreateForm(main);
  },

  renderUserSelector() {
    const sel = document.getElementById('current-user');
    sel.innerHTML = this.users.map(u => `<option value="${u.id}">${u.username} (${u.role})</option>`).join('');
    sel.addEventListener('change', e => {
      this.currentUserId = Number(e.target.value);
      const user = this.users.find(u => u.id === this.currentUserId);
      this.currentUserRole = user ? user.role : 'TESTER';
      API.setActingUser(this.currentUserId);
      // Refresh current view to apply new role permissions
      if (this.currentIssue && this.currentView !== 'dashboard' && this.currentView !== 'issues' && this.currentView !== 'create') {
        this.viewIssue(this.currentIssue.id);
      } else {
        this.navigate(this.currentView);
      }
    });
  },

  // === RBAC HELPERS ===
  getPermissions(issue) {
    const role = this.currentUserRole;
    const isAssignee = issue.assignee && issue.assignee.id === this.currentUserId;
    const isReporter = issue.reporter && issue.reporter.id === this.currentUserId;

    return {
      canEdit: role === 'ADMIN' || (role === 'DEVELOPER' && (isAssignee || isReporter)),
      canDelete: role === 'ADMIN',
      canAssign: role === 'ADMIN',
      canChangeStatus: role === 'ADMIN' || (role === 'DEVELOPER' && isAssignee) || role === 'TESTER',
    };
  },

  /** Get allowed status options based on role */
  getAllowedStatuses(issue) {
    const role = this.currentUserRole;
    const allStatuses = ['NEW','OPEN','IN_PROGRESS','RESOLVED','CLOSED'];

    if (role === 'ADMIN') {
      // Admin can attempt any transition (backend validates the transition rules)
      return allStatuses;
    }

    if (role === 'DEVELOPER') {
      // Developer can change status on assigned issues (backend validates transitions)
      return allStatuses;
    }

    if (role === 'TESTER') {
      // Tester can ONLY: RESOLVED→CLOSED/OPEN, CLOSED→OPEN
      const testerAllowed = {
        'RESOLVED': ['RESOLVED', 'CLOSED', 'OPEN'],
        'CLOSED': ['CLOSED', 'OPEN'],
      };
      return testerAllowed[issue.status] || [];
    }

    return [];
  },

  // === DASHBOARD ===
  async renderDashboard(el) {
    el.innerHTML = '<div class="section-header"><h1 class="section-title">Dashboard</h1></div><div class="stats-grid" id="stats-grid"></div><div class="charts-grid" id="charts-grid"></div><div class="issues-table-container"><h3 style="padding:20px 20px 0;font-size:16px;color:var(--text-secondary)">Recent Issues</h3><table class="issues-table"><thead><tr><th>ID</th><th>Title</th><th>Status</th><th>Priority</th><th>Assignee</th></tr></thead><tbody id="recent-issues"></tbody></table></div>';
    const [stats, issues] = await Promise.all([API.getStats(), API.getIssues()]);
    this.renderStats(stats);
    this.renderCharts(stats);
    this.renderIssueRows(document.getElementById('recent-issues'), issues.slice(0, 5));
  },

  renderStats(s) {
    const grid = document.getElementById('stats-grid');
    const cards = [
      { label: 'Total Issues', value: s.totalIssues },
      { label: 'Open', value: s.openIssues },
      { label: 'In Progress', value: s.inProgressIssues },
      { label: 'Resolved', value: s.resolvedIssues },
      { label: 'Critical', value: s.criticalIssues, cls: 'critical' },
      { label: 'High Priority', value: s.highIssues, cls: 'high' },
    ];
    grid.innerHTML = cards.map(c => `<div class="stat-card ${c.cls || ''}"><div class="stat-value">${c.value}</div><div class="stat-label">${c.label}</div></div>`).join('');
  },

  renderCharts(s) {
    const grid = document.getElementById('charts-grid');
    const total = s.totalIssues || 1;
    const statusBars = [
      { label: 'New', value: s.newIssues, cls: 'status-new' },
      { label: 'Open', value: s.openIssues, cls: 'status-open' },
      { label: 'In Progress', value: s.inProgressIssues, cls: 'status-progress' },
      { label: 'Resolved', value: s.resolvedIssues, cls: 'status-resolved' },
      { label: 'Closed', value: s.closedIssues, cls: 'status-closed' },
    ];
    const priBars = [
      { label: 'Critical', value: s.criticalIssues, cls: 'priority-critical' },
      { label: 'High', value: s.highIssues, cls: 'priority-high' },
      { label: 'Medium', value: s.mediumIssues, cls: 'priority-medium' },
      { label: 'Low', value: s.lowIssues, cls: 'priority-low' },
    ];
    const barHTML = (bars) => bars.map(b => {
      const pct = Math.max((b.value / total) * 100, b.value > 0 ? 8 : 0);
      return `<div class="bar-row"><span class="bar-label">${b.label}</span><div class="bar-track"><div class="bar-fill ${b.cls}" style="width:${pct}%">${b.value}</div></div></div>`;
    }).join('');

    grid.innerHTML = `<div class="chart-card"><h3>Issues by Status</h3><div class="bar-chart">${barHTML(statusBars)}</div></div><div class="chart-card"><h3>Issues by Priority</h3><div class="bar-chart">${barHTML(priBars)}</div></div>`;
  },

  // === ISSUE LIST ===
  async renderIssueList(el) {
    el.innerHTML = `<div class="section-header"><h1 class="section-title">All Issues</h1><button class="btn btn-primary" onclick="App.navigate('create')">＋ New Issue</button></div><div class="issues-table-container"><div class="table-toolbar"><input class="search-input" id="issue-search" placeholder="Search issues..." /><select class="filter-select" id="filter-status"><option value="">All Statuses</option><option value="NEW">New</option><option value="OPEN">Open</option><option value="IN_PROGRESS">In Progress</option><option value="RESOLVED">Resolved</option><option value="CLOSED">Closed</option></select><select class="filter-select" id="filter-priority"><option value="">All Priorities</option><option value="CRITICAL">Critical</option><option value="HIGH">High</option><option value="MEDIUM">Medium</option><option value="LOW">Low</option></select></div><table class="issues-table"><thead><tr><th>ID</th><th>Title</th><th>Status</th><th>Priority</th><th>Assignee</th><th>Created</th></tr></thead><tbody id="issue-tbody"></tbody></table></div>`;

    this.allIssues = await API.getIssues();
    this.filterAndRender();

    document.getElementById('issue-search').addEventListener('input', () => this.filterAndRender());
    document.getElementById('filter-status').addEventListener('change', () => this.filterAndRender());
    document.getElementById('filter-priority').addEventListener('change', () => this.filterAndRender());
  },

  filterAndRender() {
    const search = (document.getElementById('issue-search')?.value || '').toLowerCase();
    const status = document.getElementById('filter-status')?.value;
    const priority = document.getElementById('filter-priority')?.value;
    let filtered = this.allIssues;
    if (search) filtered = filtered.filter(i => i.title.toLowerCase().includes(search) || (i.description || '').toLowerCase().includes(search));
    if (status) filtered = filtered.filter(i => i.status === status);
    if (priority) filtered = filtered.filter(i => i.priority === priority);
    this.renderIssueRows(document.getElementById('issue-tbody'), filtered, true);
  },

  renderIssueRows(tbody, issues, showDate = false) {
    if (!tbody) return;
    if (!issues.length) {
      tbody.innerHTML = `<tr><td colspan="6"><div class="empty-state"><div class="icon">📋</div><h3>No issues found</h3><p>Try adjusting your filters</p></div></td></tr>`;
      return;
    }
    tbody.innerHTML = issues.map(i => {
      const assignee = i.assignee ? i.assignee.username : '—';
      const date = i.createdAt ? new Date(i.createdAt).toLocaleDateString() : '';
      const statusLabel = i.status.replace('_', ' ');
      return `<tr onclick="App.viewIssue(${i.id})"><td><span class="issue-id">#${i.id}</span></td><td class="issue-title-cell">${this.esc(i.title)}</td><td><span class="badge badge-${i.status.toLowerCase()}">${statusLabel}</span></td><td><span class="badge badge-${i.priority.toLowerCase()}">${i.priority}</span></td><td>${assignee}</td>${showDate ? `<td>${date}</td>` : ''}</tr>`;
    }).join('');
  },

  // === ISSUE DETAIL (with RBAC) ===
  async viewIssue(id) {
    const main = document.getElementById('main-content');
    const [issue, comments] = await Promise.all([API.getIssue(id), API.getComments(id)]);
    this.currentIssue = issue;
    this.currentView = 'detail';
    const reporter = issue.reporter ? issue.reporter.username : 'Unknown';
    const assignee = issue.assignee ? issue.assignee.username : 'Unassigned';
    const statusLabel = issue.status.replace('_', ' ');

    // --- RBAC: Determine permissions ---
    const perms = this.getPermissions(issue);
    const allowedStatuses = this.getAllowedStatuses(issue);

    // Build status dropdown (only if user can change status AND has allowed options)
    let statusHTML = '';
    if (perms.canChangeStatus && allowedStatuses.length > 0) {
      const statusOpts = allowedStatuses.map(s => `<option value="${s}" ${issue.status === s ? 'selected' : ''}>${s.replace('_',' ')}</option>`).join('');
      statusHTML = `<select class="filter-select" id="status-select" onchange="App.updateStatus(${issue.id})">${statusOpts}</select>`;
    }

    // Build assignee dropdown (ADMIN only)
    let assigneeHTML = '';
    if (perms.canAssign) {
      const assigneeOpts = this.users.map(u => `<option value="${u.id}" ${issue.assignee && issue.assignee.id === u.id ? 'selected' : ''}>${u.username}</option>`).join('');
      assigneeHTML = `<select class="filter-select" id="assignee-select" onchange="App.updateAssignee(${issue.id})"><option value="">Unassigned</option>${assigneeOpts}</select>`;
    }

    // Build action buttons
    const editBtn = perms.canEdit ? `<button class="btn btn-secondary btn-sm" onclick="App.showEditModal(${issue.id})">✏️ Edit</button>` : '';
    const deleteBtn = perms.canDelete ? `<button class="btn btn-danger btn-sm" onclick="App.deleteIssue(${issue.id})">🗑️ Delete</button>` : '';

    // Role indicator badge
    const roleBadge = `<span class="badge" style="margin-left:auto;font-size:11px;padding:4px 10px;background:rgba(139,92,246,0.15);color:#a78bfa;border:1px solid rgba(139,92,246,0.3);">${this.currentUserRole}</span>`;

    main.innerHTML = `
      <button class="back-btn" onclick="App.navigate('issues')">← Back to Issues</button>
      <div class="issue-detail active">
        <div class="issue-detail-header">
          <span class="issue-id">#${issue.id}</span>
          <h2>${this.esc(issue.title)}</h2>
        </div>
        <div class="action-bar">
          ${statusHTML}
          ${assigneeHTML}
          ${editBtn}
          ${deleteBtn}
          ${roleBadge}
        </div>
        <div class="issue-meta">
          <div class="meta-item"><label>Status</label><div class="meta-value"><span class="badge badge-${issue.status.toLowerCase()}">${statusLabel}</span></div></div>
          <div class="meta-item"><label>Priority</label><div class="meta-value"><span class="badge badge-${issue.priority.toLowerCase()}">${issue.priority}</span></div></div>
          <div class="meta-item"><label>Reporter</label><div class="meta-value">${reporter}</div></div>
          <div class="meta-item"><label>Assignee</label><div class="meta-value">${assignee}</div></div>
          <div class="meta-item"><label>Created</label><div class="meta-value">${issue.createdAt ? new Date(issue.createdAt).toLocaleString() : '—'}</div></div>
          <div class="meta-item"><label>Updated</label><div class="meta-value">${issue.updatedAt ? new Date(issue.updatedAt).toLocaleString() : '—'}</div></div>
        </div>
        <div class="issue-description">${issue.description ? this.esc(issue.description) : '<em>No description provided.</em>'}</div>
        <div class="comments-section">
          <h3>Comments (${comments.length})</h3>
          <div id="comments-list">${comments.map(c => this.renderComment(c)).join('') || '<p style="color:var(--text-muted);padding:12px 0">No comments yet.</p>'}</div>
          <div class="comment-form">
            <input class="form-control" id="comment-input" placeholder="Add a comment..." />
            <button class="btn btn-primary btn-sm" onclick="App.addComment(${issue.id})">Send</button>
          </div>
        </div>
      </div>`;
  },

  renderComment(c) {
    const date = c.createdAt ? new Date(c.createdAt).toLocaleString() : '';
    return `<div class="comment"><div class="comment-header"><span class="comment-author">${c.author ? c.author.username : 'Unknown'}</span><span class="comment-date">${date}</span></div><div class="comment-body">${this.esc(c.content)}</div></div>`;
  },

  async updateStatus(issueId) {
    const status = document.getElementById('status-select').value;
    try {
      await API.changeStatus(issueId, status);
      this.toast('Status updated', 'success');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async updateAssignee(issueId) {
    const assigneeId = Number(document.getElementById('assignee-select').value);
    if (!assigneeId) return;
    try {
      await API.assignIssue(issueId, assigneeId);
      this.toast('Issue assigned', 'success');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async addComment(issueId) {
    const input = document.getElementById('comment-input');
    const content = input.value.trim();
    if (!content) return;
    try {
      await API.addComment(issueId, this.currentUserId, content);
      input.value = '';
      this.toast('Comment added', 'success');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async deleteIssue(id) {
    if (!confirm('Delete this issue permanently?')) return;
    try {
      await API.deleteIssue(id);
      this.toast('Issue deleted', 'success');
      this.navigate('issues');
    } catch (e) { this.toast(e.message, 'error'); }
  },

  // === CREATE / EDIT ===
  renderCreateForm(el) {
    // Only ADMIN can assign during creation
    const canAssign = this.currentUserRole === 'ADMIN';
    const assigneeOptions = this.users.map(u => `<option value="${u.id}">${u.username}</option>`).join('');
    const assignField = canAssign
      ? `<div class="form-group"><label>Assign To</label><select class="form-control" id="f-assignee"><option value="">Unassigned</option>${assigneeOptions}</select></div>`
      : '';

    el.innerHTML = `
      <div class="section-header"><h1 class="section-title">Create New Issue</h1></div>
      <div class="modal" style="position:static;transform:none;max-width:100%;border:1px solid var(--border-glass)">
        <form id="create-form" onsubmit="App.submitCreate(event)">
          <div class="form-group"><label>Title *</label><input class="form-control" id="f-title" required /></div>
          <div class="form-group"><label>Description</label><textarea class="form-control" id="f-desc"></textarea></div>
          <div class="form-row">
            <div class="form-group"><label>Priority</label><select class="form-control" id="f-priority"><option value="MEDIUM">Medium</option><option value="CRITICAL">Critical</option><option value="HIGH">High</option><option value="LOW">Low</option></select></div>
            ${assignField}
          </div>
          <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:24px">
            <button type="button" class="btn btn-secondary" onclick="App.navigate('issues')">Cancel</button>
            <button type="submit" class="btn btn-primary">Create Issue</button>
          </div>
        </form>
      </div>`;
  },

  async submitCreate(e) {
    e.preventDefault();
    const data = {
      title: document.getElementById('f-title').value,
      description: document.getElementById('f-desc').value,
      priority: document.getElementById('f-priority').value,
    };
    try {
      const issue = await API.createIssue(data, this.currentUserId);
      const assigneeEl = document.getElementById('f-assignee');
      const assigneeId = assigneeEl ? assigneeEl.value : '';
      if (assigneeId) await API.assignIssue(issue.id, Number(assigneeId));
      this.toast('Issue created!', 'success');
      this.navigate('issues');
    } catch (e) { this.toast(e.message, 'error'); }
  },

  showEditModal(id) {
    const i = this.currentIssue;
    if (!i) return;
    // Double-check permission
    const perms = this.getPermissions(i);
    if (!perms.canEdit) {
      this.toast('You do not have permission to edit this issue', 'error');
      return;
    }
    const overlay = document.getElementById('modal-overlay');
    document.getElementById('modal-body').innerHTML = `
      <form onsubmit="App.submitEdit(event, ${id})">
        <div class="form-group"><label>Title</label><input class="form-control" id="e-title" value="${this.esc(i.title)}" required /></div>
        <div class="form-group"><label>Description</label><textarea class="form-control" id="e-desc">${this.esc(i.description || '')}</textarea></div>
        <div class="form-group"><label>Priority</label><select class="form-control" id="e-priority">${['CRITICAL','HIGH','MEDIUM','LOW'].map(p => `<option value="${p}" ${i.priority === p ? 'selected' : ''}>${p}</option>`).join('')}</select></div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:24px"><button type="button" class="btn btn-secondary" onclick="App.closeModal()">Cancel</button><button type="submit" class="btn btn-primary">Save Changes</button></div>
      </form>`;
    overlay.classList.add('active');
  },

  async submitEdit(e, id) {
    e.preventDefault();
    const data = {
      title: document.getElementById('e-title').value,
      description: document.getElementById('e-desc').value,
      priority: document.getElementById('e-priority').value,
    };
    try {
      await API.updateIssue(id, data);
      this.closeModal();
      this.toast('Issue updated!', 'success');
      this.viewIssue(id);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  closeModal() { document.getElementById('modal-overlay').classList.remove('active'); },

  esc(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; },
};

// Boot
document.addEventListener('DOMContentLoaded', () => App.init());
