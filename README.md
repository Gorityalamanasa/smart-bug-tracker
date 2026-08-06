# 🐛 Smart Bug Tracker

A full-stack **AI-powered Bug Management System** built using **Spring Boot, Spring Security, JWT, MySQL, Docker, Jenkins, and Google Gemini AI**. The application streamlines bug reporting, assignment, and resolution with secure role-based access and AI-powered issue analysis.

---
## 📂 GitHub Repository

https://github.com/Gorityalamanasa/smart-bug-tracker

## 🚀 Local Deployment

Application:
http://localhost:8080

Jenkins:
http://localhost:9090
---
## 🚀 Features

- 🔐 JWT Authentication with Spring Security
- 👥 Role-Based Access Control (Admin, Developer, Tester)
- 🤖 AI-Powered Issue Analysis using Google Gemini
- 📊 Dashboard Analytics
- 📝 Issue & Comment Management
- 🎯 AI-based Priority and Developer Expertise Suggestions
- 🔍 Similar Issue Detection
- 🐳 Docker Containerization
- ⚙️ Jenkins CI/CD Pipeline

---

## 🛠️ Tech Stack

**Backend**
- Spring Boot
- Spring Security
- JWT
- MySQL
- JPA/Hibernate
- Maven

**AI**
- Google Gemini API

**DevOps**
- Docker
- Jenkins

**Frontend**
- HTML
- CSS
- JavaScript

---

## 🏗️ Workflow

```text
User Login
      ↓
JWT Authentication
      ↓
Tester Creates Issue
      ↓
Backend Calls Gemini AI
      ↓
AI Analyzes Issue
      ↓
Priority, Expertise & Similar Issue Suggestions
      ↓
Admin Reviews AI Suggestions
      ↓
Assigns Developer
      ↓
Developer Resolves Issue
      ↓
Tester Verifies & Closes/Reopens Issue
```

---

## 👥 Roles

### 👨‍💼 Admin
- View all issues
- Review AI suggestions
- Assign developers
- Manage users
- View dashboard analytics

### 👨‍💻 Developer
- View assigned issues
- Update issue status
- Add comments

### 🧪 Tester
- Create issues
- View own issues
- Verify resolved issues
- Reopen issues
- Add comments

---

## 🤖 AI Features

- Optimized Issue Summary
- Priority Recommendation
- Developer Expertise Recommendation
- Similar Issue Detection
- AI-assisted Decision Support for Admin

---

## 🐳 CI/CD

- Maven Build
- Docker Containerization
- Jenkins Pipeline Automation

---

## ⚙️ Installation

```bash
git clone <repository-url>

cd smart-bug-tracker

mvn spring-boot:run



```

Open:

```
http://localhost:8080
```

---

## 🔑 Environment Variables

```properties
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=

jwt.secret=

gemini.api.key=
```

---


## 👨‍💻 Author

**Gorityala Manasa**

⭐ If you found this project useful, consider starring the repository.
