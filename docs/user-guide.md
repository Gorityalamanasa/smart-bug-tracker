# 🐛 Smart Bug Tracker — Complete User Guide

## Live Demo Recording

Here's a full walkthrough of every feature in action:

![Complete app walkthrough](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/app_demo.webp)

---

## What This App Is

This is a **Bug Tracking System** — like Jira, GitHub Issues, or Bugzilla. Software teams use it to:

- **Report** bugs found during testing
- **Assign** bugs to developers to fix
- **Track** the status of each bug from discovery to resolution
- **Discuss** bugs with comments
- **Monitor** overall project health via the dashboard

### Who Uses It

| Role | What They Do |
|------|-------------|
| **TESTER** (alice_qa, bob_qa) | Finds bugs, reports them, verifies fixes |
| **DEVELOPER** (john_dev, jane_dev) | Gets assigned bugs, fixes them, marks as resolved |
| **ADMIN** (admin) | Oversees everything, assigns work, manages priorities |

---

## Step-by-Step Guide

### Step 1: Dashboard — See the Big Picture

When you open **http://localhost:8080**, you land on the Dashboard.

![Dashboard overview](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/step1_dashboard_1777620989757.png)

**What you see:**
- **Stat cards** — Total issues (8), how many are Open (2), In Progress (2), Resolved (1)
- **Critical/High** counters — Urgent bugs that need attention
- **Status chart** — Visual breakdown of all issues by lifecycle stage
- **Priority chart** — How many bugs are Critical vs Low priority
- **Recent Issues** — Quick preview of latest bugs

> 💡 **Use case**: A project manager opens the dashboard every morning to see: "Are there any critical bugs? How many issues are stuck In Progress?"

---

### Step 2: Issues List — Browse All Bugs

Click **"Issues"** in the navigation bar.

![Issues list with all bugs](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/step2_issues_list_1777621022937.png)

**What you see:**
- Every bug in a table with **ID**, **Title**, **Status**, **Priority**, **Assignee**, **Date**
- Color-coded badges — purple=New, blue=Open, orange=In Progress, green=Resolved, gray=Closed
- Priority badges — red=Critical, orange=High, yellow=Medium, green=Low

> 💡 **Use case**: A developer opens this page to see "What bugs are assigned to me?"

---

### Step 3: Filter & Search — Find Specific Bugs

Use the **dropdown filters** at the top to narrow results.

![Filtered to show only NEW issues](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/step3_filter_new_1777621077167.png)

**What you can do:**
- **Search box** — Type any keyword to search titles/descriptions
- **Status filter** — Show only NEW, OPEN, IN_PROGRESS, RESOLVED, or CLOSED
- **Priority filter** — Show only CRITICAL, HIGH, MEDIUM, or LOW

> 💡 **Use case**: A tester filters by "RESOLVED" to find bugs that need verification before closing.

---

### Step 4: Issue Detail — See Everything About a Bug

Click any issue row to open its **detail page**.

![Issue #1 detail view](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/step4_issue_detail_1777621143839.png)

**What you see:**
- **Title & ID** — "#1 Login page crashes on invalid input"
- **Status/Assignee dropdowns** — Change these directly!
- **Edit/Delete buttons** — Modify or remove the issue
- **Metadata grid** — Status, Priority, Reporter, Assignee, Created/Updated dates
- **Description** — Full bug report with steps to reproduce
- **Comments** — Team discussion about this bug

> 💡 **Use case**: A developer reads the description to understand what's broken, checks comments for additional context from the tester.

---

### Step 5: Change Status — Move Bug Through Lifecycle

On the detail page, use the **status dropdown** to change the bug's state.

![Status changed to RESOLVED](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/step5_change_status_1777621196022.png)

**The bug lifecycle:**
```
🆕 NEW          →  Bug just reported
🔵 OPEN         →  Bug confirmed, assigned to someone
🟡 IN_PROGRESS  →  Developer is actively working on it
🟢 RESOLVED     →  Developer finished the fix
⚫ CLOSED       →  Tester verified the fix works
```

**Rules enforced by the app:**
- You can't skip steps (e.g., can't go directly from NEW to RESOLVED)
- Assigning someone auto-changes NEW → OPEN
- Closed bugs can be reopened if the fix didn't work

> 💡 **Use case**: Developer john_dev fixes the login crash → changes status to RESOLVED. Tester alice_qa verifies → changes to CLOSED.

---

### Step 6: Add Comments — Team Discussion

Type in the comment box and click **"Send"**.

![New comment added to issue](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/step6_add_comment_1777621271835.png)

**What comments are for:**
- Developer explains what they found/fixed
- Tester provides additional reproduction steps
- Admin asks for priority change justification
- Anyone can discuss approach before implementing

> 💡 **Use case**: Developer comments "The crash was caused by missing input sanitization in InputValidator.java line 42. Fix pushed in PR #47."

---

### Step 7: Switch Users — Act as Different Roles

Use the **"Acting as"** dropdown in the top-right to switch between team members.

![Switched to alice_qa TESTER](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/step7_switch_user_1777621321128.png)

**Available users:**
| User | Role | Job |
|------|------|-----|
| admin | ADMIN | Manages the project |
| john_dev | DEVELOPER | Fixes backend bugs |
| jane_dev | DEVELOPER | Fixes frontend bugs |
| alice_qa | TESTER | Tests and reports bugs |
| bob_qa | TESTER | Tests and reports bugs |

> 💡 This simulates a real team. In production, each person would have their own login.

---

### Step 8: Create Issue — Report a New Bug

Click **"+ New"** and fill in the form.

![Filled create issue form](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/step8_form_filled_v2_1777621573855.png)

**Fields:**
- **Title** (required) — Short description of the bug
- **Description** — Detailed steps to reproduce, expected vs actual behavior
- **Priority** — How urgent it is (Critical/High/Medium/Low)
- **Assign To** — Who should fix it (optional)

After clicking **"Create Issue"**, it appears in the issues list:

![New issue #11 in the list](C:/Users/91720/.gemini/antigravity/brain/5236b868-1e68-4089-8f73-8935797827e1/step8_result_list_1777621599216.png)

> 💡 **Use case**: Tester alice_qa finds that notification emails aren't sending → reports it → assigns to jane_dev → jane_dev sees it in her queue.

---

## Real-World Scenario: End-to-End Bug Fix

Here's how a real team would use this app:

```
Day 1: alice_qa finds a login crash
       → Creates Issue #1 (Status: NEW, Priority: CRITICAL)

Day 1: admin sees it on the Dashboard (CRITICAL counter = 1!)
       → Assigns it to john_dev (Status auto-changes to OPEN)

Day 2: john_dev starts investigating
       → Changes status to IN_PROGRESS
       → Comments: "Reproducing now, looks like XSS vulnerability"

Day 2: john_dev pushes a code fix
       → Changes status to RESOLVED
       → Comments: "Fixed in PR #47, added input sanitization"

Day 3: alice_qa tests the fix
       → If it works: Changes status to CLOSED ✅
       → If it doesn't: Changes back to OPEN, comments "Still crashes with unicode input"
```

---

## Summary: What Each Button Does

| Button/Element | What It Does |
|---------------|-------------|
| **Dashboard** | Shows stats overview and charts |
| **Issues** | Lists all bugs with filters |
| **+ New** | Opens form to report a new bug |
| **Status dropdown** (on detail page) | Changes bug lifecycle status |
| **Assignee dropdown** (on detail page) | Assigns bug to a developer |
| **✏️ Edit** | Modifies title/description/priority |
| **🗑️ Delete** | Permanently removes the bug |
| **Comment input + Send** | Adds a team discussion message |
| **Acting as dropdown** | Switches which team member you are |
| **Search box** | Searches issues by text |
| **Filter dropdowns** | Filters by status or priority |
