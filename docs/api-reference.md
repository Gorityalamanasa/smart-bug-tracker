# API Reference

## Base URL
```
http://localhost:8080/api
```

## Issues

### List All Issues
```
GET /api/issues
GET /api/issues?status=OPEN
GET /api/issues?priority=CRITICAL
GET /api/issues?assigneeId=2
```

**Response**: `200 OK` — Array of Issue objects

### Get Issue by ID
```
GET /api/issues/{id}
```

**Response**: `200 OK` / `404 Not Found`

### Create Issue
```
POST /api/issues?reporterId=1
Content-Type: application/json

{
  "title": "Login button not working",
  "description": "The login button is unresponsive on Firefox",
  "priority": "HIGH"
}
```

**Response**: `201 Created`

### Update Issue
```
PUT /api/issues/{id}
Content-Type: application/json

{
  "title": "Updated title",
  "description": "Updated description",
  "priority": "CRITICAL"
}
```

### Change Status
```
PATCH /api/issues/{id}/status
Content-Type: application/json

{ "status": "IN_PROGRESS" }
```

Valid transitions: NEW→OPEN, OPEN→IN_PROGRESS, IN_PROGRESS→RESOLVED, RESOLVED→CLOSED, CLOSED→OPEN

### Assign Issue
```
PATCH /api/issues/{id}/assign
Content-Type: application/json

{ "assigneeId": 2 }
```

### Delete Issue
```
DELETE /api/issues/{id}
```

**Response**: `204 No Content`

---

## Comments

### List Comments
```
GET /api/issues/{issueId}/comments
```

### Add Comment
```
POST /api/issues/{issueId}/comments
Content-Type: application/json

{
  "authorId": 1,
  "content": "This needs urgent attention"
}
```

---

## Users

### List Users
```
GET /api/users
GET /api/users?role=DEVELOPER
```

### Get User
```
GET /api/users/{id}
```

---

## Dashboard

### Statistics
```
GET /api/dashboard/stats
```

**Response**:
```json
{
  "totalIssues": 8,
  "newIssues": 2,
  "openIssues": 2,
  "inProgressIssues": 2,
  "resolvedIssues": 1,
  "closedIssues": 1,
  "criticalIssues": 2,
  "highIssues": 2,
  "mediumIssues": 2,
  "lowIssues": 2
}
```

### Health Check
```
GET /api/health
```

**Response**:
```json
{
  "status": "UP",
  "application": "Smart Bug Tracker",
  "version": "1.0.0"
}
```
