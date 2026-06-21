# 🎓 UCC Student Portal

A full-featured desktop student information system built with **JavaFX** 
and **SQLite**, modeled after the University of Cape Coast (UCC) online 
portal. Supports three roles — **Student**, **Lecturer**, and **Admin** — 
each with a dedicated dashboard and complete workflow.

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue)
![SQLite](https://img.shields.io/badge/SQLite-3.45-lightgrey)
![Maven](https://img.shields.io/badge/Build-Maven-red)

---

## 📖 Overview

This project simulates a real university portal where:
- **Students** register for courses, view grades, track assignments and 
quizzes, pay fees, and check exam timetables.
- **Lecturers** enter grades, manage assignments/quizzes, and post 
announcements.
- **Admins** manage students, lecturers, courses, fee records, and approve 
grade submissions before they're released to students.

---

## 🛠 Tech Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| UI Framework | JavaFX 21 |
| Database | SQLite (via JDBC) |
| Password Security | BCrypt hashing |
| PDF Export | iText PDF |
| JSON (legacy/migration) | Gson |
| Build Tool | Maven |

---

## ✨ Features

### 🎓 Student
- Secure login with hashed passwords
- Dashboard with GPA, course progress, pending tasks at a glance
- Course registration (self-service, within open registration windows)
- Assignments & quizzes tracker with submission status
- Grade tracker with GPA chart (only shows admin-approved grades)
- Academic transcript with semester GPA & CGPA
- Export official result slip as PDF
- Fee payment tracker with balance & payment history
- Examination timetable with live countdown
- Calendar view of all deadlines
- Notifications & lecturer announcements
- Profile editing & password change with strength meter
- Dark mode toggle
- Auto-logout after 30 minutes of inactivity

### 👨‍🏫 Lecturer
- Dashboard with course/student/assignment stats
- Enter and update student grades per course
- Submit grades for admin approval
- View rejected submissions with remarks and resubmit
- Create and manage assignments & quizzes
- View enrolled students per course
- Post course announcements

### ⚙️ Admin
- Manage students, lecturers, and courses (add/edit/delete)
- Manage course enrollments
- **Grade approval workflow** — review and approve/reject lecturer 
submissions before release
- Fee management — record payments, add fee records, view collection 
summary
- Generate GPA and course performance reports
- Database backup & restore (.sql export/import)

---

## 🗄 Database Schema

17 tables including: `users`, `students`, `lecturers`, `courses`, 
`enrollments`, `assignments`, `submissions`, `quizzes`, `quiz_results`, 
`grades`, `grade_submissions`, `fees`, `fee_payments`, `exams`, 
`registration_windows`, `course_registrations`, `announcements`.

---

## 🚀 Getting Started

### Prerequisites
- JDK 21+
- Maven 3.9+
- IntelliJ IDEA (recommended)

### Setup

```bash
git clone https://github.com/Frimpong304/student-dashboard.git
cd student-dashboard
mvn clean compile
mvn javafx:run
```

The SQLite database (`ucc_portal.db`) and all tables are created and 
seeded automatically on first launch.

---

## 🔑 Demo Credentials

| Role | Email | Password |
|---|---|---|
| Student | `paul.kissi@ucc.edu.gh` | `pass1234` |
| Lecturer | `elliot.attipoe@ucc.edu.gh` | `lecturer1234` |
| Admin | `admin@ucc.edu.gh` | `admin1234` |

---

## 📦 Packaging as a Native App

To build a standalone Mac `.app`:

```bash
mvn clean package -DskipTests
jpackage \
  --name "UCC Student Portal" \
  --input target/libs \
  --main-jar student-dashboard-1.0-SNAPSHOT.jar \
  --main-class com.dashboard.App \
  --type app-image \
  --dest output \
  --module-path target/libs \
  --add-modules javafx.controls,javafx.fxml,java.sql,java.sql.rowset \
  --java-options "--module-path \$APPDIR --add-modules 
javafx.controls,javafx.fxml,java.sql,java.sql.rowset"
```

For a `.dmg` installer, change `--type app-image` to `--type dmg`.

> **Note:** Native packages are platform-specific. Build on the target OS 
(Windows, Mac, Linux) for a working installer on that platform.

---

## 📁 Project Structure

```
src/main/java/com/dashboard/
├── App.java
├── model/          → Student, Lecturer, Admin, Course, Grade, 
Assignment, Quiz
├── controller/      → JavaFX controllers for every screen
├── db/             → DatabaseManager (all SQLite logic)
└── util/           → SessionManager, PdfExporter

src/main/resources/
├── fxml/           → All screen layouts
└── css/            → UCC-branded stylesheet (green & gold theme)
```

---

## 🔒 Security

- BCrypt password hashing (no plain-text passwords stored)
- Prepared statements throughout (SQL injection protection)
- Role-based access control
- Grades hidden from students until admin approval
- Session timeout after inactivity

---

## 📜 License

This project was built for academic purposes as part of a university 
coursework assignment.

---

## 🙏 Acknowledgments

Built with guidance from Claude (Anthropic) as a learning project for 
JavaFX, SQLite, and full-stack desktop application architecture.
