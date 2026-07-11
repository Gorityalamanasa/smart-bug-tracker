/**
 * Smart Bug Tracker — Application Logic
 * Handles UI rendering, navigation, and user interactions.
 * Uses JWT-based authentication — user role comes from the token, not a dropdown.
 *
 * Role permissions:
 *   ADMIN     — Full access: view all, assign, manage, accept/reject AI triage, analytics
 *   DEVELOPER — View assigned issues, update status, add comments
 *   TESTER    — Create issues (triggers AI triage), view own issues, verify/reopen, add comments
 */
const App = {
  currentView: 'dashboard',
  currentIssue: null,
  users: [],
  currentUserId: null,
  currentUserRole: null,
  currentUsername: null,

  // ═══════════════════════════════════════════════════════════════
  //  INITIALIZATION & AUTH
  // ═══════════════════════════════════════════════════════════════

  async init() {
    // Check if we have a stored token
    if (API.isAuthenticated()) {
      try {
        // Verify token is still valid by fetching user profile
        const user = await API.getCurrentUser();
        this.currentUserId = user.id;
        this.currentUsername = user.username;
        this.currentUserRole = user.role;

        // Update localStorage with fresh data
        localStorage.setItem('user_id', user.id.toString());
        localStorage.setItem('user_name', user.username);
        localStorage.setItem('user_role', user.role);

        // Load users list for admin features
        this.users = await API.getUsers();

        // Show the app
        this.showApp();
      } catch (e) {
        // Token is invalid/expired — show login
        console.warn('Token validation failed:', e.message);
        API.clearAuth();
        this.showLoginPage();
      }
    } else {
      this.showLoginPage();
    }
  },

  // ═══════════════════════════════════════════════════════════════
  //  LOGIN PAGE
  // ═══════════════════════════════════════════════════════════════

  showLoginPage() {
    document.getElementById('login-page').style.display = 'flex';
    document.getElementById('app-shell').style.display = 'none';
    this.bindLoginForm();
  },

  bindLoginForm() {
    const form = document.getElementById('login-form');
    if (!form) return;

    // Remove existing listeners by cloning
    const newForm = form.cloneNode(true);
    form.parentNode.replaceChild(newForm, form);

    newForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      await this.handleLogin();
    });

    // Password toggle
    const toggleBtn = document.getElementById('password-toggle');
    if (toggleBtn) {
      const newToggle = toggleBtn.cloneNode(true);
      toggleBtn.parentNode.replaceChild(newToggle, toggleBtn);
      newToggle.addEventListener('click', () => {
        const pwInput = document.getElementById('login-password');
        if (pwInput.type === 'password') {
          pwInput.type = 'text';
          newToggle.textContent = '🙈';
        } else {
          pwInput.type = 'password';
          newToggle.textContent = '👁️';
        }
      });
    }
  },

  async handleLogin() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    const errorEl = document.getElementById('login-error');
    const submitBtn = document.getElementById('login-submit-btn');

    if (!username || !password) {
      this.showLoginError('Please enter both username and password.');
      return;
    }

    // Show loading state
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="btn-spinner"></span> Signing in...';
    errorEl.style.display = 'none';

    try {
      const res = await API.login(username, password);
      this.currentUserId = res.userId;
      this.currentUsername = res.username;
      this.currentUserRole = res.role;

      // Load users list
      this.users = await API.getUsers();

      // Show the app
      this.showApp();
      this.toast(`Welcome back, ${res.username}!`, 'success');
    } catch (e) {
      this.showLoginError(e.message === 'Session expired' ? 'Invalid username or password.' : e.message);
      submitBtn.disabled = false;
      submitBtn.textContent = 'Sign In';
    }
  },

  showLoginError(msg) {
    const errorEl = document.getElementById('login-error');
    if (errorEl) {
      errorEl.textContent = msg;
      errorEl.style.display = 'block';
      // Shake animation
      errorEl.classList.remove('shake');
      void errorEl.offsetWidth; // trigger reflow
      errorEl.classList.add('shake');
    }
  },

  // ═══════════════════════════════════════════════════════════════
  //  APP SHELL — Show app after successful login
  // ═══════════════════════════════════════════════════════════════

  showApp() {
    document.getElementById('login-page').style.display = 'none';
    document.getElementById('app-shell').style.display = 'block';

    // Update user profile in navbar
    this.renderUserProfile();

    // Configure nav based on role
    this.configureNavForRole();

    // Bind nav buttons
    this.bindNav();

    // Bind logout
    this.bindLogout();

    // Navigate to dashboard
    this.navigate('dashboard');
  },

  renderUserProfile() {
    const avatarEl = document.getElementById('user-avatar');
    const nameEl = document.getElementById('user-name');
    const roleEl = document.getElementById('user-role-badge');

    if (avatarEl) avatarEl.textContent = (this.currentUsername || 'U').charAt(0).toUpperCase();
    if (nameEl) nameEl.textContent = this.currentUsername || 'User';
    if (roleEl) {
      roleEl.textContent = this.currentUserRole || 'USER';
      // Color based on role
      roleEl.className = 'user-role-badge';
      if (this.currentUserRole === 'ADMIN') roleEl.classList.add('role-admin');
      else if (this.currentUserRole === 'DEVELOPER') roleEl.classList.add('role-developer');
      else if (this.currentUserRole === 'TESTER') roleEl.classList.add('role-tester');
    }
  },

  configureNavForRole() {
    // "New Issue" button: visible for TESTER and ADMIN (testers create bugs, admins have full access)
    const createNav = document.getElementById('nav-create');
    if (createNav) {
      createNav.style.display = (this.currentUserRole === 'DEVELOPER') ? 'none' : '';
    }
  },

  bindLogout() {
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      const newBtn = logoutBtn.cloneNode(true);
      logoutBtn.parentNode.replaceChild(newBtn, logoutBtn);
      newBtn.addEventListener('click', () => this.handleLogout());
    }
  },

  handleLogout() {
    API.logout();
    this.currentUserId = null;
    this.currentUsername = null;
    this.currentUserRole = null;
    this.users = [];
    this.currentIssue = null;

    // Reset login form
    const form = document.getElementById('login-form');
    if (form) form.reset();
    const errorEl = document.getElementById('login-error');
    if (errorEl) errorEl.style.display = 'none';
    const submitBtn = document.getElementById('login-submit-btn');
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Sign In';
    }

    this.showLoginPage();
    this.toast('Signed out successfully.', 'info');
  },

  // ═══════════════════════════════════════════════════════════════
  //  TOAST NOTIFICATIONS
  // ═══════════════════════════════════════════════════════════════

  toast(msg, type = 'info') {
    const c = document.getElementById('toast-container');
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    t.textContent = msg;
    c.appendChild(t);
    setTimeout(() => t.remove(), 4000);
  },

  // ═══════════════════════════════════════════════════════════════
  //  NAVIGATION
  // ═══════════════════════════════════════════════════════════════

  bindNav() {
    document.querySelectorAll('.nav-btn').forEach(btn => {
      // Clone to remove existing listeners
      const newBtn = btn.cloneNode(true);
      btn.parentNode.replaceChild(newBtn, btn);
      newBtn.addEventListener('click', () => this.navigate(newBtn.dataset.view));
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

  // ═══════════════════════════════════════════════════════════════
  //  RBAC HELPERS
  // ═══════════════════════════════════════════════════════════════

  getPermissions(issue) {
    const role = this.currentUserRole;
    const isAssignee = issue.assignee && issue.assignee.id === this.currentUserId;
    const isReporter = issue.reporter && issue.reporter.id === this.currentUserId;

    return {
      canEdit: role === 'ADMIN' || (role === 'DEVELOPER' && (isAssignee || isReporter)),
      canDelete: role === 'ADMIN',
      canAssign: role === 'ADMIN',
      canChangeStatus: role === 'ADMIN' || (role === 'DEVELOPER' && isAssignee) || role === 'TESTER',
      canAcceptTriage: role === 'ADMIN',
      canViewDuplicateId: role === 'ADMIN' || (role === 'DEVELOPER' && isAssignee),
      canViewTriage: role === 'ADMIN',
    };
  },

  getAllowedStatuses(issue) {
    const role = this.currentUserRole;
    const allStatuses = ['NEW','OPEN','IN_PROGRESS','RESOLVED','CLOSED','REOPENED'];

    if (role === 'ADMIN') return allStatuses;
    if (role === 'DEVELOPER') return allStatuses;

    if (role === 'TESTER') {
      const testerAllowed = {
        'RESOLVED': ['RESOLVED', 'CLOSED', 'REOPENED'],
        'DUPLICATE': ['DUPLICATE', 'REOPENED'],
      };
      return testerAllowed[issue.status] || [];
    }

    return [];
  },

  // ═══════════════════════════════════════════════════════════════
  //  DASHBOARD
  // ═══════════════════════════════════════════════════════════════

  async renderDashboard(el) {
    // Show role-specific welcome
    const roleDescriptions = {
      'ADMIN': 'You have full access to all issues, assignments, and AI triage reviews.',
      'DEVELOPER': 'View and manage issues assigned to you.',
      'TESTER': 'Create bug reports and track issues you reported.'
    };

    el.innerHTML = `
      <div class="section-header">
        <div>
          <h1 class="section-title">Dashboard</h1>
          <p class="welcome-text">${roleDescriptions[this.currentUserRole] || ''}</p>
        </div>
      </div>
      <div class="stats-grid" id="stats-grid"></div>
      <div class="charts-grid" id="charts-grid"></div>
      <div class="issues-table-container">
        <h3 style="padding:20px 20px 0;font-size:16px;color:var(--text-secondary)">
          ${this.currentUserRole === 'ADMIN' ? 'Recent Issues' : this.currentUserRole === 'DEVELOPER' ? 'My Assigned Issues' : 'My Reported Issues'}
        </h3>
        <table class="issues-table">
          <thead><tr><th>ID</th><th>Title</th><th>Status</th><th>Priority</th><th>Assignee</th>${this.currentUserRole === 'ADMIN' ? '<th>AI</th>' : ''}</tr></thead>
          <tbody id="recent-issues"></tbody>
        </table>
      </div>`;

    try {
      const [stats, issues] = await Promise.all([API.getStats(), API.getIssues()]);
      this.renderStats(stats);
      this.renderCharts(stats);
      this.renderIssueRows(document.getElementById('recent-issues'), issues.slice(0, 5));
    } catch (e) {
      this.toast('Failed to load dashboard: ' + e.message, 'error');
    }
  },

  renderStats(s) {
    const grid = document.getElementById('stats-grid');
    if (!grid) return;
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
    if (!grid) return;
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

  // ═══════════════════════════════════════════════════════════════
  //  ISSUE LIST
  // ═══════════════════════════════════════════════════════════════

  async renderIssueList(el) {
    const listTitle = this.currentUserRole === 'ADMIN' ? 'All Issues'
      : this.currentUserRole === 'DEVELOPER' ? 'My Assigned Issues'
      : 'My Reported Issues';

    const newBtn = (this.currentUserRole !== 'DEVELOPER')
      ? `<button class="btn btn-primary" onclick="App.navigate('create')">＋ New Issue</button>`
      : '';

    el.innerHTML = `<div class="section-header"><h1 class="section-title">${listTitle}</h1>${newBtn}</div><div class="issues-table-container"><div class="table-toolbar"><input class="search-input" id="issue-search" placeholder="Search issues..." /><select class="filter-select" id="filter-status"><option value="">All Statuses</option><option value="NEW">New</option><option value="OPEN">Open</option><option value="IN_PROGRESS">In Progress</option><option value="RESOLVED">Resolved</option><option value="CLOSED">Closed</option><option value="REOPENED">Reopened</option><option value="DUPLICATE">Duplicate</option></select><select class="filter-select" id="filter-priority"><option value="">All Priorities</option><option value="CRITICAL">Critical</option><option value="HIGH">High</option><option value="MEDIUM">Medium</option><option value="LOW">Low</option></select></div><table class="issues-table"><thead><tr><th>ID</th><th>Title</th><th>Status</th><th>Priority</th><th>Assignee</th>${this.currentUserRole === 'ADMIN' ? '<th>AI</th>' : ''}<th>Created</th></tr></thead><tbody id="issue-tbody"></tbody></table></div>`;

    try {
      this.allIssues = await API.getIssues();
      this.filterAndRender();

      document.getElementById('issue-search').addEventListener('input', () => this.filterAndRender());
      document.getElementById('filter-status').addEventListener('change', () => this.filterAndRender());
      document.getElementById('filter-priority').addEventListener('change', () => this.filterAndRender());
    } catch (e) {
      this.toast('Failed to load issues: ' + e.message, 'error');
    }
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
    const colCount = showDate ? (this.currentUserRole === 'ADMIN' ? 7 : 6) : (this.currentUserRole === 'ADMIN' ? 6 : 5);
    if (!issues.length) {
      tbody.innerHTML = `<tr><td colspan="${colCount}"><div class="empty-state"><div class="icon">📋</div><h3>No issues found</h3><p>Try adjusting your filters</p></div></td></tr>`;
      return;
    }
    tbody.innerHTML = issues.map(i => {
      const assignee = i.assignee ? i.assignee.username : '—';
      const date = i.createdAt ? new Date(i.createdAt).toLocaleDateString() : '';
      const statusLabel = i.status.replace('_', ' ');

      let aiCol = '';
      if (this.currentUserRole === 'ADMIN') {
        if (i.aiDuplicateSimilarity) {
          aiCol = `<span title="Possible Duplicate (${i.aiDuplicateSimilarity})" style="cursor:help;font-size:16px;">⚠️</span>`;
        } else if (i.triageStatus === 'READY_FOR_TRIAGE') {
          aiCol = `<span title="AI Analysis Ready" style="cursor:help;font-size:16px;">🤖</span>`;
        } else {
          aiCol = '—';
        }
      }

      return `<tr onclick="App.viewIssue(${i.id})"><td><span class="issue-id">#${i.id}</span></td><td class="issue-title-cell">${this.esc(i.title)}</td><td><span class="badge badge-${i.status.toLowerCase()}">${statusLabel}</span></td><td><span class="badge badge-${i.priority.toLowerCase()}">${i.priority}</span></td><td>${assignee}</td>${this.currentUserRole === 'ADMIN' ? `<td style="text-align:center">${aiCol}</td>` : ''}${showDate ? `<td>${date}</td>` : ''}</tr>`;
    }).join('');
  },

  // ═══════════════════════════════════════════════════════════════
  //  ISSUE DETAIL (with RBAC + AI Triage Panel)
  // ═══════════════════════════════════════════════════════════════

  async viewIssue(id) {
    const main = document.getElementById('main-content');
    try {
      const [issue, comments] = await Promise.all([API.getIssue(id), API.getComments(id)]);
      this.currentIssue = issue;
      this.currentView = 'detail';
      const reporter = issue.reporter ? issue.reporter.username : 'Unknown';
      const assignee = issue.assignee ? issue.assignee.username : 'Unassigned';
      const statusLabel = issue.status.replace('_', ' ');

      const perms = this.getPermissions(issue);
      const allowedStatuses = this.getAllowedStatuses(issue);

      // Build status dropdown
      let statusHTML = '';
      if (perms.canChangeStatus && allowedStatuses.length > 0) {
        const statusOpts = allowedStatuses.map(s => `<option value="${s}" ${issue.status === s ? 'selected' : ''}>${s.replace('_',' ')}</option>`).join('');
        statusHTML = `<select class="filter-select" id="status-select" onchange="App.updateStatus(${issue.id})">${statusOpts}</select>`;
      }

      // Build assignee dropdown (ADMIN only)
      let assigneeHTML = '';
      if (perms.canAssign && issue.status !== 'DUPLICATE') {
        const devUsers = this.users.filter(u => u.role === 'DEVELOPER' || u.role === 'ADMIN');
        const assigneeOpts = devUsers.map(u => `<option value="${u.id}" ${issue.assignee && issue.assignee.id === u.id ? 'selected' : ''}>${u.username}${u.expertise ? ' (' + u.expertise + ')' : ''}</option>`).join('');
        assigneeHTML = `<select class="filter-select" id="assignee-select" onchange="App.updateAssignee(${issue.id})"><option value="">Unassigned</option>${assigneeOpts}</select>`;
      }

      // Build action buttons
      const editBtn = perms.canEdit ? `<button class="btn btn-secondary btn-sm" onclick="App.showEditModal(${issue.id})">✏️ Edit</button>` : '';
      const deleteBtn = perms.canDelete ? `<button class="btn btn-danger btn-sm" onclick="App.deleteIssue(${issue.id})">🗑️ Delete</button>` : '';
      const roleBadge = `<span class="badge" style="margin-left:auto;font-size:11px;padding:4px 10px;background:rgba(139,92,246,0.15);color:#a78bfa;border:1px solid rgba(139,92,246,0.3);">${this.currentUserRole}</span>`;

      // --- Build duplicate banner ---
      let duplicateBannerHTML = '';
      if (issue.status === 'DUPLICATE' && issue.duplicateOfIssueId) {
        duplicateBannerHTML = `
          <div class="duplicate-banner" style="background:rgba(239,68,68,0.08);border-left:4px solid #ef4444;padding:16px;border-radius:var(--radius-sm);margin-bottom:24px;">
            <div style="font-weight:700;color:#ef4444;font-size:14px;margin-bottom:4px;">Duplicate Issue</div>
            <div style="font-size:13px;color:var(--text-primary);line-height:1.5;">
              This issue has been marked as a duplicate of <a href="#" onclick="event.preventDefault();App.viewIssue(${issue.duplicateOfIssueId})" style="color:var(--accent-cyan);text-decoration:none;font-weight:600;">Issue #${issue.duplicateOfIssueId}</a>.
              <br>Development is already in progress on the original issue.
            </div>
          </div>`;
      }

      // --- Build AI Triage Panel (ADMIN only) ---
      let aiPanelHTML = '';
      const hasTriage = issue.aiSummary || issue.aiSuggestedPriority || issue.aiSuggestedExpertise || issue.aiDuplicateSimilarity || issue.aiReason;

      if (perms.canViewTriage && hasTriage) {
        // Suggested Priority
        let sugPriorityHTML = '';
        if (issue.aiSuggestedPriority) {
          const priorityMatch = issue.priority === issue.aiSuggestedPriority;
          sugPriorityHTML = `
            <div class="ai-field">
              <label>📊 Suggested Priority</label>
              <div style="display:flex;align-items:center;gap:8px;margin-top:6px;">
                <span class="badge badge-${issue.aiSuggestedPriority.toLowerCase()}">${issue.aiSuggestedPriority}</span>
                ${priorityMatch
                  ? '<span style="color:#22c55e;font-size:12px;font-weight:600;">✓ Applied</span>'
                  : `<button class="btn btn-sm" style="padding:3px 12px;font-size:11px;height:auto;background:rgba(139,92,246,0.2);color:#c084fc;border:1px solid rgba(139,92,246,0.4);" onclick="App.applyAiPriority(${issue.id})">Accept</button>`
                }
              </div>
            </div>`;
        }

        // Suggested Expertise
        let sugExpertiseHTML = '';
        if (issue.aiSuggestedExpertise) {
          sugExpertiseHTML = `
            <div class="ai-field">
              <label>🎯 Suggested Expertise</label>
              <div style="margin-top:6px;">
                <span class="badge" style="background:rgba(6,182,212,0.15);color:var(--accent-cyan);border:1px solid rgba(6,182,212,0.3);">${issue.aiSuggestedExpertise}</span>
                ${issue.status !== 'DUPLICATE' ? `<button class="btn btn-sm" style="padding:3px 12px;font-size:11px;height:auto;margin-left:8px;background:rgba(6,182,212,0.15);color:var(--accent-cyan);border:1px solid rgba(6,182,212,0.3);" onclick="App.showMatchingDevs(${issue.id})">View Matching Devs</button>` : ''}
              </div>
            </div>`;
        }

        // Duplicate Detection
        let duplicateHTML = '';
        if (issue.aiDuplicateSimilarity && issue.status !== 'DUPLICATE') {
          const simValue = parseInt(issue.aiDuplicateSimilarity) || 0;
          const simColor = simValue >= 80 ? '#ef4444' : simValue >= 60 ? '#f59e0b' : '#22c55e';
          duplicateHTML = `
            <div class="ai-field" style="grid-column: 1 / -1;">
              <label>🔍 Duplicate Detection</label>
              <div style="margin-top:8px;padding:12px;background:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.2);border-radius:var(--radius-sm);display:flex;align-items:center;gap:12px;flex-wrap:wrap;justify-content:space-between;">
                <div style="display:flex;align-items:center;gap:12px;">
                  <span style="font-size:20px;">⚠️</span>
                  <div>
                    <div style="font-weight:700;color:#f87171;font-size:14px;">Possible Duplicate: Issue #${issue.aiDuplicateBugId}</div>
                    <div style="display:flex;align-items:center;gap:10px;margin-top:4px;">
                      <span style="font-size:13px;color:var(--text-secondary);">Similarity:</span>
                      <span style="font-weight:800;color:${simColor};font-size:15px;">${issue.aiDuplicateSimilarity}</span>
                      <a href="#" onclick="event.preventDefault();App.viewIssue(${issue.aiDuplicateBugId})" style="color:var(--accent-cyan);text-decoration:none;font-weight:600;font-size:13px;border:1px solid rgba(6,182,212,0.3);padding:2px 10px;border-radius:var(--radius-sm);">→ View Issue #${issue.aiDuplicateBugId}</a>
                    </div>
                  </div>
                </div>
                <div style="display:flex;gap:8px;margin-top:8px;">
                  <button class="btn btn-sm" style="padding:6px 12px;font-size:12px;background:rgba(255,255,255,0.05);color:var(--text-primary);border:1px solid rgba(255,255,255,0.15);" onclick="App.ignoreDuplicate(${issue.id})">Ignore AI Suggestion</button>
                  <button class="btn btn-danger btn-sm" style="padding:6px 12px;font-size:12px;" onclick="App.markDuplicate(${issue.id}, ${issue.aiDuplicateBugId})">Mark as Duplicate</button>
                </div>
              </div>
            </div>`;
        }

        // AI Summary
        let summaryHTML = '';
        if (issue.aiSummary) {
          const summaryMatch = issue.title === issue.aiSummary;
          summaryHTML = `
            <div class="ai-field" style="grid-column: 1 / -1;">
              <label>📝 AI Optimized Summary</label>
              <div style="display:flex;align-items:center;justify-content:space-between;gap:16px;font-size:14px;color:var(--text-primary);margin-top:6px;font-style:italic;background:rgba(0,0,0,0.2);padding:12px 16px;border-radius:var(--radius-sm);border-left:3px solid rgba(139,92,246,0.5);">
                <span style="flex-grow: 1;">"${this.esc(issue.aiSummary)}"</span>
                ${summaryMatch
                  ? '<span style="color:#22c55e;font-size:12px;font-weight:600;font-style:normal;">✓ Applied</span>'
                  : `<button class="btn btn-sm" style="padding:3px 12px;font-size:11px;height:auto;background:rgba(139,92,246,0.2);color:#c084fc;border:1px solid rgba(139,92,246,0.4);" onclick="App.applyAiSummary(${issue.id})">Accept</button>`
                }
              </div>
            </div>`;
        }

        // AI Reason
        let reasonHTML = '';
        if (issue.aiReason) {
          reasonHTML = `
            <div class="ai-field" style="grid-column: 1 / -1;">
              <label>💡 AI Analysis & Reasoning</label>
              <div style="font-size:13px;color:var(--text-secondary);margin-top:6px;line-height:1.6;">
                ${this.esc(issue.aiReason)}
              </div>
            </div>`;
        }

        aiPanelHTML = `
          <div style="margin-bottom:24px;padding:24px;background:linear-gradient(135deg, rgba(139,92,246,0.08) 0%, rgba(6,182,212,0.05) 100%);border:1px solid rgba(139,92,246,0.2);border-radius:var(--radius-md);position:relative;overflow:hidden;">
            <div style="position:absolute;top:-15px;right:-15px;font-size:90px;opacity:0.04;pointer-events:none;font-weight:900;">AI</div>
            <div style="display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:20px;flex-wrap:wrap;">
              <div style="display:flex;align-items:center;gap:10px;">
                <span style="font-size:24px;">🤖</span>
                <h3 style="color:var(--accent-purple);font-size:17px;margin:0;border:none;padding:0;">AI Analysis</h3>
              </div>
              <div style="display:flex;gap:8px;flex-wrap:wrap;">
                <button class="btn btn-sm" style="padding:6px 12px;font-size:12px;background:rgba(34,197,94,0.15);color:#22c55e;border:1px solid rgba(34,197,94,0.3);" onclick="App.acceptAllAi(${issue.id})">Accept Suggestions</button>
                <button class="btn btn-sm" style="padding:6px 12px;font-size:12px;background:rgba(255,255,255,0.05);color:var(--text-primary);border:1px solid rgba(255,255,255,0.15);" onclick="App.ignoreAllAi(${issue.id})">Ignore Suggestions</button>
              </div>
            </div>
            <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:18px;">
              ${summaryHTML}
              ${sugPriorityHTML}
              ${sugExpertiseHTML}
              ${duplicateHTML}
              ${reasonHTML}
            </div>
          </div>`;
      }

      // Matching developers panel placeholder
      const matchingDevsContainer = perms.canAssign && issue.status !== 'DUPLICATE' ? `<div id="matching-devs-panel"></div>` : '';

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
          
          ${duplicateBannerHTML}
          ${aiPanelHTML}
          ${matchingDevsContainer}

          <div class="comments-section">
            <h3>Comments (${comments.length})</h3>
            <div id="comments-list">${comments.map(c => this.renderComment(c)).join('') || '<p style="color:var(--text-muted);padding:12px 0">No comments yet.</p>'}</div>
            <div class="comment-form">
              <input class="form-control" id="comment-input" placeholder="Add a comment..." />
              <button class="btn btn-primary btn-sm" onclick="App.addComment(${issue.id})">Send</button>
            </div>
          </div>
        </div>`;
    } catch (e) {
      this.toast(e.message, 'error');
      if (e.message !== 'Session expired') this.navigate('issues');
    }
  },

  async applyAiPriority(issueId) {
    try {
      await API.acceptAiTriage(issueId, true, false);
      this.toast('AI priority recommendation accepted!', 'success');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async applyAiSummary(issueId) {
    try {
      await API.acceptAiTriage(issueId, false, true);
      this.toast('AI summary suggestion accepted!', 'success');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async acceptAllAi(issueId) {
    try {
      await API.acceptAiTriage(issueId, true, true);
      this.toast('AI suggestions accepted!', 'success');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async ignoreAllAi(issueId) {
    if (!confirm('Dismiss all AI suggestions for this issue?')) return;
    try {
      await API.ignoreAiTriage(issueId);
      this.toast('AI suggestions dismissed.', 'info');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async ignoreDuplicate(issueId) {
    try {
      await API.ignoreDuplicate(issueId);
      this.toast('Duplicate suggestion ignored.', 'info');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async markDuplicate(issueId, originalIssueId) {
    if (!confirm(`Mark this issue as a duplicate of Issue #${originalIssueId}?`)) return;
    try {
      await API.markAsDuplicate(issueId, originalIssueId);
      this.toast('Issue marked as duplicate!', 'success');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async showMatchingDevs(issueId) {
    try {
      const data = await API.getMatchingDevelopers(issueId);
      const panel = document.getElementById('matching-devs-panel');
      if (!panel) return;

      const devsList = (data.matchingDevelopers || []).map(d => `
        <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 14px;background:rgba(0,0,0,0.15);border-radius:var(--radius-sm);margin-bottom:6px;">
          <div>
            <span style="font-weight:600;color:var(--text-primary);">${this.esc(d.username)}</span>
            ${d.expertise ? `<span class="badge" style="margin-left:8px;font-size:10px;padding:2px 8px;background:rgba(6,182,212,0.15);color:var(--accent-cyan);border:1px solid rgba(6,182,212,0.3);">${d.expertise}</span>` : ''}
          </div>
          <button class="btn btn-sm" style="padding:3px 12px;font-size:11px;height:auto;background:rgba(34,197,94,0.15);color:#22c55e;border:1px solid rgba(34,197,94,0.3);" onclick="App.assignDev(${issueId},${d.id},'${this.esc(d.username)}')">Assign</button>
        </div>
      `).join('');

      panel.innerHTML = `
        <div style="margin-bottom:24px;padding:20px;background:rgba(6,182,212,0.06);border:1px solid rgba(6,182,212,0.2);border-radius:var(--radius-md);">
          <h3 style="color:var(--accent-cyan);font-size:15px;margin-bottom:14px;border:none;padding:0;display:flex;align-items:center;gap:8px;">
            👥 Matching Developers
            ${data.suggestedExpertise ? `<span class="badge" style="font-size:10px;padding:2px 8px;background:rgba(6,182,212,0.15);color:var(--accent-cyan);border:1px solid rgba(6,182,212,0.3);">Expertise: ${data.suggestedExpertise}</span>` : ''}
          </h3>
          ${devsList || '<p style="color:var(--text-muted);font-size:13px;">No matching developers found.</p>'}
        </div>`;
    } catch (e) { this.toast(e.message, 'error'); }
  },

  async assignDev(issueId, devId, devName) {
    try {
      await API.assignIssue(issueId, devId);
      this.toast(`Assigned to ${devName}!`, 'success');
      this.viewIssue(issueId);
    } catch (e) { this.toast(e.message, 'error'); }
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
      await API.addComment(issueId, content);
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

  // ═══════════════════════════════════════════════════════════════
  //  CREATE FORM (with AI Triage)
  // ═══════════════════════════════════════════════════════════════

  renderCreateForm(el) {
    const isTester = this.currentUserRole === 'TESTER';

    el.innerHTML = `
      <div class="section-header"><h1 class="section-title">Create New Issue</h1></div>
      <div class="modal" style="position:static;transform:none;max-width:100%;border:1px solid var(--border-glass)">
        <form id="create-form" onsubmit="App.submitCreate(event)">
          <div class="form-group">
            <label>Title *</label>
            <input class="form-control" id="f-title" required placeholder="Brief summary of the bug" />
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea class="form-control" id="f-desc" rows="5" placeholder="Describe what happened and how to reproduce it"></textarea>
          </div>
          <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:24px">
            <button type="button" class="btn btn-secondary" onclick="App.navigate('issues')">Cancel</button>
            <button type="submit" class="btn btn-primary" id="create-submit-btn">${isTester ? 'Submit Issue' : 'Create Issue'}</button>
          </div>
        </form>
      </div>`;
  },

  async submitCreate(e) {
    e.preventDefault();
    const submitBtn = document.getElementById('create-submit-btn');
    const originalLabel = submitBtn.textContent;
    submitBtn.disabled = true;
    submitBtn.textContent = 'Submitting...';

    const data = {
      title: document.getElementById('f-title').value,
      description: document.getElementById('f-desc').value
    };

    try {
      const issue = await API.createIssue(data);
      this.toast('Issue created successfully.', 'success');

      if (this.currentUserRole === 'ADMIN') {
        this.viewIssue(issue.id);
      } else {
        this.navigate('issues');
      }

    } catch (err) {
      submitBtn.disabled = false;
      submitBtn.textContent = originalLabel;
      this.toast(err.message, 'error');
    }
  },

  // ═══════════════════════════════════════════════════════════════
  //  EDIT MODAL
  // ═══════════════════════════════════════════════════════════════

  showEditModal(id) {
    const i = this.currentIssue;
    if (!i) return;
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
