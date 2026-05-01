# System Architecture

## Overview

The Smart Bug & Issue Tracking System follows a layered architecture pattern with clear separation of concerns.

## Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Frontend | HTML/CSS/JS | User interface |
| API | Spring Boot 3.4.5 | REST endpoints |
| Business Logic | Spring Services | Status transitions, validation |
| Data Access | Spring Data JPA | Repository pattern |
| Database | H2 (in-memory) | Data storage |
| Build | Maven 3.9 / Gradle 8.x | Compilation & packaging |
| CI/CD | Jenkins | Automated pipeline |
| Container | Docker | Deployment |

## Data Model

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│    users     │     │     issues       │     │   comments   │
├──────────────┤     ├──────────────────┤     ├──────────────┤
│ id (PK)      │◄──┐ │ id (PK)          │◄──┐ │ id (PK)      │
│ username     │   │ │ title            │   │ │ content      │
│ email        │   │ │ description      │   │ │ created_at   │
│ role (enum)  │   ├─│ reporter_id (FK) │   └─│ issue_id(FK) │
│ created_at   │   ├─│ assignee_id (FK) │     │ author_id(FK)│
└──────────────┘   │ │ status (enum)    │     └──────────────┘
                   │ │ priority (enum)  │
                   │ │ created_at       │
                   │ │ updated_at       │
                   │ └──────────────────┘
                   └── Foreign keys to users
```

## Status Workflow

```
NEW ──→ OPEN ──→ IN_PROGRESS ──→ RESOLVED ──→ CLOSED
 │        │          │              │            │
 │        │          └──→ OPEN ◄────┘            │
 │        └──→ CLOSED                            │
 └──→ CLOSED                     CLOSED ──→ OPEN (reopen)
```

Valid transitions are enforced in the IssueService. Assigning an issue to a user auto-transitions from NEW to OPEN.
