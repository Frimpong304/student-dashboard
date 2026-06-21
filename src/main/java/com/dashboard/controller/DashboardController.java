package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.model.*;
import com.dashboard.util.SessionManager;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    // Sidebar
    @FXML private Circle profileCircle;
    @FXML private Label  studentNameLabel;
    @FXML private Label  studentMajorLabel;
    @FXML private Label  gpaLabel;

    @FXML private Button navHome;
    @FXML private Button navCourses;
    @FXML private Button navAssignments;
    @FXML private Button navQuizzes;
    @FXML private Button navGrades;
    @FXML private Button navCalendar;
    @FXML private Button navNotifications;
    @FXML private Button navProfile;
    @FXML private Button navTranscript;
    @FXML private Button navExport;
    @FXML private Button navRegistration;
    @FXML private Button navFees;
    @FXML private Button navTimetable;

    // Top bar
    @FXML private Label     pageTitle;
    @FXML private TextField searchField;
    @FXML private Label     notificationBadge;

    // Welcome banner
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label semesterLabel;
    @FXML private Label yearLabel;

    // Stat cards
    @FXML private Label gpaStatLabel;
    @FXML private Label coursesStatLabel;
    @FXML private Label pendingStatLabel;
    @FXML private Label quizzesStatLabel;

    // Content
    @FXML private VBox             contentArea;
    @FXML private VBox             courseProgressContainer;
    @FXML private ListView<String> upcomingList;
    @FXML private ListView<String> notificationsList;
    @FXML private BarChart<String, Number> gradeChart;

    // Status bar
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;

    private Student         currentStudent;
    private Button          activeNavButton;
    private DatabaseManager db;

    // ── Initialization ────────────────────────────────────────────

    @FXML
    public void initialize() {
        db = DatabaseManager.getInstance();
        loadStudentFromDatabase();

        if (currentStudent == null) {
            updateStatus("Error: could not load student data.");
            return;
        }

        setupSidebar();
        setupWelcomeBanner();
        setupStatCards();
        setupCourseProgress();
        setupUpcomingDeadlines();
        setupNotifications();
        setupGradeChart();
        setupSearch();
        setActiveNav(navHome);
        updateStatus("Welcome back, "
                + currentStudent.getFirstName() + "!");
    }
    @FXML
    private void handleNavRegistration() {
        loadView("Registration.fxml",
                "Course Registration",
                navHome); // replace with navRegistration if you create one
    }

    @FXML
    private void handleNavFees() {
        loadView("Fees.fxml",
                "Fee Payment",
                navHome); // replace with navFees if you create one
    }

    @FXML
    private void handleNavTimetable() {
        loadView("Timetable.fxml",
                "Exam Timetable",
                navHome); // replace with navTimetable if you create one
    }
    // add to fields
    @FXML private Button darkModeBtn;
    private boolean darkMode = false;

    // add handler
    @FXML
    private void handleDarkMode() {
        darkMode = !darkMode;
        javafx.scene.Scene scene =
                darkModeBtn.getScene();
        if (darkMode) {
            scene.getRoot().getStyleClass()
                    .add("dark-mode");
            darkModeBtn.setText("☀️");
        } else {
            scene.getRoot().getStyleClass()
                    .remove("dark-mode");
            darkModeBtn.setText("🌙");
        }
    }
    // ── Load Student from DB ──────────────────────────────────────

    private void loadStudentFromDatabase() {
        try {
            String email = SessionManager.getInstance().getEmail();
            ResultSet rs = db.getStudentByEmail(email);

            if (rs == null || !rs.next()) {
                updateStatus("Error: student record not found.");
                return;
            }

            currentStudent = new Student();
            currentStudent.setId(rs.getInt("student_id"));
            currentStudent.setName(rs.getString("name"));
            currentStudent.setEmail(rs.getString("email"));
            currentStudent.setMajor(rs.getString("major"));
            currentStudent.setYear(rs.getInt("year"));
            currentStudent.setProfileImagePath(
                    rs.getString("profile_image"));

            SessionManager.getInstance()
                    .setRoleSpecificId(rs.getInt("student_id"));

            // load courses
            ResultSet courses = db.getCoursesForStudent(
                    currentStudent.getId());
            while (courses.next()) {
                Course c = new Course();
                c.setCourseCode(courses.getString("code"));
                c.setCourseName(courses.getString("name"));
                c.setCredits(courses.getInt("credits"));
                String title    = courses.getString("lecturer_title");
                String lecName  = courses.getString("lecturer_name");
                c.setInstructor(
                        (title != null ? title + " " : "") + lecName);
                c.setSemester(courses.getString("semester"));
                c.setDescription(courses.getString("description"));
                currentStudent.addCourse(c);
            }

            // load grades
            ResultSet grades = db.getGradesForStudent(
                    currentStudent.getId());
            while (grades.next()) {
                Course stub = new Course();
                stub.setCourseCode(grades.getString("code"));
                stub.setCourseName(grades.getString("name"));
                Grade g = new Grade(stub,
                        grades.getDouble("score"),
                        grades.getString("semester"));
                currentStudent.addGrade(g);
            }

        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error loading student data.");
        }
    }

    // ── Sidebar ───────────────────────────────────────────────────

    private void setupSidebar() {
        studentNameLabel.setText(currentStudent.getName());
        studentMajorLabel.setText(currentStudent.getMajor());
        gpaLabel.setText(String.format("GPA: %.2f",
                currentStudent.getGPA()));
    }

    // ── Welcome Banner ────────────────────────────────────────────

    private void setupWelcomeBanner() {
        welcomeLabel.setText("Welcome back, "
                + currentStudent.getFirstName() + "! ");
        dateLabel.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, MMMM dd yyyy")));
        yearLabel.setText(currentStudent.getYearLabel());
        semesterLabel.setText("2025/2026 Second Semester");
    }

    // ── Stat Cards ────────────────────────────────────────────────

    private void setupStatCards() {
        gpaStatLabel.setText(
                String.format("%.2f", currentStudent.getGPA()));
        coursesStatLabel.setText(
                String.valueOf(currentStudent.getCourses().size()));

        try {
            // count pending assignments from DB
            long pending = 0;
            long quizzesDue = 0;

            for (Course course : currentStudent.getCourses()) {
                ResultSet as = db.getAssignmentsForCourse(
                        getCourseId(course.getCourseCode()),
                        currentStudent.getId());
                while (as.next()) {
                    if (as.getInt("submitted") == 0) pending++;
                }

                ResultSet qs = db.getQuizzesForCourse(
                        getCourseId(course.getCourseCode()),
                        currentStudent.getId());
                while (qs.next()) {
                    if (qs.getInt("completed") == 0) quizzesDue++;
                }
            }

            pendingStatLabel.setText(String.valueOf(pending));
            quizzesStatLabel.setText(String.valueOf(quizzesDue));

            if (pending > 0)
                pendingStatLabel.setStyle("-fx-text-fill: #e74c3c;");
            if (quizzesDue > 0)
                quizzesStatLabel.setStyle("-fx-text-fill: #e67e22;");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Course Progress ───────────────────────────────────────────

    private void setupCourseProgress() {
        courseProgressContainer.getChildren().clear();
        List<Course> courses = currentStudent.getCourses();

        if (courses.isEmpty()) {
            courseProgressContainer.getChildren()
                    .add(new Label("No courses enrolled yet."));
            return;
        }

        for (Course course : courses) {
            try {
                int courseId = getCourseId(course.getCourseCode());
                int total = 0, submitted = 0;
                ResultSet as = db.getAssignmentsForCourse(
                        courseId, currentStudent.getId());
                while (as.next()) {
                    total++;
                    if (as.getInt("submitted") == 1) submitted++;
                }
                double progress = total == 0 ? 0.0
                        : (double) submitted / total * 100.0;
                course.setProgressPercent(progress);
            } catch (Exception e) {
                e.printStackTrace();
            }

            VBox card = new VBox(6);
            card.getStyleClass().add("progress-card");

            HBox header = new HBox();
            Label name  = new Label(course.getDisplayLabel());
            name.getStyleClass().add("progress-course-name");
            Label pct   = new Label(
                    String.format("%.0f%%", course.getProgressPercent()));
            pct.getStyleClass().add("progress-percent");
            Region sp   = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            header.getChildren().addAll(name, sp, pct);

            ProgressBar bar = new ProgressBar(
                    course.getProgressPercent() / 100.0);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("course-progress-bar");

            Label sub = new Label(course.getInstructor());
            sub.getStyleClass().add("progress-sub");

            card.getChildren().addAll(header, bar, sub);
            courseProgressContainer.getChildren().add(card);
        }
    }

    // ── Upcoming Deadlines ────────────────────────────────────────

    private void setupUpcomingDeadlines() {
        ObservableList<String> items = FXCollections.observableArrayList();

        try {
            List<String[]> deadlines = new ArrayList<>();

            for (Course course : currentStudent.getCourses()) {
                int courseId = getCourseId(course.getCourseCode());
                ResultSet as = db.getAssignmentsForCourse(
                        courseId, currentStudent.getId());
                while (as.next()) {
                    if (as.getInt("submitted") == 0) {
                        String due     = as.getString("due_date");
                        String title   = as.getString("title");
                        String label   = getDaysLeftLabel(
                                LocalDate.parse(due));
                        deadlines.add(new String[]{due, title, label});
                    }
                }
            }

            deadlines.sort((a, b) -> a[0].compareTo(b[0]));

            if (deadlines.isEmpty()) {
                items.add("  No upcoming deadlines!");
            } else {
                for (String[] d : deadlines)
                    items.add("📝  " + d[1] + "  —  " + d[2]);
            }

        } catch (Exception e) {
            e.printStackTrace();
            items.add("Could not load deadlines.");
        }

        upcomingList.setItems(items);
    }

    private String getDaysLeftLabel(LocalDate dueDate) {
        long days = LocalDate.now().until(dueDate,
                java.time.temporal.ChronoUnit.DAYS);
        if (days < 0)  return " Overdue by " + Math.abs(days) + "d";
        if (days == 0) return " Due today";
        if (days == 1) return "Due tomorrow";
        return "Due in " + days + " days";
    }

    // ── Notifications ─────────────────────────────────────────────

    private void setupNotifications() {
        ObservableList<String> items = FXCollections.observableArrayList();

        try {
            // overdue assignments
            for (Course course : currentStudent.getCourses()) {
                int courseId = getCourseId(course.getCourseCode());
                ResultSet as = db.getAssignmentsForCourse(
                        courseId, currentStudent.getId());
                while (as.next()) {
                    if (as.getInt("submitted") == 0) {
                        LocalDate due = LocalDate.parse(
                                as.getString("due_date"));
                        if (due.isBefore(LocalDate.now()))
                            items.add("Overdue: "
                                    + as.getString("title")
                                    + " (" + course.getCourseCode() + ")");
                    }
                }
            }

            // announcements
            ResultSet ann = db.getAnnouncementsForStudent(
                    currentStudent.getId());
            int count = 0;
            while (ann.next() && count < 4) {
                items.add(" " + ann.getString("course_code")
                        + ": " + ann.getString("title"));
                count++;
            }

            if (items.isEmpty())
                items.add("You're all caught up!");

            notificationBadge.setText(String.valueOf(items.size()));
            notificationBadge.setVisible(!items.isEmpty());

        } catch (Exception e) {
            e.printStackTrace();
        }

        notificationsList.setItems(items);
    }

    // ── Grade Chart ───────────────────────────────────────────────

    private void setupGradeChart() {
        gradeChart.getData().clear();
        gradeChart.setLegendVisible(false);

        List<Grade> grades = currentStudent.getGrades();
        if (grades.isEmpty()) {
            gradeChart.setVisible(false);
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Grade g : grades)
            series.getData().add(new XYChart.Data<>(
                    g.getCourse().getCourseCode(), g.getScore()));

        gradeChart.getData().add(series);

        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d : series.getData()) {
                double score = d.getYValue().doubleValue();
                String color = score >= 80 ? "#2ecc71"
                        : score >= 60 ? "#f39c12"
                        : "#e74c3c";
                if (d.getNode() != null)
                    d.getNode().setStyle(
                            "-fx-bar-fill: " + color + ";");
            }
        });
    }

    // ── Search ────────────────────────────────────────────────────

    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                updateStatus("Ready");
                return;
            }
            long matches = currentStudent.getCourses().stream()
                    .filter(c -> c.getCourseName().toLowerCase()
                            .contains(newVal.toLowerCase())
                            || c.getCourseCode().toLowerCase()
                            .contains(newVal.toLowerCase()))
                    .count();
            updateStatus("Search: " + matches + " course(s) found");
        });
    }

    // ── Navigation ────────────────────────────────────────────────

    private void setActiveNav(Button btn) {
        if (activeNavButton != null)
            activeNavButton.getStyleClass().remove("nav-active");
        btn.getStyleClass().add("nav-active");
        activeNavButton = btn;
    }

    private void loadView(String fxml, String title, Button nav) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/" + fxml));
            VBox view = loader.load();
            FadeTransition fade =
                    new FadeTransition(Duration.millis(200), view);
            fade.setFromValue(0);
            fade.setToValue(1);
            contentArea.getChildren().setAll(view);
            fade.play();
            pageTitle.setText(title);
            setActiveNav(nav);
            updateStatus("Viewing: " + title);
        } catch (IOException e) {
            updateStatus("View coming soon: " + title);
        }
    }

    @FXML private void handleNavHome() {
        pageTitle.setText("Dashboard");
        setActiveNav(navHome);
        setupStatCards();
        setupCourseProgress();
        setupUpcomingDeadlines();
        setupNotifications();
        setupGradeChart();
        updateStatus("Home");
    }

    @FXML private void handleNavCourses() {
        loadView("Courses.fxml", "My Courses", navCourses); }
    @FXML private void handleNavAssignments() {
        loadView("Assignments.fxml", "Assignments", navAssignments); }
    @FXML private void handleNavQuizzes() {
        loadView("Quizzes.fxml", "Quizzes", navQuizzes); }
    @FXML private void handleNavGrades() {
        loadView("Grades.fxml", "Grade Tracker", navGrades); }
    @FXML private void handleNavCalendar() {
        loadView("Calendar.fxml", "Calendar", navCalendar); }
    @FXML private void handleNavNotifications() {
        loadView("Notifications.fxml", "Notifications",
                navNotifications); }
    @FXML private void handleNavProfile() {
        loadView("Profile.fxml", "My Profile", navProfile);
    }
    @FXML private void handleNavTranscript() {
        loadView("Transcript.fxml", "Academic Transcript",
                navTranscript);
    }

    @FXML
    private void handleExportPDF() {
        // let student choose save location
        javafx.stage.FileChooser chooser =
                new javafx.stage.FileChooser();
        chooser.setTitle("Save Result Slip");
        chooser.setInitialFileName(
                "result_slip_"
                        + currentStudent.getName()
                        .replace(" ", "_")
                        + ".pdf");
        chooser.getExtensionFilters().add(
                new javafx.stage.FileChooser
                        .ExtensionFilter("PDF Files", "*.pdf"));

        java.io.File file = chooser.showSaveDialog(
                navExport.getScene().getWindow());

        if (file != null) {
            try {
                com.dashboard.util.PdfExporter
                        .exportResultSlip(
                                currentStudent.getId(),
                                file.getAbsolutePath());

                new Alert(Alert.AlertType.INFORMATION,
                        "Result slip saved to:\n"
                                + file.getAbsolutePath(),
                        ButtonType.OK).showAndWait();

            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR,
                        "Failed to export PDF:\n"
                                + e.getMessage(),
                        ButtonType.OK).showAndWait();
            }
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Are you sure you want to logout?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                SessionManager.getInstance().clearSession();
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/fxml/Login.fxml"));
                    Scene scene = new Scene(
                            loader.load(), 1100, 700);
                    scene.getStylesheets().add(
                            getClass().getResource("/css/style.css")
                                    .toExternalForm());
                    Stage stage = (Stage) navHome.getScene()
                            .getWindow();
                    stage.setScene(scene);
                    stage.centerOnScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────

    /**
     * Looks up the database course_id by course code.
     */
    private int getCourseId(String courseCode) throws Exception {
        ResultSet rs = db.getConnection().prepareStatement(
                        "SELECT id FROM courses WHERE code = '"
                                + courseCode + "'")
                .executeQuery();
        return rs.next() ? rs.getInt("id") : -1;
    }

    private void updateStatus(String msg) {
        if (statusLabel != null)
            statusLabel.setText(msg);
    }
}