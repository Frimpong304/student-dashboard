package com.dashboard.db;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDate;

/**
 * Central database manager for the UCC Student Portal.
 * Handles all SQLite connections, table creation, and seed data.
 *
 * Database file: ucc_portal.db (created at project root)
 *
 * Roles: STUDENT, LECTURER, ADMIN
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:ucc_portal.db";
    private static DatabaseManager instance;
    private Connection connection;

    // ── Singleton ─────────────────────────────────────────────────

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null)
            instance = new DatabaseManager();
        return instance;
    }

    // ── Connection ────────────────────────────────────────────────

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            // enable foreign keys
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON");
            }
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    // ── Initialize ────────────────────────────────────────────────

    /**
     * Creates all tables and seeds data if the database is fresh.
     * Called once on app startup from App.java.
     */
    public void initialize() {
        try {
            createTables();
            if (isDatabaseEmpty()) {
                seedData();
                System.out.println("✔ Database initialized and seeded.");
            } else {
                System.out.println("✔ Database already exists. Skipping seed.");
            }
        } catch (SQLException e) {
            System.err.println("Database initialization failed: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Table Creation ────────────────────────────────────────────

    private void createTables() throws SQLException {
        Connection conn = getConnection();

        // users — base table for all roles
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS users (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                name         TEXT    NOT NULL,
                email        TEXT    NOT NULL UNIQUE,
                password     TEXT    NOT NULL,
                role         TEXT    NOT NULL CHECK(role IN ('STUDENT','LECTURER','ADMIN')),
                created_at   TEXT    DEFAULT (date('now'))
            )
        """);

        // students
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS students (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id        INTEGER NOT NULL UNIQUE,
                student_number TEXT    NOT NULL UNIQUE,
                major          TEXT,
                year           INTEGER DEFAULT 1,
                profile_image  TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """);

        // lecturers
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS lecturers (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER NOT NULL UNIQUE,
                department TEXT,
                title      TEXT,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """);

        // courses
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS courses (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                code        TEXT    NOT NULL UNIQUE,
                name        TEXT    NOT NULL,
                credits     INTEGER NOT NULL DEFAULT 3,
                lecturer_id INTEGER,
                semester    TEXT,
                description TEXT,
                FOREIGN KEY (lecturer_id) REFERENCES lecturers(id)
            )
        """);

        // enrollments — links students to courses
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS enrollments (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                course_id  INTEGER NOT NULL,
                enrolled_at TEXT DEFAULT (date('now')),
                UNIQUE(student_id, course_id),
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
                FOREIGN KEY (course_id)  REFERENCES courses(id)  ON DELETE CASCADE
            )
        """);

        // assignments
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS assignments (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                course_id   INTEGER NOT NULL,
                title       TEXT    NOT NULL,
                description TEXT,
                due_date    TEXT,
                priority    TEXT    DEFAULT 'MEDIUM'
                                    CHECK(priority IN ('HIGH','MEDIUM','LOW')),
                created_at  TEXT    DEFAULT (date('now')),
                FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
            )
        """);

        // submissions — student responses to assignments
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS submissions (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                assignment_id INTEGER NOT NULL,
                student_id    INTEGER NOT NULL,
                submitted     INTEGER DEFAULT 0,
                score         REAL    DEFAULT -1,
                submitted_at  TEXT,
                feedback      TEXT,
                UNIQUE(assignment_id, student_id),
                FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
                FOREIGN KEY (student_id)    REFERENCES students(id)    ON DELETE CASCADE
            )
        """);

        // quizzes
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS quizzes (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                course_id       INTEGER NOT NULL,
                title           TEXT    NOT NULL,
                due_date        TEXT,
                total_questions INTEGER DEFAULT 10,
                duration_mins   INTEGER DEFAULT 20,
                created_at      TEXT    DEFAULT (date('now')),
                FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
            )
        """);

        // quiz_results — student quiz attempts
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS quiz_results (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                quiz_id    INTEGER NOT NULL,
                student_id INTEGER NOT NULL,
                completed  INTEGER DEFAULT 0,
                score      REAL    DEFAULT -1,
                taken_at   TEXT,
                UNIQUE(quiz_id, student_id),
                FOREIGN KEY (quiz_id)    REFERENCES quizzes(id)  ON DELETE CASCADE,
                FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
            )
        """);

        // grades — final course grades entered by lecturers
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS grades (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id   INTEGER NOT NULL,
                course_id    INTEGER NOT NULL,
                score        REAL    NOT NULL,
                letter_grade TEXT,
                semester     TEXT,
                entered_by   INTEGER,
                entered_at   TEXT    DEFAULT (date('now')),
                UNIQUE(student_id, course_id),
                FOREIGN KEY (student_id) REFERENCES students(id)  ON DELETE CASCADE,
                FOREIGN KEY (course_id)  REFERENCES courses(id)   ON DELETE CASCADE,
                FOREIGN KEY (entered_by) REFERENCES lecturers(id)
            )
        """);

        // announcements — lecturers post, students see
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS announcements (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                course_id   INTEGER,
                lecturer_id INTEGER,
                title       TEXT NOT NULL,
                body        TEXT,
                posted_at   TEXT DEFAULT (date('now')),
                FOREIGN KEY (course_id)   REFERENCES courses(id)   ON DELETE CASCADE,
                FOREIGN KEY (lecturer_id) REFERENCES lecturers(id)
            )
        """);
        // registration windows — admin opens/closes per semester
        conn.createStatement().execute("""
    CREATE TABLE IF NOT EXISTS registration_windows (
        id         INTEGER PRIMARY KEY AUTOINCREMENT,
        semester   TEXT    NOT NULL,
        open_date  TEXT    NOT NULL,
        close_date TEXT    NOT NULL,
        is_open    INTEGER DEFAULT 1,
        created_at TEXT    DEFAULT (date('now'))
    )
""");

// course registrations — student self-registers
        conn.createStatement().execute("""
    CREATE TABLE IF NOT EXISTS course_registrations (
        id          INTEGER PRIMARY KEY AUTOINCREMENT,
        student_id  INTEGER NOT NULL,
        course_id   INTEGER NOT NULL,
        semester    TEXT    NOT NULL,
        status      TEXT    DEFAULT 'PENDING'
                            CHECK(status IN
                            ('PENDING','APPROVED','DROPPED')),
        registered_at TEXT  DEFAULT (date('now')),
        UNIQUE(student_id, course_id, semester),
        FOREIGN KEY (student_id) REFERENCES students(id)
                                 ON DELETE CASCADE,
        FOREIGN KEY (course_id)  REFERENCES courses(id)
                                 ON DELETE CASCADE
    )
""");

// fee payments
        conn.createStatement().execute("""
    CREATE TABLE IF NOT EXISTS fees (
        id          INTEGER PRIMARY KEY AUTOINCREMENT,
        student_id  INTEGER NOT NULL,
        semester    TEXT    NOT NULL,
        amount      REAL    NOT NULL,
        paid        REAL    DEFAULT 0.0,
        due_date    TEXT,
        description TEXT,
        FOREIGN KEY (student_id) REFERENCES students(id)
                                 ON DELETE CASCADE
    )
""");

        conn.createStatement().execute("""
    CREATE TABLE IF NOT EXISTS fee_payments (
        id         INTEGER PRIMARY KEY AUTOINCREMENT,
        fee_id     INTEGER NOT NULL,
        amount     REAL    NOT NULL,
        paid_at    TEXT    DEFAULT (date('now')),
        method     TEXT    DEFAULT 'Cash',
        reference  TEXT,
        FOREIGN KEY (fee_id) REFERENCES fees(id)
                             ON DELETE CASCADE
    )
""");

// examination timetable
        conn.createStatement().execute("""
    CREATE TABLE IF NOT EXISTS exams (
        id         INTEGER PRIMARY KEY AUTOINCREMENT,
        course_id  INTEGER NOT NULL,
        exam_date  TEXT    NOT NULL,
        start_time TEXT    NOT NULL,
        end_time   TEXT    NOT NULL,
        venue      TEXT,
        semester   TEXT,
        type       TEXT    DEFAULT 'Final'
                           CHECK(type IN
                           ('Mid-Semester','Final','Resit')),
        FOREIGN KEY (course_id) REFERENCES courses(id)
                                ON DELETE CASCADE
    )
""");
        // results approval workflow
        conn.createStatement().execute("""
    CREATE TABLE IF NOT EXISTS grade_submissions (
        id          INTEGER PRIMARY KEY AUTOINCREMENT,
        course_id   INTEGER NOT NULL,
        lecturer_id INTEGER NOT NULL,
        semester    TEXT    NOT NULL,
        status      TEXT    DEFAULT 'PENDING'
                            CHECK(status IN
                            ('PENDING','APPROVED','REJECTED')),
        submitted_at TEXT   DEFAULT (date('now')),
        approved_at  TEXT,
        approved_by  INTEGER,
        remarks      TEXT,
        FOREIGN KEY (course_id)   REFERENCES courses(id),
        FOREIGN KEY (lecturer_id) REFERENCES lecturers(id),
        FOREIGN KEY (approved_by) REFERENCES users(id)
    )
""");

// add released column to grades if not exists
        try {
            conn.createStatement().execute(
                    "ALTER TABLE grades ADD COLUMN released INTEGER DEFAULT 0"
            );
        } catch (SQLException ignored) {
            // column already exists — safe to ignore
        }

        System.out.println("✔ Tables created.");
    }

    // ── Seed Data ─────────────────────────────────────────────────

    private boolean isDatabaseEmpty() throws SQLException {
        ResultSet rs = getConnection()
                .createStatement()
                .executeQuery("SELECT COUNT(*) FROM users");
        return rs.getInt(1) == 0;
    }

    private void seedData() throws SQLException {
        seedAdmin();
        seedLecturers();
        seedCourses();
        seedStudents();
        seedEnrollments();
        seedAssignments();
        seedSubmissions();
        seedQuizzes();
        seedQuizResults();
        seedGrades();
        seedAnnouncements();
        seedRegistrationWindow();
        seedFees();
        seedExams();

    }

    // ── Seed: Admin ───────────────────────────────────────────────

    private void seedAdmin() throws SQLException {
        String sql = """
            INSERT INTO users (name, email, password, role)
            VALUES (?, ?, ?, 'ADMIN')
        """;
        PreparedStatement ps = getConnection().prepareStatement(sql);
        ps.setString(1, "System Administrator");
        ps.setString(2, "admin@ucc.edu.gh");
        ps.setString(3, BCrypt.hashpw("admin1234", BCrypt.gensalt()));
        ps.executeUpdate();
        System.out.println("✔ Seeded admin.");
    }

    // ── Seed: Lecturers ───────────────────────────────────────────

    private void seedLecturers() throws SQLException {
        String userSql = """
            INSERT INTO users (name, email, password, role)
            VALUES (?, ?, ?, 'LECTURER')
        """;
        String lecturerSql = """
            INSERT INTO lecturers (user_id, department, title)
            VALUES (?, ?, ?)
        """;

        Object[][] lecturers = {
                {"Elliot Attipoe",          "elliot.attipoe@ucc.edu.gh",     "Mr.",  "Computer Science"},
                {"Emmanuel Tetteh",         "emmanuel.tetteh@ucc.edu.gh",    "Dr.",  "Computer Science"},
                {"Sandro Amofa",            "sandro.amofa@ucc.edu.gh",       "Mr.",  "Computer Science"},
                {"Benard Buckman",          "benard.buckman@ucc.edu.gh",     "Mr.",  "Computer Science"},
                {"Alimatu-Saadia Yussiff",  "alimatu.yussiff@ucc.edu.gh",   "Mrs.", "Computer Science"},
                {"Richard Adu Agyapong",    "richard.agyapong@ucc.edu.gh",   "Dr.",  "Business Studies"},
        };

        for (Object[] l : lecturers) {
            // insert user
            PreparedStatement ps = getConnection().prepareStatement(
                    userSql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, (String) l[0]);
            ps.setString(2, (String) l[1]);
            ps.setString(3, BCrypt.hashpw("lecturer1234", BCrypt.gensalt()));
            ps.executeUpdate();

            // get generated user id
            ResultSet keys = ps.getGeneratedKeys();
            int userId = keys.getInt(1);

            // insert lecturer
            PreparedStatement lps = getConnection().prepareStatement(lecturerSql);
            lps.setInt(1, userId);
            lps.setString(2, (String) l[3]);
            lps.setString(3, (String) l[2]);
            lps.executeUpdate();
        }
        System.out.println("✔ Seeded lecturers.");
    }

    // ── Seed: Courses ─────────────────────────────────────────────

    private void seedCourses() throws SQLException {
        String sql = """
            INSERT INTO courses (code, name, credits, lecturer_id, semester, description)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        // lecturer_id matches insertion order above (1=Attipoe, 2=Tetteh etc.)
        Object[][] courses = {
                {"CSC302", "Prog. & Problem Analysis II (Java)", 3, 1,
                        "2025/2026 Second Semester",
                        "Advanced Java programming focusing on OOP, collections and design patterns."},
                {"CSC314", "Network Computing I", 3, 2,
                        "2025/2026 Second Semester",
                        "Fundamentals of computer networking including OSI model, TCP/IP and subnetting."},
                {"CSC399", "Research Methods", 3, 3,
                        "2025/2026 Second Semester",
                        "Introduction to research design, data collection and academic writing."},
                {"MAT302", "Advanced Calculus II", 3, null,
                        "2025/2026 Second Semester",
                        "Sequences, series, integration techniques and multivariable calculus."},
                {"CSC312", "Human Computer Interface", 3, 4,
                        "2025/2026 Second Semester",
                        "Principles of UI/UX design, usability testing and prototyping."},
                {"CSC316", "Web Technologies", 3, 5,
                        "2025/2026 Second Semester",
                        "HTML, CSS, JavaScript, React and modern web development practices."},
                {"ENT302", "Introduction to Entrepreneurship", 2, 6,
                        "2025/2026 Second Semester",
                        "Business plan development, market analysis and startup fundamentals."},
        };

        for (Object[] c : courses) {
            PreparedStatement ps = getConnection().prepareStatement(sql);
            ps.setString(1, (String) c[0]);
            ps.setString(2, (String) c[1]);
            ps.setInt(3,    (Integer) c[2]);
            if (c[3] == null) ps.setNull(4, Types.INTEGER);
            else              ps.setInt(4, (Integer) c[3]);
            ps.setString(5, (String) c[4]);
            ps.setString(6, (String) c[5]);
            ps.executeUpdate();
        }
        System.out.println("✔ Seeded courses.");
    }

    // ── Seed: Students ────────────────────────────────────────────

    private void seedStudents() throws SQLException {
        String userSql = """
            INSERT INTO users (name, email, password, role)
            VALUES (?, ?, ?, 'STUDENT')
        """;
        String studentSql = """
            INSERT INTO students
                (user_id, student_number, major, year, profile_image)
            VALUES (?, ?, ?, ?, ?)
        """;

        Object[][] students = {
                {"Paul Kissi",   "paul.kissi@ucc.edu.gh",   "pass1234",
                        "PS/CSC/23/0210", "Computer Science", 3, "/images/paul.jpg"},
                {"Brenda Mensah","brenda.mensah@ucc.edu.gh", "pass1234",
                        "PS/PHY/22/0045", "Physics", 3, null},
                {"Kwame Asante", "kwame.asante@ucc.edu.gh",  "pass1234",
                        "PS/CSC/23/0211", "Computer Science", 3, null},
                {"Abena Owusu",  "abena.owusu@ucc.edu.gh",   "pass1234",
                        "PS/CSC/23/0212", "Computer Science", 3, null},
        };

        for (Object[] s : students) {
            // insert user
            PreparedStatement ps = getConnection().prepareStatement(
                    userSql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, (String) s[0]);
            ps.setString(2, (String) s[1]);
            ps.setString(3, BCrypt.hashpw((String) s[2], BCrypt.gensalt()));
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int userId = keys.getInt(1);

            // insert student
            PreparedStatement sps = getConnection().prepareStatement(studentSql);
            sps.setInt(1,    userId);
            sps.setString(2, (String) s[3]);
            sps.setString(3, (String) s[4]);
            sps.setInt(4,    (Integer) s[5]);
            if (s[6] == null) sps.setNull(5, Types.VARCHAR);
            else              sps.setString(5, (String) s[6]);
            sps.executeUpdate();
        }
        System.out.println("✔ Seeded students.");
    }

    // ── Seed: Enrollments ─────────────────────────────────────────

    private void seedEnrollments() throws SQLException {
        String sql = """
            INSERT INTO enrollments (student_id, course_id)
            VALUES (?, ?)
        """;

        // Paul (student_id=1) enrolled in all 7 courses
        int[][] paulCourses = {{1,1},{1,2},{1,3},{1,4},{1,5},{1,6},{1,7}};

        // Brenda (student_id=2) — Physics student, different courses
        // Kwame (student_id=3) — same CS courses as Paul
        // Abena (student_id=4) — same CS courses as Paul
        int[][] otherEnrollments = {
                {3,1},{3,2},{3,3},{3,4},{3,5},{3,6},{3,7},
                {4,1},{4,2},{4,3},{4,4},{4,5},{4,6},{4,7},
        };

        PreparedStatement ps = getConnection().prepareStatement(sql);
        for (int[] e : paulCourses) {
            ps.setInt(1, e[0]); ps.setInt(2, e[1]);
            ps.executeUpdate();
        }
        for (int[] e : otherEnrollments) {
            ps.setInt(1, e[0]); ps.setInt(2, e[1]);
            ps.executeUpdate();
        }
        System.out.println("✔ Seeded enrollments.");
    }

    // ── Seed: Assignments ─────────────────────────────────────────

    private void seedAssignments() throws SQLException {
        String sql = """
            INSERT INTO assignments
                (course_id, title, description, due_date, priority)
            VALUES (?, ?, ?, ?, ?)
        """;

        LocalDate today = LocalDate.now();
        Object[][] assignments = {
                // CSC302 (course_id=1)
                {1, "Java Lab 1 - OOP Basics",
                        "Implement a class hierarchy in Java",
                        today.minusDays(20).toString(), "HIGH"},
                {1, "Java Lab 2 - Exception Handling",
                        "Handle runtime exceptions in a banking app",
                        today.minusDays(10).toString(), "HIGH"},
                {1, "Java Lab 3 - Collections",
                        "Use ArrayList and HashMap in a student registry",
                        today.plusDays(5).toString(), "HIGH"},

                // CSC314 (course_id=2)
                {2, "Network Lab 1 - OSI Model",
                        "Map a real-world scenario to OSI layers",
                        today.minusDays(18).toString(), "MEDIUM"},
                {2, "Network Lab 2 - IP Addressing",
                        "Subnetting exercises for a campus network",
                        today.minusDays(7).toString(), "HIGH"},
                {2, "Network Lab 3 - LAN Setup",
                        "Configure a basic LAN using Cisco Packet Tracer",
                        today.plusDays(6).toString(), "HIGH"},

                // CSC399 (course_id=3)
                {3, "Research Proposal",
                        "Write a 2-page research proposal on AI in healthcare",
                        today.minusDays(15).toString(), "HIGH"},
                {3, "Literature Review",
                        "Review 5 journal articles related to your research topic",
                        today.minusDays(5).toString(), "HIGH"},
                {3, "Methodology Draft",
                        "Describe your research design and data collection method",
                        today.plusDays(8).toString(), "MEDIUM"},

                // MAT302 (course_id=4)
                {4, "Problem Set 1 - Sequences",
                        "Chapter 3 exercises 1-20",
                        today.minusDays(22).toString(), "MEDIUM"},
                {4, "Problem Set 2 - Integration",
                        "Evaluate definite and indefinite integrals",
                        today.minusDays(8).toString(), "HIGH"},
                {4, "Problem Set 3 - Series",
                        "Convergence tests for infinite series",
                        today.plusDays(4).toString(), "HIGH"},

                // CSC312 (course_id=5)
                {5, "HCI Report 1 - Heuristic Evaluation",
                        "Evaluate a mobile app using Nielsen's 10 heuristics",
                        today.minusDays(16).toString(), "MEDIUM"},
                {5, "HCI Report 2 - User Personas",
                        "Create 3 user personas for a university portal",
                        today.minusDays(6).toString(), "MEDIUM"},
                {5, "HCI Prototype",
                        "Build a low-fidelity prototype of your redesigned app",
                        today.plusDays(7).toString(), "HIGH"},

                // CSC316 (course_id=6)
                {6, "Web Lab 1 - HTML/CSS",
                        "Build a responsive university landing page",
                        today.minusDays(19).toString(), "MEDIUM"},
                {6, "Web Lab 2 - JavaScript",
                        "Add interactivity to your landing page using JS",
                        today.minusDays(9).toString(), "HIGH"},
                {6, "Web Lab 3 - React Basics",
                        "Build a simple React component for a student list",
                        today.plusDays(6).toString(), "HIGH"},

                // ENT302 (course_id=7)
                {7, "Business Idea Pitch",
                        "Present a 1-page summary of a tech startup idea",
                        today.minusDays(14).toString(), "LOW"},
                {7, "Business Plan Draft",
                        "Outline market analysis and revenue model",
                        today.minusDays(4).toString(), "MEDIUM"},
                {7, "Final Business Plan",
                        "Full business plan with financial projections",
                        today.plusDays(10).toString(), "HIGH"},
        };

        PreparedStatement ps = getConnection().prepareStatement(sql);
        for (Object[] a : assignments) {
            ps.setInt(1,    (Integer) a[0]);
            ps.setString(2, (String)  a[1]);
            ps.setString(3, (String)  a[2]);
            ps.setString(4, (String)  a[3]);
            ps.setString(5, (String)  a[4]);
            ps.executeUpdate();
        }
        System.out.println("✔ Seeded assignments.");
    }

    // ── Seed: Submissions ─────────────────────────────────────────

    private void seedSubmissions() throws SQLException {
        String sql = """
            INSERT INTO submissions
                (assignment_id, student_id, submitted, score, submitted_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        LocalDate today = LocalDate.now();

        // Paul's submissions (student_id=1)
        // assignment_id, student_id, submitted, score, submitted_at
        Object[][] submissions = {
                {1,  1, 1, 93.0, today.minusDays(19).toString()},
                {2,  1, 1, 91.0, today.minusDays(9).toString()},
                {3,  1, 0, -1.0, null},
                {4,  1, 1, 92.0, today.minusDays(17).toString()},
                {5,  1, 1, 90.0, today.minusDays(6).toString()},
                {6,  1, 0, -1.0, null},
                {7,  1, 1, 90.0, today.minusDays(14).toString()},
                {8,  1, 1, 91.0, today.minusDays(4).toString()},
                {9,  1, 0, -1.0, null},
                {10, 1, 1, 82.0, today.minusDays(21).toString()},
                {11, 1, 1, 84.0, today.minusDays(7).toString()},
                {12, 1, 0, -1.0, null},
                {13, 1, 1, 95.0, today.minusDays(15).toString()},
                {14, 1, 1, 93.0, today.minusDays(5).toString()},
                {15, 1, 0, -1.0, null},
                {16, 1, 1, 97.0, today.minusDays(18).toString()},
                {17, 1, 1, 96.0, today.minusDays(8).toString()},
                {18, 1, 0, -1.0, null},
                {19, 1, 1, 92.0, today.minusDays(13).toString()},
                {20, 1, 1, 93.0, today.minusDays(3).toString()},
                {21, 1, 0, -1.0, null},
        };

        PreparedStatement ps = getConnection().prepareStatement(sql);
        for (Object[] s : submissions) {
            ps.setInt(1,    (Integer) s[0]);
            ps.setInt(2,    (Integer) s[1]);
            ps.setInt(3,    (Integer) s[2]);
            ps.setDouble(4, (Double)  s[3]);
            if (s[4] == null) ps.setNull(5, Types.VARCHAR);
            else              ps.setString(5, (String) s[4]);
            ps.executeUpdate();
        }
        System.out.println("✔ Seeded submissions.");
    }

    // ── Seed: Quizzes ─────────────────────────────────────────────

    private void seedQuizzes() throws SQLException {
        String sql = """
            INSERT INTO quizzes
                (course_id, title, due_date, total_questions, duration_mins)
            VALUES (?, ?, ?, ?, ?)
        """;

        LocalDate today = LocalDate.now();
        Object[][] quizzes = {
                {1, "Java Quiz 1 - OOP Concepts",    today.minusDays(18).toString(), 10, 20},
                {1, "Java Quiz 2 - Inheritance",     today.minusDays(8).toString(),  10, 20},
                {1, "Java Quiz 3 - Collections",     today.plusDays(5).toString(),   10, 20},
                {2, "Networking Quiz 1 - OSI Model", today.minusDays(17).toString(), 12, 25},
                {2, "Networking Quiz 2 - TCP/IP",    today.minusDays(6).toString(),  12, 25},
                {2, "Networking Quiz 3 - Subnetting",today.plusDays(6).toString(),   12, 25},
                {3, "Research Quiz 1 - Types",       today.minusDays(14).toString(), 10, 15},
                {3, "Research Quiz 2 - Data Collection", today.plusDays(8).toString(), 10, 15},
                {4, "Calculus Quiz 1 - Limits",      today.minusDays(20).toString(), 15, 30},
                {4, "Calculus Quiz 2 - Derivatives", today.minusDays(9).toString(),  15, 30},
                {4, "Calculus Quiz 3 - Integration", today.plusDays(4).toString(),   15, 30},
                {5, "HCI Quiz 1 - Design Principles",today.minusDays(15).toString(), 10, 15},
                {5, "HCI Quiz 2 - Usability Testing",today.plusDays(7).toString(),   10, 15},
                {6, "Web Quiz 1 - HTML/CSS",         today.minusDays(18).toString(), 10, 20},
                {6, "Web Quiz 2 - JavaScript DOM",   today.minusDays(7).toString(),  10, 20},
                {6, "Web Quiz 3 - React",            today.plusDays(5).toString(),   10, 20},
                {7, "ENT Quiz 1 - Concepts",         today.minusDays(13).toString(), 10, 15},
                {7, "ENT Quiz 2 - Business Models",  today.plusDays(9).toString(),   10, 15},
        };

        PreparedStatement ps = getConnection().prepareStatement(sql);
        for (Object[] q : quizzes) {
            ps.setInt(1,    (Integer) q[0]);
            ps.setString(2, (String)  q[1]);
            ps.setString(3, (String)  q[2]);
            ps.setInt(4,    (Integer) q[3]);
            ps.setInt(5,    (Integer) q[4]);
            ps.executeUpdate();
        }
        System.out.println("✔ Seeded quizzes.");
    }

    // ── Seed: Quiz Results ────────────────────────────────────────

    private void seedQuizResults() throws SQLException {
        String sql = """
            INSERT INTO quiz_results
                (quiz_id, student_id, completed, score, taken_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        LocalDate today = LocalDate.now();

        // Paul's quiz results (student_id=1)
        Object[][] results = {
                {1,  1, 1, 92.0, today.minusDays(17).toString()},
                {2,  1, 1, 93.0, today.minusDays(7).toString()},
                {3,  1, 0, -1.0, null},
                {4,  1, 1, 91.0, today.minusDays(16).toString()},
                {5,  1, 1, 90.0, today.minusDays(5).toString()},
                {6,  1, 0, -1.0, null},
                {7,  1, 1, 90.0, today.minusDays(13).toString()},
                {8,  1, 0, -1.0, null},
                {9,  1, 1, 82.0, today.minusDays(19).toString()},
                {10, 1, 1, 84.0, today.minusDays(8).toString()},
                {11, 1, 0, -1.0, null},
                {12, 1, 1, 94.0, today.minusDays(14).toString()},
                {13, 1, 0, -1.0, null},
                {14, 1, 1, 97.0, today.minusDays(17).toString()},
                {15, 1, 1, 96.0, today.minusDays(6).toString()},
                {16, 1, 0, -1.0, null},
                {17, 1, 1, 92.0, today.minusDays(12).toString()},
                {18, 1, 0, -1.0, null},
        };

        PreparedStatement ps = getConnection().prepareStatement(sql);
        for (Object[] r : results) {
            ps.setInt(1,    (Integer) r[0]);
            ps.setInt(2,    (Integer) r[1]);
            ps.setInt(3,    (Integer) r[2]);
            ps.setDouble(4, (Double)  r[3]);
            if (r[4] == null) ps.setNull(5, Types.VARCHAR);
            else              ps.setString(5, (String) r[4]);
            ps.executeUpdate();
        }
        System.out.println("✔ Seeded quiz results.");
    }

    // ── Seed: Grades ──────────────────────────────────────────────

    private void seedGrades() throws SQLException {
        String sql = """
            INSERT INTO grades
                (student_id, course_id, score, letter_grade, semester, entered_by)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        // GPA target 3.85:
        // CSC302→92 A, CSC314→91 A, CSC399→90 A,
        // MAT302→83 B, CSC312→94 A, CSC316→97 A, ENT302→92 A
        // Points: 4+4+4+3+4+4+4 = 27/7 = 3.857 ≈ 3.85 ✔
        Object[][] grades = {
                {1, 1, 92.0, "A", "2025/2026 Second Semester", 1},
                {1, 2, 91.0, "A", "2025/2026 Second Semester", 2},
                {1, 3, 90.0, "A", "2025/2026 Second Semester", 3},
                {1, 4, 83.0, "B", "2025/2026 Second Semester", null},
                {1, 5, 94.0, "A", "2025/2026 Second Semester", 4},
                {1, 6, 97.0, "A", "2025/2026 Second Semester", 5},
                {1, 7, 92.0, "A", "2025/2026 Second Semester", 6},
        };

        PreparedStatement ps = getConnection().prepareStatement(sql);
        for (Object[] g : grades) {
            ps.setInt(1,    (Integer) g[0]);
            ps.setInt(2,    (Integer) g[1]);
            ps.setDouble(3, (Double)  g[2]);
            ps.setString(4, (String)  g[3]);
            ps.setString(5, (String)  g[4]);
            if (g[5] == null) ps.setNull(6, Types.INTEGER);
            else              ps.setInt(6, (Integer) g[5]);
            ps.executeUpdate();
        }
        System.out.println("✔ Seeded grades.");
    }

    // ── Seed: Announcements ───────────────────────────────────────

    private void seedAnnouncements() throws SQLException {
        String sql = """
            INSERT INTO announcements
                (course_id, lecturer_id, title, body, posted_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        LocalDate today = LocalDate.now();
        Object[][] announcements = {
                {1, 1,
                        "Lab 3 Submission Deadline Extended",
                        "The deadline for Java Lab 3 has been extended by 2 days. "
                                + "Ensure all code is well commented before submission.",
                        today.minusDays(1).toString()},
                {2, 2,
                        "Packet Tracer Installation Guide",
                        "Please download and install Cisco Packet Tracer before "
                                + "the next lab session. Installation guide on the course page.",
                        today.minusDays(3).toString()},
                {3, 3,
                        "Research Proposal Feedback",
                        "Feedback for Research Proposal submissions has been uploaded. "
                                + "Check your student portal for individual comments.",
                        today.minusDays(2).toString()},
                {5, 4,
                        "HCI Prototype Demo Week",
                        "Prototype demonstrations will be held during the week of "
                                + today.plusDays(14) + ". Prepare a 5-minute walkthrough.",
                        today.toString()},
                {6, 5,
                        "React Workshop This Friday",
                        "An optional React workshop will be held this Friday at 2PM "
                                + "in Lab 4. Highly recommended for Web Lab 3.",
                        today.toString()},
                {7, 6,
                        "Guest Lecturer Next Week",
                        "A successful UCC alumnus and startup founder will be joining "
                                + "us next week to share their entrepreneurship journey.",
                        today.minusDays(1).toString()},
        };

        PreparedStatement ps = getConnection().prepareStatement(sql);
        for (Object[] a : announcements) {
            ps.setInt(1,    (Integer) a[0]);
            ps.setInt(2,    (Integer) a[1]);
            ps.setString(3, (String)  a[2]);
            ps.setString(4, (String)  a[3]);
            ps.setString(5, (String)  a[4]);
            ps.executeUpdate();
        }
        System.out.println("✔ Seeded announcements.");
    }
    private void seedRegistrationWindow() throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO registration_windows
            (semester, open_date, close_date, is_open)
        VALUES (?, ?, ?, ?)
    """);
        ps.setString(1, "2025/2026 Second Semester");
        ps.setString(2, LocalDate.now().minusDays(5).toString());
        ps.setString(3, LocalDate.now().plusDays(14).toString());
        ps.setInt(4, 1);
        ps.executeUpdate();
        System.out.println("✔ Seeded registration window.");
    }

    private void seedFees() throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO fees
            (student_id, semester, amount, paid, due_date,
             description)
        VALUES (?, ?, ?, ?, ?, ?)
    """);

        LocalDate today = LocalDate.now();

        // Paul's fees (student_id = 1)
        Object[][] fees = {
                {1, "2025/2026 Second Semester",
                        2500.00, 2500.00,
                        today.minusDays(30).toString(),
                        "Tuition Fees"},
                {1, "2025/2026 Second Semester",
                        150.00, 150.00,
                        today.minusDays(30).toString(),
                        "Sports & Recreation Levy"},
                {1, "2025/2026 Second Semester",
                        80.00, 0.00,
                        today.plusDays(14).toString(),
                        "Library Levy"},
                {1, "2025/2026 Second Semester",
                        120.00, 60.00,
                        today.plusDays(7).toString(),
                        "Examination Fees"},
                // Brenda's fees (student_id = 2)
                {2, "2025/2026 Second Semester",
                        2500.00, 1500.00,
                        today.minusDays(30).toString(),
                        "Tuition Fees"},
                {2, "2025/2026 Second Semester",
                        150.00, 0.00,
                        today.plusDays(14).toString(),
                        "Sports & Recreation Levy"},
        };

        for (Object[] f : fees) {
            ps.setInt(1,    (Integer) f[0]);
            ps.setString(2, (String)  f[1]);
            ps.setDouble(3, (Double)  f[2]);
            ps.setDouble(4, (Double)  f[3]);
            ps.setString(5, (String)  f[4]);
            ps.setString(6, (String)  f[5]);
            ps.executeUpdate();
        }

        // seed some payment records for Paul
        PreparedStatement pps = getConnection().prepareStatement("""
        INSERT INTO fee_payments
            (fee_id, amount, paid_at, method, reference)
        VALUES (?, ?, ?, ?, ?)
    """);

        Object[][] payments = {
                {1, 2500.00, today.minusDays(28).toString(),
                        "Mobile Money", "MM-2025-001234"},
                {2, 150.00,  today.minusDays(28).toString(),
                        "Mobile Money", "MM-2025-001235"},
                {4, 60.00,   today.minusDays(10).toString(),
                        "Bank Transfer", "BT-2025-004521"},
        };

        for (Object[] p : payments) {
            pps.setInt(1,    (Integer) p[0]);
            pps.setDouble(2, (Double)  p[1]);
            pps.setString(3, (String)  p[2]);
            pps.setString(4, (String)  p[3]);
            pps.setString(5, (String)  p[4]);
            pps.executeUpdate();
        }

        System.out.println("✔ Seeded fees and payments.");
    }

    private void seedExams() throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO exams
            (course_id, exam_date, start_time, end_time,
             venue, semester, type)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """);

        LocalDate base = LocalDate.now().plusDays(30);
        String sem     = "2025/2026 Second Semester";

        Object[][] exams = {
                // Mid-semester exams
                {1, base.toString(),
                        "08:00", "10:00", "Main Hall A", sem, "Mid-Semester"},
                {2, base.plusDays(1).toString(),
                        "10:00", "12:00", "Science Block B", sem, "Mid-Semester"},
                {3, base.plusDays(2).toString(),
                        "14:00", "16:00", "Main Hall B", sem, "Mid-Semester"},
                {4, base.plusDays(3).toString(),
                        "08:00", "10:00", "Main Hall A", sem, "Mid-Semester"},
                {5, base.plusDays(4).toString(),
                        "10:00", "12:00", "IT Lab 1", sem, "Mid-Semester"},
                {6, base.plusDays(5).toString(),
                        "14:00", "16:00", "Science Block A", sem, "Mid-Semester"},
                {7, base.plusDays(6).toString(),
                        "08:00", "10:00", "Main Hall C", sem, "Mid-Semester"},

                // Final exams
                {1, base.plusDays(45).toString(),
                        "08:00", "11:00", "Main Hall A", sem, "Final"},
                {2, base.plusDays(46).toString(),
                        "10:00", "13:00", "Science Block B", sem, "Final"},
                {3, base.plusDays(47).toString(),
                        "14:00", "17:00", "Main Hall B", sem, "Final"},
                {4, base.plusDays(48).toString(),
                        "08:00", "11:00", "Main Hall A", sem, "Final"},
                {5, base.plusDays(49).toString(),
                        "10:00", "13:00", "IT Lab 1", sem, "Final"},
                {6, base.plusDays(50).toString(),
                        "14:00", "17:00", "Science Block A", sem, "Final"},
                {7, base.plusDays(51).toString(),
                        "08:00", "10:00", "Main Hall C", sem, "Final"},
        };

        for (Object[] e : exams) {
            ps.setInt(1,    (Integer) e[0]);
            ps.setString(2, (String)  e[1]);
            ps.setString(3, (String)  e[2]);
            ps.setString(4, (String)  e[3]);
            ps.setString(5, (String)  e[4]);
            ps.setString(6, (String)  e[5]);
            ps.setString(7, (String)  e[6]);
            ps.executeUpdate();
        }
        System.out.println("✔ Seeded exams.");
    }

    // ── Query Helpers ─────────────────────────────────────────────
    // ── Safe Query Helpers ────────────────────────────────────────

    /**
     * Authenticate any user by email and password.
     */
    public ResultSet authenticateUser(String email, String password)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement(
                "SELECT * FROM users WHERE email = ?");
        ps.setString(1, email.trim().toLowerCase());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            String hashed = rs.getString("password");
            if (BCrypt.checkpw(password, hashed))
                return rs;
        }
        return null;
    }

    public String getUserRole(String email) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement(
                "SELECT role FROM users WHERE email = ?");
        ps.setString(1, email.trim().toLowerCase());
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getString("role") : null;
    }

    public ResultSet getStudentByEmail(String email)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT u.id as user_id, u.name, u.email, u.role,
               s.id as student_id, s.student_number,
               s.major, s.year, s.profile_image
        FROM users u
        JOIN students s ON s.user_id = u.id
        WHERE u.email = ?
    """);
        ps.setString(1, email.trim().toLowerCase());
        return ps.executeQuery();
    }

    public ResultSet getLecturerByEmail(String email)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT u.id as user_id, u.name, u.email,
               l.id as lecturer_id, l.department, l.title
        FROM users u
        JOIN lecturers l ON l.user_id = u.id
        WHERE u.email = ?
    """);
        ps.setString(1, email.trim().toLowerCase());
        return ps.executeQuery();
    }

    public ResultSet getCoursesForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT c.id, c.code, c.name, c.credits,
               c.semester, c.description,
               u.name as lecturer_name,
               l.title as lecturer_title
        FROM courses c
        JOIN enrollments e ON e.course_id = c.id
        LEFT JOIN lecturers l ON l.id = c.lecturer_id
        LEFT JOIN users u ON u.id = l.user_id
        WHERE e.student_id = ?
        ORDER BY c.code
    """);
        ps.setInt(1, studentId);
        return ps.executeQuery();
    }

    public ResultSet getCoursesForLecturer(int lecturerId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT c.id, c.code, c.name, c.credits,
               c.semester, c.description,
               COUNT(e.student_id) as enrolled_count
        FROM courses c
        LEFT JOIN enrollments e ON e.course_id = c.id
        WHERE c.lecturer_id = ?
        GROUP BY c.id
        ORDER BY c.code
    """);
        ps.setInt(1, lecturerId);
        return ps.executeQuery();
    }

    public ResultSet getGradesForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT g.id, c.code, c.name, g.score,
               g.letter_grade, g.semester, g.entered_at
        FROM grades g
        JOIN courses c ON c.id = g.course_id
        WHERE g.student_id = ?
        ORDER BY c.code
    """);
        ps.setInt(1, studentId);
        return ps.executeQuery();
    }

    public ResultSet getStudentsInCourse(int courseId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT u.name, s.student_number,
               s.id as student_id,
               g.score, g.letter_grade
        FROM enrollments e
        JOIN students s ON s.id = e.student_id
        JOIN users u ON u.id = s.user_id
        LEFT JOIN grades g ON g.student_id = s.id
                           AND g.course_id = ?
        WHERE e.course_id = ?
        ORDER BY u.name
    """);
        ps.setInt(1, courseId);
        ps.setInt(2, courseId);
        return ps.executeQuery();
    }

    public void upsertGrade(int studentId, int courseId,
                            double score, int lecturerId, String semester)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO grades
            (student_id, course_id, score, letter_grade,
             semester, entered_by, entered_at)
        VALUES (?, ?, ?, ?, ?, ?, date('now'))
        ON CONFLICT(student_id, course_id)
        DO UPDATE SET score        = excluded.score,
                      letter_grade = excluded.letter_grade,
                      entered_by   = excluded.entered_by,
                      entered_at   = date('now')
    """);
        ps.setInt(1,    studentId);
        ps.setInt(2,    courseId);
        ps.setDouble(3, score);
        ps.setString(4, deriveLetterGrade(score));
        ps.setString(5, semester);
        ps.setInt(6,    lecturerId);
        ps.executeUpdate();
    }

    public void submitAssignment(int assignmentId, int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO submissions
            (assignment_id, student_id, submitted, submitted_at)
        VALUES (?, ?, 1, date('now'))
        ON CONFLICT(assignment_id, student_id)
        DO UPDATE SET submitted    = 1,
                      submitted_at = date('now')
    """);
        ps.setInt(1, assignmentId);
        ps.setInt(2, studentId);
        ps.executeUpdate();
    }

    public ResultSet getAssignmentsForCourse(int courseId,
                                             int studentId) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT a.id, a.title, a.description, a.due_date,
               a.priority,
               COALESCE(sub.submitted, 0) as submitted,
               COALESCE(sub.score, -1)    as score,
               sub.feedback
        FROM assignments a
        LEFT JOIN submissions sub
               ON sub.assignment_id = a.id
              AND sub.student_id = ?
        WHERE a.course_id = ?
        ORDER BY a.due_date ASC
    """);
        ps.setInt(1, studentId);
        ps.setInt(2, courseId);
        return ps.executeQuery();
    }

    public ResultSet getQuizzesForCourse(int courseId,
                                         int studentId) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT q.id, q.title, q.due_date,
               q.total_questions, q.duration_mins,
               COALESCE(qr.completed, 0) as completed,
               COALESCE(qr.score, -1)    as score
        FROM quizzes q
        LEFT JOIN quiz_results qr
               ON qr.quiz_id = q.id
              AND qr.student_id = ?
        WHERE q.course_id = ?
        ORDER BY q.due_date ASC
    """);
        ps.setInt(1, studentId);
        ps.setInt(2, courseId);
        return ps.executeQuery();
    }

    public ResultSet getAnnouncementsForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT a.title, a.body, a.posted_at,
               c.code as course_code, c.name as course_name,
               u.name as lecturer_name,
               l.title as lecturer_title
        FROM announcements a
        JOIN courses c ON c.id = a.course_id
        JOIN lecturers l ON l.id = a.lecturer_id
        JOIN users u ON u.id = l.user_id
        WHERE c.id IN (
            SELECT course_id FROM enrollments
            WHERE student_id = ?
        )
        ORDER BY a.posted_at DESC
    """);
        ps.setInt(1, studentId);
        return ps.executeQuery();
    }

    public double getGPAForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT AVG(CASE
            WHEN score >= 90 THEN 4.0
            WHEN score >= 80 THEN 3.0
            WHEN score >= 70 THEN 2.0
            WHEN score >= 60 THEN 1.0
            ELSE 0.0
        END) as gpa
        FROM grades
        WHERE student_id = ?
    """);
        ps.setInt(1, studentId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            double gpa = rs.getDouble("gpa");
            return Math.round(gpa * 100.0) / 100.0;
        }
        return 0.0;
    }

    public ResultSet getAllStudents() throws SQLException {
        return getConnection().createStatement().executeQuery("""
        SELECT u.id as user_id, u.name, u.email,
               u.created_at,
               s.id as student_id, s.student_number,
               s.major, s.year
        FROM users u
        JOIN students s ON s.user_id = u.id
        ORDER BY u.name
    """);
    }

    public ResultSet getAllLecturers() throws SQLException {
        return getConnection().createStatement().executeQuery("""
        SELECT u.id as user_id, u.name, u.email,
               l.id as lecturer_id,
               l.department, l.title
        FROM users u
        JOIN lecturers l ON l.user_id = u.id
        ORDER BY u.name
    """);

    }
    // ── Course Registration ───────────────────────────────────────

    public ResultSet getRegistrationWindow(String semester)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT * FROM registration_windows
        WHERE semester = ? AND is_open = 1
        ORDER BY id DESC LIMIT 1
    """);
        ps.setString(1, semester);
        return ps.executeQuery();
    }

    public ResultSet getAvailableCoursesForRegistration(
            int studentId, String semester) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT c.id, c.code, c.name, c.credits,
               c.semester, c.description,
               u.name as lecturer_name,
               l.title as lecturer_title,
               CASE WHEN e.id IS NOT NULL
                    THEN 1 ELSE 0
               END as already_enrolled,
               CASE WHEN cr.id IS NOT NULL
                    THEN cr.status ELSE 'NOT REGISTERED'
               END as reg_status
        FROM courses c
        LEFT JOIN lecturers l ON l.id = c.lecturer_id
        LEFT JOIN users u ON u.id = l.user_id
        LEFT JOIN enrollments e
               ON e.course_id = c.id
              AND e.student_id = ?
        LEFT JOIN course_registrations cr
               ON cr.course_id = c.id
              AND cr.student_id = ?
              AND cr.semester = ?
        ORDER BY c.code
    """);
        ps.setInt(1, studentId);
        ps.setInt(2, studentId);
        ps.setString(3, semester);
        return ps.executeQuery();
    }

    public void registerCourse(int studentId, int courseId,
                               String semester) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO course_registrations
            (student_id, course_id, semester, status)
        VALUES (?, ?, ?, 'APPROVED')
        ON CONFLICT(student_id, course_id, semester)
        DO UPDATE SET status = 'APPROVED'
    """);
        ps.setInt(1, studentId);
        ps.setInt(2, courseId);
        ps.setString(3, semester);
        ps.executeUpdate();

        // also add to enrollments so it shows up in dashboard
        PreparedStatement ep = getConnection().prepareStatement("""
        INSERT OR IGNORE INTO enrollments
            (student_id, course_id)
        VALUES (?, ?)
    """);
        ep.setInt(1, studentId);
        ep.setInt(2, courseId);
        ep.executeUpdate();
    }

    public void dropCourse(int studentId, int courseId,
                           String semester) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        UPDATE course_registrations
        SET status = 'DROPPED'
        WHERE student_id = ? AND course_id = ?
        AND semester = ?
    """);
        ps.setInt(1, studentId);
        ps.setInt(2, courseId);
        ps.setString(3, semester);
        ps.executeUpdate();

        // remove from enrollments
        PreparedStatement ep = getConnection().prepareStatement("""
        DELETE FROM enrollments
        WHERE student_id = ? AND course_id = ?
    """);
        ep.setInt(1, studentId);
        ep.setInt(2, courseId);
        ep.executeUpdate();
    }

    public ResultSet getRegisteredCourses(int studentId,
                                          String semester) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT c.id, c.code, c.name, c.credits,
               cr.status, cr.registered_at,
               u.name as lecturer_name,
               l.title as lecturer_title
        FROM course_registrations cr
        JOIN courses c ON c.id = cr.course_id
        LEFT JOIN lecturers l ON l.id = c.lecturer_id
        LEFT JOIN users u ON u.id = l.user_id
        WHERE cr.student_id = ?
        AND cr.semester = ?
        ORDER BY c.code
    """);
        ps.setInt(1, studentId);
        ps.setString(2, semester);
        return ps.executeQuery();
    }

// ── Fees ──────────────────────────────────────────────────────

    public ResultSet getFeesForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT f.id, f.semester, f.description,
               f.amount, f.paid, f.due_date,
               (f.amount - f.paid) as balance,
               CASE WHEN f.paid >= f.amount
                    THEN 'PAID'
                    WHEN f.paid > 0
                    THEN 'PARTIAL'
                    ELSE 'UNPAID'
               END as status
        FROM fees f
        WHERE f.student_id = ?
        ORDER BY f.semester, f.due_date
    """);
        ps.setInt(1, studentId);
        return ps.executeQuery();
    }

    public ResultSet getPaymentHistoryForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT fp.amount, fp.paid_at, fp.method,
               fp.reference, f.description, f.semester
        FROM fee_payments fp
        JOIN fees f ON f.id = fp.fee_id
        WHERE f.student_id = ?
        ORDER BY fp.paid_at DESC
    """);
        ps.setInt(1, studentId);
        return ps.executeQuery();
    }

    public void recordPayment(int feeId, double amount,
                              String method, String reference) throws SQLException {
        // insert payment record
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO fee_payments
            (fee_id, amount, method, reference)
        VALUES (?, ?, ?, ?)
    """);
        ps.setInt(1,    feeId);
        ps.setDouble(2, amount);
        ps.setString(3, method);
        ps.setString(4, reference);
        ps.executeUpdate();

        // update paid amount on fee
        PreparedStatement up = getConnection().prepareStatement("""
        UPDATE fees
        SET paid = MIN(amount, paid + ?)
        WHERE id = ?
    """);
        up.setDouble(1, amount);
        up.setInt(2,    feeId);
        up.executeUpdate();
    }

    public double getTotalOutstandingFees(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT COALESCE(SUM(amount - paid), 0) as outstanding
        FROM fees
        WHERE student_id = ? AND paid < amount
    """);
        ps.setInt(1, studentId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getDouble("outstanding") : 0.0;
    }

// ── Exams ─────────────────────────────────────────────────────

    public ResultSet getExamsForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT e.id, c.code, c.name,
               e.exam_date, e.start_time, e.end_time,
               e.venue, e.type, e.semester
        FROM exams e
        JOIN courses c ON c.id = e.course_id
        JOIN enrollments en ON en.course_id = c.id
                           AND en.student_id = ?
        ORDER BY e.exam_date ASC, e.start_time ASC
    """);
        ps.setInt(1, studentId);
        return ps.executeQuery();
    }

    public ResultSet getAllExams() throws SQLException {
        return getConnection().createStatement().executeQuery("""
        SELECT e.id, c.code, c.name,
               e.exam_date, e.start_time, e.end_time,
               e.venue, e.type, e.semester
        FROM exams e
        JOIN courses c ON c.id = e.course_id
        ORDER BY e.exam_date ASC, e.start_time ASC
    """);
    }

    public void addExam(int courseId, String examDate,
                        String startTime, String endTime, String venue,
                        String semester, String type) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO exams
            (course_id, exam_date, start_time,
             end_time, venue, semester, type)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """);
        ps.setInt(1,    courseId);
        ps.setString(2, examDate);
        ps.setString(3, startTime);
        ps.setString(4, endTime);
        ps.setString(5, venue);
        ps.setString(6, semester);
        ps.setString(7, type);
        ps.executeUpdate();
    }

    public void deleteExam(int examId) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement(
                "DELETE FROM exams WHERE id = ?");
        ps.setInt(1, examId);
        ps.executeUpdate();
    }

    public void openCloseRegistration(String semester,
                                      boolean open) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        UPDATE registration_windows
        SET is_open = ?
        WHERE semester = ?
    """);
        ps.setInt(1,    open ? 1 : 0);
        ps.setString(2, semester);
        ps.executeUpdate();
    }

    /**
     * Change a user's password securely.
     */
    public boolean changePassword(int userId,
                                  String currentPassword, String newPassword)
            throws SQLException {
        PreparedStatement check = getConnection().prepareStatement(
                "SELECT password FROM users WHERE id = ?");
        check.setInt(1, userId);
        ResultSet rs = check.executeQuery();
        if (!rs.next()) return false;

        String hashed = rs.getString("password");
        if (!BCrypt.checkpw(currentPassword, hashed))
            return false;

        PreparedStatement update = getConnection().prepareStatement(
                "UPDATE users SET password = ? WHERE id = ?");
        update.setString(1,
                BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        update.setInt(2, userId);
        update.executeUpdate();
        return true;
    }

    /**
     * Update student profile details.
     */
    public void updateStudentProfile(int studentId,
                                     String major, int year, String profileImage)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        UPDATE students
        SET major = ?, year = ?, profile_image = ?
        WHERE id = ?
    """);
        ps.setString(1, major);
        ps.setInt(2,    year);
        ps.setString(3, profileImage);
        ps.setInt(4,    studentId);
        ps.executeUpdate();
    }

    /**
     * Update user name and email.
     */
    public void updateUserInfo(int userId,
                               String name, String email)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement(
                "UPDATE users SET name = ?, email = ? WHERE id = ?");
        ps.setString(1, name);
        ps.setString(2, email.trim().toLowerCase());
        ps.setInt(3,    userId);
        ps.executeUpdate();
    }

    /**
     * Get full transcript for a student.
     */
    public ResultSet getTranscriptForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT c.code, c.name, c.credits,
               g.score, g.letter_grade, g.semester,
               g.entered_at,
               CASE
                   WHEN g.score >= 90 THEN 4.0
                   WHEN g.score >= 80 THEN 3.0
                   WHEN g.score >= 70 THEN 2.0
                   WHEN g.score >= 60 THEN 1.0
                   ELSE 0.0
               END as grade_points,
               u.name as lecturer_name,
               l.title as lecturer_title
        FROM grades g
        JOIN courses c ON c.id = g.course_id
        LEFT JOIN lecturers l ON l.id = g.entered_by
        LEFT JOIN users u ON u.id = l.user_id
        WHERE g.student_id = ?
        ORDER BY g.semester, c.code
    """);
        ps.setInt(1, studentId);
        return ps.executeQuery();
    }

    /**
     * Get semester GPA for a student.
     */
    public ResultSet getSemesterGPAForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT semester,
               ROUND(AVG(CASE
                   WHEN score >= 90 THEN 4.0
                   WHEN score >= 80 THEN 3.0
                   WHEN score >= 70 THEN 2.0
                   WHEN score >= 60 THEN 1.0
                   ELSE 0.0
               END), 2) as semester_gpa,
               COUNT(*) as courses_taken,
               SUM(CASE
                   WHEN score >= 90 THEN 4.0
                   WHEN score >= 80 THEN 3.0
                   WHEN score >= 70 THEN 2.0
                   WHEN score >= 60 THEN 1.0
                   ELSE 0.0
               END) as total_points
        FROM grades
        WHERE student_id = ?
        GROUP BY semester
        ORDER BY semester
    """);
        ps.setInt(1, studentId);
        return ps.executeQuery();
    }

    public void resetDatabase() throws SQLException {
        String[] tables = {
                "announcements", "grades", "quiz_results",
                "quizzes", "submissions", "assignments",
                "enrollments", "courses", "lecturers",
                "students", "users"
        };
        for (String table : tables)
            getConnection().createStatement()
                    .execute("DELETE FROM " + table);
        seedData();
        System.out.println("✔ Database reset and reseeded.");
    }
    // ── Results Approval ──────────────────────────────────────────

    /**
     * Lecturer submits grades for a course for admin approval.
     */
    public void submitGradesForApproval(int courseId,
                                        int lecturerId, String semester) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO grade_submissions
            (course_id, lecturer_id, semester, status)
        VALUES (?, ?, ?, 'PENDING')
        ON CONFLICT DO NOTHING
    """);
        ps.setInt(1,    courseId);
        ps.setInt(2,    lecturerId);
        ps.setString(3, semester);
        ps.executeUpdate();
    }

    public ResultSet getPendingGradeSubmissions()
            throws SQLException {
        return getConnection().createStatement().executeQuery("""
        SELECT gs.id, gs.semester, gs.submitted_at,
               gs.status, gs.remarks,
               c.code, c.name as course_name,
               u.name as lecturer_name,
               l.title as lecturer_title,
               COUNT(g.id) as grades_count
        FROM grade_submissions gs
        JOIN courses c ON c.id = gs.course_id
        JOIN lecturers l ON l.id = gs.lecturer_id
        JOIN users u ON u.id = l.user_id
        LEFT JOIN grades g ON g.course_id = gs.course_id
        GROUP BY gs.id
        ORDER BY gs.submitted_at DESC
    """);
    }

    public void approveGradeSubmission(int submissionId,
                                       int adminUserId) throws SQLException {
        // approve submission
        PreparedStatement ps = getConnection().prepareStatement("""
        UPDATE grade_submissions
        SET status      = 'APPROVED',
            approved_at = date('now'),
            approved_by = ?
        WHERE id = ?
    """);
        ps.setInt(1, adminUserId);
        ps.setInt(2, submissionId);
        ps.executeUpdate();

        // release grades for that course
        PreparedStatement rs = getConnection().prepareStatement("""
        UPDATE grades SET released = 1
        WHERE course_id = (
            SELECT course_id FROM grade_submissions
            WHERE id = ?
        )
    """);
        rs.setInt(1, submissionId);
        rs.executeUpdate();
    }

    public void rejectGradeSubmission(int submissionId,
                                      String remarks) throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        UPDATE grade_submissions
        SET status  = 'REJECTED',
            remarks = ?
        WHERE id = ?
    """);
        ps.setString(1, remarks);
        ps.setInt(2,    submissionId);
        ps.executeUpdate();
    }

    /**
     * Returns only RELEASED grades for a student.
     * Unreleased grades are hidden until admin approves.
     */
    public ResultSet getReleasedGradesForStudent(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT g.id, c.code, c.name, g.score,
               g.letter_grade, g.semester, g.entered_at,
               g.released
        FROM grades g
        JOIN courses c ON c.id = g.course_id
        WHERE g.student_id = ?
        AND g.released = 1
        ORDER BY c.code
    """);
        ps.setInt(1, studentId);
        return ps.executeQuery();
    }

    public boolean hasReleasedGrades(int studentId)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        SELECT COUNT(*) as cnt FROM grades
        WHERE student_id = ? AND released = 1
    """);
        ps.setInt(1, studentId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt("cnt") > 0;
    }

// ── Fee Management (Admin) ────────────────────────────────────

    public ResultSet getAllFeesAdmin() throws SQLException {
        return getConnection().createStatement().executeQuery("""
        SELECT f.id, u.name as student_name,
               s.student_number, f.semester,
               f.description, f.amount, f.paid,
               (f.amount - f.paid) as balance,
               f.due_date,
               CASE WHEN f.paid >= f.amount THEN 'PAID'
                    WHEN f.paid > 0 THEN 'PARTIAL'
                    ELSE 'UNPAID'
               END as status
        FROM fees f
        JOIN students s ON s.id = f.student_id
        JOIN users u ON u.id = s.user_id
        ORDER BY status DESC, u.name
    """);
    }

    public void addFeeRecord(int studentId, String semester,
                             double amount, String description, String dueDate)
            throws SQLException {
        PreparedStatement ps = getConnection().prepareStatement("""
        INSERT INTO fees
            (student_id, semester, amount, paid,
             due_date, description)
        VALUES (?, ?, ?, 0, ?, ?)
    """);
        ps.setInt(1,    studentId);
        ps.setString(2, semester);
        ps.setDouble(3, amount);
        ps.setString(4, dueDate);
        ps.setString(5, description);
        ps.executeUpdate();
    }

    public void adminRecordPayment(int feeId, double amount,
                                   String method, String reference) throws SQLException {
        recordPayment(feeId, amount, method, reference);
    }

    public ResultSet getFeesSummaryAdmin() throws SQLException {
        return getConnection().createStatement().executeQuery("""
        SELECT
            COUNT(DISTINCT s.id) as total_students,
            SUM(f.amount) as total_billed,
            SUM(f.paid) as total_collected,
            SUM(f.amount - f.paid) as total_outstanding,
            COUNT(CASE WHEN f.paid >= f.amount
                  THEN 1 END) as fully_paid_count,
            COUNT(CASE WHEN f.paid = 0
                  THEN 1 END) as unpaid_count
        FROM fees f
        JOIN students s ON s.id = f.student_id
    """);
    }

// ── Backup & Restore ──────────────────────────────────────────

    /**
     * Exports the full database as a SQL dump string.
     */
    public String exportDatabaseSQL() throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("-- UCC Portal Database Backup\n");
        sb.append("-- Generated: ")
                .append(java.time.LocalDate.now()).append("\n\n");

        String[] tables = {
                "users", "students", "lecturers", "courses",
                "enrollments", "assignments", "submissions",
                "quizzes", "quiz_results", "grades",
                "grade_submissions", "fees", "fee_payments",
                "exams", "registration_windows",
                "course_registrations", "announcements"
        };

        for (String table : tables) {
            try {
                sb.append("-- Table: ").append(table)
                        .append("\n");
                ResultSet rs = getConnection()
                        .createStatement()
                        .executeQuery(
                                "SELECT * FROM " + table);
                java.sql.ResultSetMetaData meta =
                        rs.getMetaData();
                int cols = meta.getColumnCount();

                while (rs.next()) {
                    sb.append("INSERT OR IGNORE INTO ")
                            .append(table).append(" VALUES (");
                    for (int i = 1; i <= cols; i++) {
                        String val = rs.getString(i);
                        if (val == null)
                            sb.append("NULL");
                        else
                            sb.append("'")
                                    .append(val.replace("'", "''"))
                                    .append("'");
                        if (i < cols) sb.append(", ");
                    }
                    sb.append(");\n");
                }
                sb.append("\n");
            } catch (SQLException e) {
                sb.append("-- Skipped: ").append(table)
                        .append(" (").append(e.getMessage())
                        .append(")\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * Restores the database from a SQL dump string.
     */
    public void restoreDatabaseSQL(String sql)
            throws SQLException {
        String[] statements = sql.split(";");
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()
                    || trimmed.startsWith("--"))
                continue;
            try {
                getConnection().createStatement()
                        .execute(trimmed);
            } catch (SQLException e) {
                System.err.println("Restore warning: "
                        + e.getMessage());
            }
        }
        System.out.println("✔ Database restored.");
    }

    private String deriveLetterGrade(double score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    }
