package com.dashboard.storage;

import com.dashboard.model.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JsonDataManager {

    private static final String DATA_DIR         = "data/";
    private static final String STUDENTS_FILE    = DATA_DIR + "students.json";
    private static final String COURSES_FILE     = DATA_DIR + "courses.json";
    private static final String ASSIGNMENTS_FILE = DATA_DIR + "assignments.json";
    private static final String QUIZZES_FILE     = DATA_DIR + "quizzes.json";
    private static final String GRADES_FILE      = DATA_DIR + "grades.json";

    private final Gson gson;

    public JsonDataManager() {
        gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>)   (src, t, ctx) ->
                                new JsonPrimitive(src.toString()))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, t, ctx) ->
                                LocalDate.parse(json.getAsString()))
                .setPrettyPrinting()
                .create();
    }

    // ── Init ──────────────────────────────────────────────────────

    public void initializeDataFiles() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            if (!Files.exists(Paths.get(STUDENTS_FILE)))    seedStudents();
            if (!Files.exists(Paths.get(COURSES_FILE)))     seedCourses();
            if (!Files.exists(Paths.get(ASSIGNMENTS_FILE))) seedAssignments();
            if (!Files.exists(Paths.get(QUIZZES_FILE)))     seedQuizzes();
            if (!Files.exists(Paths.get(GRADES_FILE)))      seedGrades();
        } catch (IOException e) {
            System.err.println("Data init failed: " + e.getMessage());
        }
    }

    // ── Students ──────────────────────────────────────────────────

    public List<Student> loadStudents() {
        return loadList(STUDENTS_FILE,
                new TypeToken<List<Student>>(){}.getType());
    }

    public void saveStudents(List<Student> students) {
        saveList(STUDENTS_FILE, students);
    }

    public Student findStudentByEmail(String email) {
        return loadStudents().stream()
                .filter(s -> s.getEmail().equalsIgnoreCase(email.trim()))
                .findFirst().orElse(null);
    }

    // ── Courses ───────────────────────────────────────────────────

    public List<Course> loadCourses() {
        return loadList(COURSES_FILE,
                new TypeToken<List<Course>>(){}.getType());
    }

    public List<Course> getCoursesForStudent(int studentId) {
        return loadStudents().stream()
                .filter(s -> s.getId() == studentId)
                .findFirst()
                .map(Student::getCourses)
                .orElse(new ArrayList<>());
    }

    // ── Assignments ───────────────────────────────────────────────

    public List<Assignment> loadAssignments() {
        return loadList(ASSIGNMENTS_FILE,
                new TypeToken<List<Assignment>>(){}.getType());
    }

    public void saveAssignments(List<Assignment> list) {
        saveList(ASSIGNMENTS_FILE, list);
    }

    public List<Assignment> getAssignmentsForStudent(int studentId) {
        return loadAssignments().stream()
                .filter(a -> a.getStudentId() == studentId)
                .collect(Collectors.toList());
    }

    public void markAssignmentSubmitted(int assignmentId) {
        List<Assignment> all = loadAssignments();
        all.stream()
                .filter(a -> a.getId() == assignmentId)
                .findFirst()
                .ifPresent(a -> a.setSubmitted(true));
        saveAssignments(all);
    }

    // ── Quizzes ───────────────────────────────────────────────────

    public List<Quiz> loadQuizzes() {
        return loadList(QUIZZES_FILE,
                new TypeToken<List<Quiz>>(){}.getType());
    }

    public void saveQuizzes(List<Quiz> list) {
        saveList(QUIZZES_FILE, list);
    }

    public List<Quiz> getQuizzesForStudent(int studentId) {
        return loadQuizzes().stream()
                .filter(q -> q.getStudentId() == studentId)
                .collect(Collectors.toList());
    }

    public void markQuizCompleted(int quizId, double score) {
        List<Quiz> all = loadQuizzes();
        all.stream()
                .filter(q -> q.getId() == quizId)
                .findFirst()
                .ifPresent(q -> {
                    q.setCompleted(true);
                    q.setScore(score);
                });
        saveQuizzes(all);
    }

    // ── Grades ────────────────────────────────────────────────────

    public List<Grade> loadGrades() {
        return loadList(GRADES_FILE,
                new TypeToken<List<Grade>>(){}.getType());
    }

    public void saveGrades(List<Grade> list) {
        saveList(GRADES_FILE, list);
    }

    public List<Grade> getGradesForStudent(int studentId) {
        return loadStudents().stream()
                .filter(s -> s.getId() == studentId)
                .findFirst()
                .map(Student::getGrades)
                .orElse(new ArrayList<>());
    }

    // ── Generic IO ────────────────────────────────────────────────

    private <T> List<T> loadList(String path, Type type) {
        try (Reader r = new FileReader(path)) {
            List<T> list = gson.fromJson(r, type);
            return list != null ? list : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Read error " + path + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private <T> void saveList(String path, List<T> list) {
        try (Writer w = new FileWriter(path)) {
            gson.toJson(list, w);
        } catch (IOException e) {
            System.err.println("Write error " + path + ": " + e.getMessage());
        }
    }

    // ── Seed Data ─────────────────────────────────────────────────

    private void seedStudents() {
        Course csc302 = new Course("CSC302", "Prog. & Problem Analysis II (Java)", 3,
                "Mr. Elliot Attipoe", "TBA", "2025/2026 Second Semester");
        Course csc314 = new Course("CSC314", "Network Computing I", 3,
                "Dr. Emmanuel Tetteh", "TBA", "2025/2026 Second Semester");
        Course csc399 = new Course("CSC399", "Research Methods", 3,
                "Mr. Sandro Amofa", "TBA", "2025/2026 Second Semester");
        Course mat302 = new Course("MAT302", "Advanced Calculus II", 3,
                "TBA", "TBA", "2025/2026 Second Semester");
        Course csc312 = new Course("CSC312", "Human Computer Interface", 3,
                "Mr. Benard Buckman", "TBA", "2025/2026 Second Semester");
        Course csc316 = new Course("CSC316", "Web Technologies", 3,
                "Mrs. Alimatu-Saadia Yussiff", "TBA", "2025/2026 Second Semester");
        Course ent302 = new Course("ENT302", "Introduction to Entrepreneurship", 2,
                "Dr. Richard Adu Agyapong", "TBA", "2025/2026 Second Semester");

        Student s1 = new Student(1001, "Paul Kissi",
                "paul.kissi@stu.ucc.edu.gh", "pass1234",
                "Computer Science", 3);
        s1.setProfileImagePath("/images/paul.jpeg");
        s1.addCourse(csc302);
        s1.addCourse(csc314);
        s1.addCourse(csc399);
        s1.addCourse(mat302);
        s1.addCourse(csc312);
        s1.addCourse(csc316);
        s1.addCourse(ent302);
        s1.addGrade(new Grade(csc302, 92.0, "2025/2026 Second Semester"));
        s1.addGrade(new Grade(csc314, 91.0, "2025/2026 Second Semester"));
        s1.addGrade(new Grade(csc399, 90.0, "2025/2026 Second Semester"));
        s1.addGrade(new Grade(mat302, 83.0, "2025/2026 Second Semester"));
        s1.addGrade(new Grade(csc312, 94.0, "2025/2026 Second Semester"));
        s1.addGrade(new Grade(csc316, 97.0, "2025/2026 Second Semester"));
        s1.addGrade(new Grade(ent302, 92.0, "2025/2026 Second Semester"));

        Course phys101 = new Course("PHYS101", "Physics I", 4,
                "Dr. Williams", "Mon/Wed 13:00", "Fall 2025");
        Course math101 = new Course("MATH101", "Calculus I", 4,
                "Dr. Smith", "Mon/Wed 10:00", "Fall 2025");

        Student s2 = new Student(1002, "Brenda Mensah",
                "brenda@stu.ucc.edu.gh", "pass1234", "Physics", 3);
        s2.addCourse(phys101);
        s2.addCourse(math101);
        s2.addGrade(new Grade(phys101, 78.0, "Fall 2025"));
        s2.addGrade(new Grade(math101, 85.5, "Fall 2025"));

        saveList(STUDENTS_FILE, List.of(s1, s2));
        System.out.println("✔ Seeded students.json");
    }

    private void seedCourses() {
        saveList(COURSES_FILE, List.of(
                new Course("CSC302", "Prog. & Problem Analysis II (Java)", 3,
                        "Mr. Elliot Attipoe", "TBA", "2025/2026 Second Semester"),
                new Course("CSC314", "Network Computing I", 3,
                        "Dr. Emmanuel Tetteh", "TBA", "2025/2026 Second Semester"),
                new Course("CSC399", "Research Methods", 3,
                        "Mr. Sandro Amofa", "TBA", "2025/2026 Second Semester"),
                new Course("MAT302", "Advanced Calculus II", 3,
                        "TBA", "TBA", "2025/2026 Second Semester"),
                new Course("CSC312", "Human Computer Interface", 3,
                        "Mr. Benard Buckman", "TBA", "2025/2026 Second Semester"),
                new Course("CSC316", "Web Technologies", 3,
                        "Mrs. Alimatu-Saadia Yussiff", "TBA", "2025/2026 Second Semester"),
                new Course("ENT302", "Introduction to Entrepreneurship", 2,
                        "Dr. Richard Adu Agyapong", "TBA", "2025/2026 Second Semester"),
                new Course("PHYS101", "Physics I", 4,
                        "Dr. Williams", "Mon/Wed 13:00", "Fall 2025"),
                new Course("MATH101", "Calculus I", 4,
                        "Dr. Smith", "Mon/Wed 10:00", "Fall 2025")
        ));
        System.out.println("✔ Seeded courses.json");
    }

    private void seedAssignments() {
        LocalDate today = LocalDate.now();
        List<Assignment> list = new ArrayList<>();

        // ── CSC302 — Prog. & Problem Analysis II (Java) ───────────
        Assignment a1 = new Assignment(1, 1001, "CSC302",
                "Java Lab 1 - OOP Basics",
                "Implement a class hierarchy in Java",
                today.minusDays(20), "HIGH");
        a1.setSubmitted(true);
        a1.setScore(93.0);
        list.add(a1);

        Assignment a2 = new Assignment(2, 1001, "CSC302",
                "Java Lab 2 - Exception Handling",
                "Handle runtime exceptions in a banking app",
                today.minusDays(10), "HIGH");
        a2.setSubmitted(true);
        a2.setScore(91.0);
        list.add(a2);

        Assignment a3 = new Assignment(3, 1001, "CSC302",
                "Java Lab 3 - Collections",
                "Use ArrayList and HashMap in a student registry",
                today.plusDays(5), "HIGH");
        list.add(a3);

        // ── CSC314 — Network Computing I ──────────────────────────
        Assignment a4 = new Assignment(4, 1001, "CSC314",
                "Network Lab 1 - OSI Model",
                "Map a real-world scenario to OSI layers",
                today.minusDays(18), "MEDIUM");
        a4.setSubmitted(true);
        a4.setScore(92.0);
        list.add(a4);

        Assignment a5 = new Assignment(5, 1001, "CSC314",
                "Network Lab 2 - IP Addressing",
                "Subnetting exercises for a campus network",
                today.minusDays(7), "HIGH");
        a5.setSubmitted(true);
        a5.setScore(90.0);
        list.add(a5);

        Assignment a6 = new Assignment(6, 1001, "CSC314",
                "Network Lab 3 - LAN Setup",
                "Configure a basic LAN using Cisco Packet Tracer",
                today.plusDays(6), "HIGH");
        list.add(a6);

        // ── CSC399 — Research Methods ──────────────────────────────
        Assignment a7 = new Assignment(7, 1001, "CSC399",
                "Research Proposal",
                "Write a 2-page research proposal on AI in healthcare",
                today.minusDays(15), "HIGH");
        a7.setSubmitted(true);
        a7.setScore(90.0);
        list.add(a7);

        Assignment a8 = new Assignment(8, 1001, "CSC399",
                "Literature Review",
                "Review 5 journal articles related to your research topic",
                today.minusDays(5), "HIGH");
        a8.setSubmitted(true);
        a8.setScore(91.0);
        list.add(a8);

        Assignment a9 = new Assignment(9, 1001, "CSC399",
                "Methodology Draft",
                "Describe your research design and data collection method",
                today.plusDays(8), "MEDIUM");
        list.add(a9);

        // ── MAT302 — Advanced Calculus II ─────────────────────────
        Assignment a10 = new Assignment(10, 1001, "MAT302",
                "Problem Set 1 - Sequences",
                "Chapter 3 exercises 1-20",
                today.minusDays(22), "MEDIUM");
        a10.setSubmitted(true);
        a10.setScore(82.0);
        list.add(a10);

        Assignment a11 = new Assignment(11, 1001, "MAT302",
                "Problem Set 2 - Integration",
                "Evaluate definite and indefinite integrals",
                today.minusDays(8), "HIGH");
        a11.setSubmitted(true);
        a11.setScore(84.0);
        list.add(a11);

        Assignment a12 = new Assignment(12, 1001, "MAT302",
                "Problem Set 3 - Series",
                "Convergence tests for infinite series",
                today.plusDays(4), "HIGH");
        list.add(a12);

        // ── CSC312 — Human Computer Interface ─────────────────────
        Assignment a13 = new Assignment(13, 1001, "CSC312",
                "HCI Report 1 - Heuristic Evaluation",
                "Evaluate a mobile app using Nielsen's 10 heuristics",
                today.minusDays(16), "MEDIUM");
        a13.setSubmitted(true);
        a13.setScore(95.0);
        list.add(a13);

        Assignment a14 = new Assignment(14, 1001, "CSC312",
                "HCI Report 2 - User Personas",
                "Create 3 user personas for a university portal",
                today.minusDays(6), "MEDIUM");
        a14.setSubmitted(true);
        a14.setScore(93.0);
        list.add(a14);

        Assignment a15 = new Assignment(15, 1001, "CSC312",
                "HCI Prototype",
                "Build a low-fidelity prototype of your redesigned app",
                today.plusDays(7), "HIGH");
        list.add(a15);

        // ── CSC316 — Web Technologies ──────────────────────────────
        Assignment a16 = new Assignment(16, 1001, "CSC316",
                "Web Lab 1 - HTML/CSS",
                "Build a responsive university landing page",
                today.minusDays(19), "MEDIUM");
        a16.setSubmitted(true);
        a16.setScore(97.0);
        list.add(a16);

        Assignment a17 = new Assignment(17, 1001, "CSC316",
                "Web Lab 2 - JavaScript",
                "Add interactivity to your landing page using JS",
                today.minusDays(9), "HIGH");
        a17.setSubmitted(true);
        a17.setScore(96.0);
        list.add(a17);

        Assignment a18 = new Assignment(18, 1001, "CSC316",
                "Web Lab 3 - React Basics",
                "Build a simple React component for a student list",
                today.plusDays(6), "HIGH");
        list.add(a18);

        // ── ENT302 — Introduction to Entrepreneurship ──────────────
        Assignment a19 = new Assignment(19, 1001, "ENT302",
                "Business Idea Pitch",
                "Present a 1-page summary of a tech startup idea",
                today.minusDays(14), "LOW");
        a19.setSubmitted(true);
        a19.setScore(92.0);
        list.add(a19);

        Assignment a20 = new Assignment(20, 1001, "ENT302",
                "Business Plan Draft",
                "Outline market analysis and revenue model",
                today.minusDays(4), "MEDIUM");
        a20.setSubmitted(true);
        a20.setScore(93.0);
        list.add(a20);

        Assignment a21 = new Assignment(21, 1001, "ENT302",
                "Final Business Plan",
                "Full business plan with financial projections",
                today.plusDays(10), "HIGH");
        list.add(a21);

        // ── Brenda ─────────────────────────────────────────────────
        Assignment a22 = new Assignment(22, 1002, "PHYS101",
                "Lab Report 1", "Motion experiment write-up",
                today.plusDays(4), "HIGH");
        list.add(a22);

        saveList(ASSIGNMENTS_FILE, list);
        System.out.println("✔ Seeded assignments.json");
    }

    private void seedQuizzes() {
        LocalDate today = LocalDate.now();
        List<Quiz> list = new ArrayList<>();

        // ── CSC302 ────────────────────────────────────────────────
        Quiz q1 = new Quiz(1, 1001, "CSC302", "Java Quiz 1 - OOP Concepts",
                today.minusDays(18), 10, 20);
        q1.setCompleted(true); q1.setScore(92.0);
        list.add(q1);

        Quiz q2 = new Quiz(2, 1001, "CSC302", "Java Quiz 2 - Inheritance",
                today.minusDays(8), 10, 20);
        q2.setCompleted(true); q2.setScore(93.0);
        list.add(q2);

        Quiz q3 = new Quiz(3, 1001, "CSC302", "Java Quiz 3 - Collections",
                today.plusDays(5), 10, 20);
        list.add(q3);

        // ── CSC314 ────────────────────────────────────────────────
        Quiz q4 = new Quiz(4, 1001, "CSC314", "Networking Quiz 1 - OSI Model",
                today.minusDays(17), 12, 25);
        q4.setCompleted(true); q4.setScore(91.0);
        list.add(q4);

        Quiz q5 = new Quiz(5, 1001, "CSC314", "Networking Quiz 2 - TCP/IP",
                today.minusDays(6), 12, 25);
        q5.setCompleted(true); q5.setScore(90.0);
        list.add(q5);

        Quiz q6 = new Quiz(6, 1001, "CSC314", "Networking Quiz 3 - Subnetting",
                today.plusDays(6), 12, 25);
        list.add(q6);

        // ── CSC399 ────────────────────────────────────────────────
        Quiz q7 = new Quiz(7, 1001, "CSC399", "Research Quiz 1 - Research Types",
                today.minusDays(14), 10, 15);
        q7.setCompleted(true); q7.setScore(90.0);
        list.add(q7);

        Quiz q8 = new Quiz(8, 1001, "CSC399", "Research Quiz 2 - Data Collection",
                today.plusDays(8), 10, 15);
        list.add(q8);

        // ── MAT302 ────────────────────────────────────────────────
        Quiz q9 = new Quiz(9, 1001, "MAT302", "Calculus Quiz 1 - Limits",
                today.minusDays(20), 15, 30);
        q9.setCompleted(true); q9.setScore(82.0);
        list.add(q9);

        Quiz q10 = new Quiz(10, 1001, "MAT302", "Calculus Quiz 2 - Derivatives",
                today.minusDays(9), 15, 30);
        q10.setCompleted(true); q10.setScore(84.0);
        list.add(q10);

        Quiz q11 = new Quiz(11, 1001, "MAT302", "Calculus Quiz 3 - Integration",
                today.plusDays(4), 15, 30);
        list.add(q11);

        // ── CSC312 ────────────────────────────────────────────────
        Quiz q12 = new Quiz(12, 1001, "CSC312", "HCI Quiz 1 - Design Principles",
                today.minusDays(15), 10, 15);
        q12.setCompleted(true); q12.setScore(94.0);
        list.add(q12);

        Quiz q13 = new Quiz(13, 1001, "CSC312", "HCI Quiz 2 - Usability Testing",
                today.plusDays(7), 10, 15);
        list.add(q13);

        // ── CSC316 ────────────────────────────────────────────────
        Quiz q14 = new Quiz(14, 1001, "CSC316", "Web Quiz 1 - HTML/CSS",
                today.minusDays(18), 10, 20);
        q14.setCompleted(true); q14.setScore(97.0);
        list.add(q14);

        Quiz q15 = new Quiz(15, 1001, "CSC316", "Web Quiz 2 - JavaScript DOM",
                today.minusDays(7), 10, 20);
        q15.setCompleted(true); q15.setScore(96.0);
        list.add(q15);

        Quiz q16 = new Quiz(16, 1001, "CSC316", "Web Quiz 3 - React",
                today.plusDays(5), 10, 20);
        list.add(q16);

        // ── ENT302 ────────────────────────────────────────────────
        Quiz q17 = new Quiz(17, 1001, "ENT302",
                "Entrepreneurship Quiz 1 - Concepts",
                today.minusDays(13), 10, 15);
        q17.setCompleted(true); q17.setScore(92.0);
        list.add(q17);

        Quiz q18 = new Quiz(18, 1001, "ENT302",
                "Entrepreneurship Quiz 2 - Business Models",
                today.plusDays(9), 10, 15);
        list.add(q18);

        // ── Brenda ────────────────────────────────────────────────
        Quiz q19 = new Quiz(19, 1002, "PHYS101", "Quiz 1 - Kinematics",
                today.plusDays(3), 12, 25);
        list.add(q19);

        saveList(QUIZZES_FILE, list);
        System.out.println("✔ Seeded quizzes.json");
    }

    private void seedGrades() {
        List<Grade> grades = new ArrayList<>();
        loadStudents().forEach(s -> grades.addAll(s.getGrades()));
        saveList(GRADES_FILE, grades);
        System.out.println("✔ Seeded grades.json");
    }
}