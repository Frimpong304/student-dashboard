package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.model.Lecturer;
import com.dashboard.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LecturerController {

    // ── Sidebar ───────────────────────────────────────────────────
    @FXML private Label  lecturerInitialsLabel;
    @FXML private Label  lecturerNameLabel;
    @FXML private Label  lecturerTitleLabel;
    @FXML private Label  departmentLabel;

    @FXML private Button navOverview;
    @FXML private Button navMyCourses;
    @FXML private Button navGrades;
    @FXML private Button navAssignments;
    @FXML private Button navQuizzes;
    @FXML private Button navStudents;
    @FXML private Button navAnnouncements;

    // ── Top Bar ───────────────────────────────────────────────────
    @FXML private Label pageTitle;
    @FXML private Label semesterLabel;

    // ── Welcome Banner ────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label courseCountLabel;
    @FXML private Label totalStudentsLabel;

    // ── Stat Cards ────────────────────────────────────────────────
    @FXML private Label coursesStatLabel;
    @FXML private Label studentsStatLabel;
    @FXML private Label assignmentsStatLabel;
    @FXML private Label gradesStatLabel;

    // ── Overview Table ────────────────────────────────────────────
    @FXML private TableView<ObservableList<String>>
            overviewCoursesTable;
    @FXML private TableColumn<ObservableList<String>, String>
            overviewCodeCol;
    @FXML private TableColumn<ObservableList<String>, String>
            overviewNameCol;
    @FXML private TableColumn<ObservableList<String>, String>
            overviewCreditsCol;
    @FXML private TableColumn<ObservableList<String>, String>
            overviewStudentsCol;
    @FXML private TableColumn<ObservableList<String>, String>
            overviewSemesterCol;

    // ── Announcements ─────────────────────────────────────────────
    @FXML private ListView<String> announcementsList;

    // ── Content & Status ──────────────────────────────────────────
    @FXML private VBox  contentArea;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;

    // ── State ─────────────────────────────────────────────────────
    private Lecturer        currentLecturer;
    private DatabaseManager db;
    private Button          activeNavButton;

    // ══════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ══════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        db = DatabaseManager.getInstance();
        loadLecturerFromDatabase();

        if (currentLecturer == null) {
            updateStatus(
                    "Error: could not load lecturer data.");
            return;
        }

        setupSidebar();
        setupWelcomeBanner();
        setupStatCards();
        setupOverviewTable();
        setupAnnouncements();
        setActiveNav(navOverview);
        updateStatus("Welcome back, "
                + currentLecturer.getFirstName() + "!");
    }

    // ══════════════════════════════════════════════════════════════
    // LOAD LECTURER FROM DATABASE
    // ══════════════════════════════════════════════════════════════

    private void loadLecturerFromDatabase() {
        try {
            String email =
                    SessionManager.getInstance().getEmail();
            ResultSet rs = db.getLecturerByEmail(email);

            if (rs == null || !rs.next()) return;

            currentLecturer = new Lecturer();
            currentLecturer.setId(rs.getInt("user_id"));
            currentLecturer.setLecturerId(
                    rs.getInt("lecturer_id"));
            currentLecturer.setName(rs.getString("name"));
            currentLecturer.setEmail(rs.getString("email"));
            currentLecturer.setTitle(rs.getString("title"));
            currentLecturer.setDepartment(
                    rs.getString("department"));

            SessionManager.getInstance()
                    .setRoleSpecificId(
                            rs.getInt("lecturer_id"));

            // load courses into model
            ResultSet courses = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (courses.next()) {
                com.dashboard.model.Course c =
                        new com.dashboard.model.Course();
                c.setCourseCode(courses.getString("code"));
                c.setCourseName(courses.getString("name"));
                c.setCredits(courses.getInt("credits"));
                c.setSemester(courses.getString("semester"));
                currentLecturer.addCourse(c);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // SIDEBAR SETUP
    // ══════════════════════════════════════════════════════════════

    private void setupSidebar() {
        lecturerInitialsLabel.setText(
                currentLecturer.getInitials());
        lecturerNameLabel.setText(
                currentLecturer.getName());
        lecturerTitleLabel.setText(
                currentLecturer.getTitle()
                        + " — "
                        + currentLecturer.getDepartment());
        departmentLabel.setText(
                "University of Cape Coast");
    }

    // ══════════════════════════════════════════════════════════════
    // WELCOME BANNER
    // ══════════════════════════════════════════════════════════════

    private void setupWelcomeBanner() {
        welcomeLabel.setText("Welcome back, "
                + currentLecturer.getFirstName() + "! ");
        dateLabel.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern(
                        "EEEE, MMMM dd yyyy")));
        courseCountLabel.setText(
                currentLecturer.getCourses().size()
                        + " Courses");
        try {
            int total = 0;
            ResultSet courses = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (courses.next())
                total += courses.getInt("enrolled_count");
            totalStudentsLabel.setText(
                    total + " Students");
        } catch (Exception e) {
            totalStudentsLabel.setText("-- Students");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // STAT CARDS
    // ══════════════════════════════════════════════════════════════

    private void setupStatCards() {
        try {
            int totalCourses     = 0;
            int totalStudents    = 0;
            int totalAssignments = 0;
            int totalGrades      = 0;

            ResultSet courses = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (courses.next()) {
                totalCourses++;
                totalStudents +=
                        courses.getInt("enrolled_count");
                int courseId = courses.getInt("id");

                PreparedStatement aStmt =
                        db.getConnection().prepareStatement(
                                "SELECT COUNT(*) as cnt "
                                        + "FROM assignments "
                                        + "WHERE course_id = ?");
                aStmt.setInt(1, courseId);
                ResultSet aRs = aStmt.executeQuery();
                if (aRs.next())
                    totalAssignments +=
                            aRs.getInt("cnt");

                PreparedStatement gStmt =
                        db.getConnection().prepareStatement(
                                "SELECT COUNT(*) as cnt "
                                        + "FROM grades "
                                        + "WHERE course_id = ?");
                gStmt.setInt(1, courseId);
                ResultSet gRs = gStmt.executeQuery();
                if (gRs.next())
                    totalGrades += gRs.getInt("cnt");
            }

            coursesStatLabel.setText(
                    String.valueOf(totalCourses));
            studentsStatLabel.setText(
                    String.valueOf(totalStudents));
            assignmentsStatLabel.setText(
                    String.valueOf(totalAssignments));
            gradesStatLabel.setText(
                    String.valueOf(totalGrades));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // OVERVIEW TABLE
    // ══════════════════════════════════════════════════════════════

    private void setupOverviewTable() {
        overviewCodeCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        overviewNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        overviewCreditsCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        overviewStudentsCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));
        overviewSemesterCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(4)));

        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(String.valueOf(
                        rs.getInt("credits")));
                row.add(String.valueOf(
                        rs.getInt("enrolled_count")));
                row.add(rs.getString("semester"));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        overviewCoursesTable.setItems(rows);
    }

    // ══════════════════════════════════════════════════════════════
    // ANNOUNCEMENTS LIST
    // ══════════════════════════════════════════════════════════════

    private void setupAnnouncements() {
        ObservableList<String> items =
                FXCollections.observableArrayList();
        try {
            PreparedStatement ps =
                    db.getConnection().prepareStatement("""
                SELECT a.title, a.posted_at, c.code
                FROM announcements a
                JOIN courses c ON c.id = a.course_id
                WHERE a.lecturer_id = ?
                ORDER BY a.posted_at DESC
                LIMIT 5
            """);
            ps.setInt(1, currentLecturer.getLecturerId());
            ResultSet ann = ps.executeQuery();
            while (ann.next())
                items.add("📢  ["
                        + ann.getString("code") + "]  "
                        + ann.getString("title")
                        + "  —  "
                        + ann.getString("posted_at"));
            if (items.isEmpty())
                items.add("No announcements posted yet.");
        } catch (Exception e) {
            e.printStackTrace();
            items.add("Could not load announcements.");
        }
        announcementsList.setItems(items);
    }

    // ══════════════════════════════════════════════════════════════
    // ENTER GRADES VIEW
    // ══════════════════════════════════════════════════════════════

    private void showEnterGradesView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        Label heading = new Label(
                "Select a Course to Enter Grades");
        heading.getStyleClass().add("section-title");

        ComboBox<String> courseCombo = new ComboBox<>();
        courseCombo.setPromptText("Choose course...");
        courseCombo.setPrefWidth(320);

        java.util.Map<String, Integer> courseMap =
                new java.util.LinkedHashMap<>();
        try {
            ResultSet rs = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (rs.next()) {
                String label = rs.getString("code")
                        + " — " + rs.getString("name");
                courseMap.put(label, rs.getInt("id"));
                courseCombo.getItems().add(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // grades table
        TableView<ObservableList<String>> gradesTable =
                new TableView<>();
        gradesTable.setPrefHeight(350);

        TableColumn<ObservableList<String>, String>
                numCol = new TableColumn<>("Student No.");
        TableColumn<ObservableList<String>, String>
                nameCol = new TableColumn<>("Name");
        TableColumn<ObservableList<String>, String>
                scoreCol =
                new TableColumn<>("Current Score");
        TableColumn<ObservableList<String>, String>
                gradeCol = new TableColumn<>("Grade");

        numCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        nameCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        scoreCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        gradeCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));

        numCol.setPrefWidth(120);
        nameCol.setPrefWidth(200);
        scoreCol.setPrefWidth(120);
        gradeCol.setPrefWidth(80);

        gradesTable.getColumns().addAll(
                numCol, nameCol, scoreCol, gradeCol);

        // load students when course is selected
        courseCombo.setOnAction(e -> {
            String selected = courseCombo.getValue();
            if (selected == null) return;
            loadStudentsIntoTable(gradesTable,
                    courseMap.get(selected));
        });

        // Enter Grade button
        Button enterBtn = new Button(
                "✏️  Enter / Update Grade");
        enterBtn.getStyleClass().add("primary-button");
        enterBtn.setOnAction(e -> {
            String selected = courseCombo.getValue();
            if (selected == null) {
                showAlert(
                        "Please select a course first.");
                return;
            }
            showEnterGradeDialog(gradesTable,
                    courseMap.get(selected));
        });

        // Submit for Approval button
        Button submitBtn = new Button(
                "📤  Submit for Approval");
        submitBtn.getStyleClass().add("secondary-button");
        submitBtn.setOnAction(e -> {
            String selected = courseCombo.getValue();
            if (selected == null) {
                showAlert(
                        "Please select a course first.");
                return;
            }
            int courseId = courseMap.get(selected);
            handleSubmitForApproval(
                    courseId, selected);
        });

        HBox toolbar = new HBox(12,
                courseCombo, enterBtn, submitBtn);
        toolbar.setAlignment(
                javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                heading, toolbar, gradesTable);
        card.getStyleClass().add("section-card");
        card.setPadding(
                new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
    }

    private void handleSubmitForApproval(
            int courseId, String courseName) {
        try {
            PreparedStatement check =
                    db.getConnection().prepareStatement(
                            "SELECT COUNT(*) as cnt "
                                    + "FROM grades "
                                    + "WHERE course_id = ?");
            check.setInt(1, courseId);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getInt("cnt") == 0) {
                showAlert("No grades entered "
                        + "yet for this course.");
                return;
            }

            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Submit Grades");
            confirm.setHeaderText(
                    "Submit grades for "
                            + courseName
                            + " for admin approval?");
            confirm.setContentText(
                    "Once submitted, grades will be "
                            + "reviewed by the admin before "
                            + "students can see them.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    try {
                        db.submitGradesForApproval(
                                courseId,
                                currentLecturer
                                        .getLecturerId(),
                                "2025/2026 "
                                        + "Second Semester");
                        showAlert(
                                "Grades submitted "
                                        + "for approval!");
                        updateStatus(
                                "Grades submitted: "
                                        + courseName);
                        setupStatCards();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert("Error: "
                                + ex.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error: " + e.getMessage());
        }
    }

    private void loadStudentsIntoTable(
            TableView<ObservableList<String>> table,
            int courseId) {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs =
                    db.getStudentsInCourse(courseId);
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("student_number"));
                row.add(rs.getString("name"));
                double score = rs.getDouble("score");
                row.add(score > 0
                        ? String.format("%.1f", score)
                        : "Not entered");
                row.add(rs.getString("letter_grade") != null
                        ? rs.getString("letter_grade")
                        : "--");
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);
        updateStatus("Loaded " + rows.size()
                + " students.");
    }

    private void showEnterGradeDialog(
            TableView<ObservableList<String>> table,
            int courseId) {
        ObservableList<String> selected =
                table.getSelectionModel()
                        .getSelectedItem();
        if (selected == null) {
            showAlert("Please select a student "
                    + "from the table.");
            return;
        }

        String studentNum  = selected.get(0);
        String studentName = selected.get(1);

        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Enter Grade");
        dialog.setHeaderText("Enter grade for "
                + studentName
                + " (" + studentNum + ")");

        ButtonType saveBtn = new ButtonType(
                "Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField scoreField = new TextField();
        scoreField.setPromptText(
                "Enter score (0 - 100)");
        scoreField.getStyleClass().add("login-field");

        Label currentLabel = new Label(
                "Current: " + selected.get(2));
        currentLabel.setStyle(
                "-fx-text-fill: #5a8a6a; "
                        + "-fx-font-size: 11px;");

        VBox content = new VBox(10,
                new Label("Score:"),
                scoreField,
                currentLabel);
        content.setPadding(
                new javafx.geometry.Insets(20));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    double score = Double.parseDouble(
                            scoreField.getText().trim());
                    if (score < 0 || score > 100) {
                        showAlert("Score must be "
                                + "between 0 and 100.");
                        return null;
                    }
                    return score;
                } catch (NumberFormatException e) {
                    showAlert("Please enter a valid "
                            + "number.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(score -> {
            try {
                PreparedStatement ps =
                        db.getConnection().prepareStatement(
                                "SELECT id FROM students "
                                        + "WHERE student_number = ?");
                ps.setString(1, studentNum);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int studentId = rs.getInt("id");
                    db.upsertGrade(
                            studentId,
                            courseId,
                            score,
                            currentLecturer.getLecturerId(),
                            "2025/2026 Second Semester");
                    showAlert(
                            "Grade saved successfully!");
                    loadStudentsIntoTable(
                            table, courseId);
                    setupStatCards();
                    updateStatus("Grade saved for "
                            + studentName);
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Error saving grade: "
                        + e.getMessage());
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // ASSIGNMENTS VIEW
    // ══════════════════════════════════════════════════════════════

    private void showAssignmentsView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        ComboBox<String> courseCombo = new ComboBox<>();
        courseCombo.setPromptText("Select course...");
        courseCombo.setPrefWidth(320);

        java.util.Map<String, Integer> courseMap =
                new java.util.LinkedHashMap<>();
        try {
            ResultSet rs = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (rs.next()) {
                String label = rs.getString("code")
                        + " — " + rs.getString("name");
                courseMap.put(label, rs.getInt("id"));
                courseCombo.getItems().add(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TableView<ObservableList<String>> table =
                new TableView<>();
        table.setPrefHeight(320);

        TableColumn<ObservableList<String>, String>
                titleCol = new TableColumn<>("Title");
        TableColumn<ObservableList<String>, String>
                descCol =
                new TableColumn<>("Description");
        TableColumn<ObservableList<String>, String>
                dueCol = new TableColumn<>("Due Date");
        TableColumn<ObservableList<String>, String>
                priCol = new TableColumn<>("Priority");

        titleCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        descCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        dueCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        priCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));

        titleCol.setPrefWidth(180);
        descCol.setPrefWidth(250);
        dueCol.setPrefWidth(110);
        priCol.setPrefWidth(80);

        table.getColumns().addAll(
                titleCol, descCol, dueCol, priCol);

        courseCombo.setOnAction(e -> {
            String sel = courseCombo.getValue();
            if (sel == null) return;
            loadAssignmentsIntoTable(
                    table, courseMap.get(sel));
        });

        Button addBtn =
                new Button("➕  Add Assignment");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            if (courseCombo.getValue() == null) {
                showAlert(
                        "Please select a course first.");
                return;
            }
            showAddAssignmentDialog(table,
                    courseMap.get(
                            courseCombo.getValue()));
        });

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> {
            ObservableList<String> sel =
                    table.getSelectionModel()
                            .getSelectedItem();
            if (sel == null) {
                showAlert("Please select an assignment"
                        + " to delete.");
                return;
            }
            if (courseCombo.getValue() == null) return;
            deleteAssignment(table, sel.get(0),
                    courseMap.get(
                            courseCombo.getValue()));
        });

        HBox toolbar = new HBox(12,
                courseCombo, addBtn, deleteBtn);
        toolbar.setAlignment(
                javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                new Label("Manage Assignments") {{
                    getStyleClass().add("section-title");
                }},
                toolbar, table);
        card.getStyleClass().add("section-card");
        card.setPadding(
                new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
    }

    private void loadAssignmentsIntoTable(
            TableView<ObservableList<String>> table,
            int courseId) {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            PreparedStatement ps =
                    db.getConnection().prepareStatement(
                            "SELECT title, description, "
                                    + "due_date, priority "
                                    + "FROM assignments "
                                    + "WHERE course_id = ? "
                                    + "ORDER BY due_date");
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("title"));
                row.add(rs.getString("description"));
                row.add(rs.getString("due_date"));
                row.add(rs.getString("priority"));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);
        updateStatus(rows.size()
                + " assignments loaded.");
    }

    private void showAddAssignmentDialog(
            TableView<ObservableList<String>> table,
            int courseId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Assignment");
        dialog.setHeaderText("Add New Assignment");

        ButtonType saveBtn = new ButtonType(
                "Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Assignment title");

        TextField descField = new TextField();
        descField.setPromptText("Description");

        DatePicker duePicker = new DatePicker(
                LocalDate.now().plusDays(7));

        ComboBox<String> priorityCombo =
                new ComboBox<>();
        priorityCombo.getItems().addAll(
                "HIGH", "MEDIUM", "LOW");
        priorityCombo.setValue("MEDIUM");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(
                new javafx.geometry.Insets(20));
        grid.add(new Label("Title:"),       0, 0);
        grid.add(titleField,                1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField,                 1, 1);
        grid.add(new Label("Due Date:"),    0, 2);
        grid.add(duePicker,                 1, 2);
        grid.add(new Label("Priority:"),    0, 3);
        grid.add(priorityCombo,             1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    String title =
                            titleField.getText().trim();
                    if (title.isEmpty()) {
                        showAlert(
                                "Title cannot be empty.");
                        return null;
                    }
                    PreparedStatement ps =
                            db.getConnection()
                                    .prepareStatement(
                                            "INSERT INTO assignments"
                                                    + " (course_id, title,"
                                                    + " description,"
                                                    + " due_date, priority)"
                                                    + " VALUES (?,?,?,?,?)");
                    ps.setInt(1, courseId);
                    ps.setString(2, title);
                    ps.setString(3,
                            descField.getText().trim());
                    ps.setString(4,
                            duePicker.getValue()
                                    .toString());
                    ps.setString(5,
                            priorityCombo.getValue());
                    ps.executeUpdate();

                    loadAssignmentsIntoTable(
                            table, courseId);
                    setupStatCards();
                    updateStatus(
                            "Assignment added: " + title);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteAssignment(
            TableView<ObservableList<String>> table,
            String title, int courseId) {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Assignment");
        confirm.setHeaderText(
                "Delete \"" + title + "\"?");
        confirm.setContentText(
                "This will also delete all student "
                        + "submissions for this assignment.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    PreparedStatement ps =
                            db.getConnection()
                                    .prepareStatement(
                                            "DELETE FROM assignments"
                                                    + " WHERE title = ?"
                                                    + " AND course_id = ?");
                    ps.setString(1, title);
                    ps.setInt(2, courseId);
                    ps.executeUpdate();
                    loadAssignmentsIntoTable(
                            table, courseId);
                    setupStatCards();
                    updateStatus("Deleted: " + title);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // QUIZZES VIEW
    // ══════════════════════════════════════════════════════════════

    private void showQuizzesView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        ComboBox<String> courseCombo = new ComboBox<>();
        courseCombo.setPromptText("Select course...");
        courseCombo.setPrefWidth(320);

        java.util.Map<String, Integer> courseMap =
                new java.util.LinkedHashMap<>();
        try {
            ResultSet rs = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (rs.next()) {
                String label = rs.getString("code")
                        + " — " + rs.getString("name");
                courseMap.put(label, rs.getInt("id"));
                courseCombo.getItems().add(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TableView<ObservableList<String>> table =
                new TableView<>();
        table.setPrefHeight(320);

        TableColumn<ObservableList<String>, String>
                titleCol = new TableColumn<>("Title");
        TableColumn<ObservableList<String>, String>
                dueCol = new TableColumn<>("Due Date");
        TableColumn<ObservableList<String>, String>
                qCol = new TableColumn<>("Questions");
        TableColumn<ObservableList<String>, String>
                durCol = new TableColumn<>("Duration");

        titleCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        dueCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        qCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        durCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));

        titleCol.setPrefWidth(220);
        dueCol.setPrefWidth(110);
        qCol.setPrefWidth(90);
        durCol.setPrefWidth(90);

        table.getColumns().addAll(
                titleCol, dueCol, qCol, durCol);

        courseCombo.setOnAction(e -> {
            String sel = courseCombo.getValue();
            if (sel != null)
                loadQuizzesIntoTable(
                        table, courseMap.get(sel));
        });

        Button addBtn = new Button("➕  Add Quiz");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> {
            if (courseCombo.getValue() == null) {
                showAlert(
                        "Please select a course first.");
                return;
            }
            showAddQuizDialog(table,
                    courseMap.get(
                            courseCombo.getValue()));
        });

        HBox toolbar = new HBox(12,
                courseCombo, addBtn);
        toolbar.setAlignment(
                javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                new Label("Manage Quizzes") {{
                    getStyleClass().add("section-title");
                }},
                toolbar, table);
        card.getStyleClass().add("section-card");
        card.setPadding(
                new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
    }

    private void loadQuizzesIntoTable(
            TableView<ObservableList<String>> table,
            int courseId) {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            PreparedStatement ps =
                    db.getConnection().prepareStatement(
                            "SELECT title, due_date, "
                                    + "total_questions, "
                                    + "duration_mins "
                                    + "FROM quizzes "
                                    + "WHERE course_id = ? "
                                    + "ORDER BY due_date");
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("title"));
                row.add(rs.getString("due_date"));
                row.add(rs.getInt("total_questions")
                        + " Qs");
                row.add(rs.getInt("duration_mins")
                        + " min");
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);
        updateStatus(rows.size() + " quizzes loaded.");
    }

    private void showAddQuizDialog(
            TableView<ObservableList<String>> table,
            int courseId) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Quiz");
        dialog.setHeaderText("Add New Quiz");

        ButtonType saveBtn = new ButtonType(
                "Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Quiz title");

        DatePicker duePicker = new DatePicker(
                LocalDate.now().plusDays(7));

        TextField questionsField =
                new TextField("10");
        TextField durationField =
                new TextField("20");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(
                new javafx.geometry.Insets(20));
        grid.add(new Label("Title:"),            0, 0);
        grid.add(titleField,                     1, 0);
        grid.add(new Label("Due Date:"),         0, 1);
        grid.add(duePicker,                      1, 1);
        grid.add(new Label("No. of Questions:"), 0, 2);
        grid.add(questionsField,                 1, 2);
        grid.add(new Label("Duration (mins):"),  0, 3);
        grid.add(durationField,                  1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    String title =
                            titleField.getText().trim();
                    if (title.isEmpty()) {
                        showAlert(
                                "Title cannot be empty.");
                        return null;
                    }
                    int questions = Integer.parseInt(
                            questionsField
                                    .getText().trim());
                    int duration = Integer.parseInt(
                            durationField
                                    .getText().trim());

                    PreparedStatement ps =
                            db.getConnection()
                                    .prepareStatement(
                                            "INSERT INTO quizzes"
                                                    + " (course_id, title,"
                                                    + " due_date,"
                                                    + " total_questions,"
                                                    + " duration_mins)"
                                                    + " VALUES (?,?,?,?,?)");
                    ps.setInt(1, courseId);
                    ps.setString(2, title);
                    ps.setString(3,
                            duePicker.getValue()
                                    .toString());
                    ps.setInt(4, questions);
                    ps.setInt(5, duration);
                    ps.executeUpdate();

                    loadQuizzesIntoTable(
                            table, courseId);
                    updateStatus(
                            "Quiz added: " + title);
                } catch (NumberFormatException e) {
                    showAlert(
                            "Questions and duration "
                                    + "must be numbers.");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // ══════════════════════════════════════════════════════════════
    // STUDENTS VIEW
    // ══════════════════════════════════════════════════════════════

    private void showStudentsView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        ComboBox<String> courseCombo = new ComboBox<>();
        courseCombo.setPromptText("Select course...");
        courseCombo.setPrefWidth(320);

        java.util.Map<String, Integer> courseMap =
                new java.util.LinkedHashMap<>();
        try {
            ResultSet rs = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (rs.next()) {
                String label = rs.getString("code")
                        + " — " + rs.getString("name");
                courseMap.put(label, rs.getInt("id"));
                courseCombo.getItems().add(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TableView<ObservableList<String>> table =
                new TableView<>();
        table.setPrefHeight(350);

        TableColumn<ObservableList<String>, String>
                numCol =
                new TableColumn<>("Student No.");
        TableColumn<ObservableList<String>, String>
                nameCol = new TableColumn<>("Name");
        TableColumn<ObservableList<String>, String>
                scoreCol = new TableColumn<>("Score");
        TableColumn<ObservableList<String>, String>
                letterCol =
                new TableColumn<>("Grade");

        numCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        nameCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        scoreCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        letterCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));

        numCol.setPrefWidth(130);
        nameCol.setPrefWidth(200);
        scoreCol.setPrefWidth(100);
        letterCol.setPrefWidth(80);

        table.getColumns().addAll(
                numCol, nameCol, scoreCol, letterCol);

        courseCombo.setOnAction(e -> {
            String sel = courseCombo.getValue();
            if (sel != null)
                loadStudentsIntoTable(
                        table, courseMap.get(sel));
        });

        HBox toolbar = new HBox(12, courseCombo);
        toolbar.setAlignment(
                javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                new Label("Enrolled Students") {{
                    getStyleClass().add("section-title");
                }},
                toolbar, table);
        card.getStyleClass().add("section-card");
        card.setPadding(
                new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
        updateStatus("Select a course to view students.");
    }

    // ══════════════════════════════════════════════════════════════
    // ANNOUNCEMENTS VIEW
    // ══════════════════════════════════════════════════════════════

    private void showAnnouncementsView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        ComboBox<String> courseCombo = new ComboBox<>();
        courseCombo.setPromptText("Select course...");
        courseCombo.setPrefWidth(280);

        java.util.Map<String, Integer> courseMap =
                new java.util.LinkedHashMap<>();
        try {
            ResultSet rs = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (rs.next()) {
                String label = rs.getString("code")
                        + " — " + rs.getString("name");
                courseMap.put(label, rs.getInt("id"));
                courseCombo.getItems().add(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextField titleField = new TextField();
        titleField.setPromptText(
                "Announcement title");
        titleField.setPrefWidth(300);

        TextArea bodyArea = new TextArea();
        bodyArea.setPromptText(
                "Write your announcement here...");
        bodyArea.setPrefHeight(120);
        bodyArea.setWrapText(true);

        Button postBtn = new Button(
                "📢  Post Announcement");
        postBtn.getStyleClass().add("primary-button");
        postBtn.setOnAction(e -> {
            if (courseCombo.getValue() == null) {
                showAlert("Please select a course.");
                return;
            }
            if (titleField.getText().trim().isEmpty()) {
                showAlert("Please enter a title.");
                return;
            }
            try {
                int courseId = courseMap.get(
                        courseCombo.getValue());
                PreparedStatement ps =
                        db.getConnection().prepareStatement(
                                "INSERT INTO announcements"
                                        + " (course_id, lecturer_id,"
                                        + " title, body)"
                                        + " VALUES (?, ?, ?, ?)");
                ps.setInt(1, courseId);
                ps.setInt(2,
                        currentLecturer.getLecturerId());
                ps.setString(3,
                        titleField.getText().trim());
                ps.setString(4,
                        bodyArea.getText().trim());
                ps.executeUpdate();

                titleField.clear();
                bodyArea.clear();
                setupAnnouncements();
                updateStatus("Announcement posted.");
                showAlert("Announcement posted "
                        + "successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error: " + ex.getMessage());
            }
        });

        // recent announcements list
        ListView<String> recentList =
                new ListView<>();
        recentList.setPrefHeight(160);
        recentList.getStyleClass().add(
                "notification-list");

        ObservableList<String> recentItems =
                FXCollections.observableArrayList();
        try {
            PreparedStatement ps =
                    db.getConnection().prepareStatement("""
                SELECT a.title, a.body,
                       a.posted_at, c.code
                FROM announcements a
                JOIN courses c ON c.id = a.course_id
                WHERE a.lecturer_id = ?
                ORDER BY a.posted_at DESC
                LIMIT 10
            """);
            ps.setInt(1,
                    currentLecturer.getLecturerId());
            ResultSet ann = ps.executeQuery();
            while (ann.next())
                recentItems.add("📢  ["
                        + ann.getString("code") + "]  "
                        + ann.getString("title")
                        + "  ("
                        + ann.getString("posted_at")
                        + ")");
            if (recentItems.isEmpty())
                recentItems.add(
                        "No announcements yet.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        recentList.setItems(recentItems);

        VBox card = new VBox(14,
                new Label("Post New Announcement") {{
                    getStyleClass().add("section-title");
                }},
                new Label("Course:"),
                courseCombo,
                new Label("Title:"),
                titleField,
                new Label("Message:"),
                bodyArea,
                postBtn,
                new Separator(),
                new Label("Recent Announcements") {{
                    getStyleClass().add("section-title");
                }},
                recentList);
        card.getStyleClass().add("section-card");
        card.setPadding(
                new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
    }

    // ══════════════════════════════════════════════════════════════
    // MY COURSES VIEW
    // ══════════════════════════════════════════════════════════════

    private void showCoursesView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        TableView<ObservableList<String>> table =
                new TableView<>();
        table.setPrefHeight(400);

        TableColumn<ObservableList<String>, String>
                codeCol = new TableColumn<>("Code");
        TableColumn<ObservableList<String>, String>
                nameCol =
                new TableColumn<>("Course Name");
        TableColumn<ObservableList<String>, String>
                creditsCol =
                new TableColumn<>("Credits");
        TableColumn<ObservableList<String>, String>
                studentsCol =
                new TableColumn<>("Students");
        TableColumn<ObservableList<String>, String>
                semCol = new TableColumn<>("Semester");
        TableColumn<ObservableList<String>, String>
                descCol =
                new TableColumn<>("Description");

        codeCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        nameCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        creditsCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        studentsCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));
        semCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(4)));
        descCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(5)));

        codeCol.setPrefWidth(80);
        nameCol.setPrefWidth(200);
        creditsCol.setPrefWidth(70);
        studentsCol.setPrefWidth(80);
        semCol.setPrefWidth(170);
        descCol.setPrefWidth(200);

        table.getColumns().addAll(codeCol, nameCol,
                creditsCol, studentsCol,
                semCol, descCol);

        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getCoursesForLecturer(
                    currentLecturer.getLecturerId());
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(String.valueOf(
                        rs.getInt("credits")));
                row.add(String.valueOf(
                        rs.getInt("enrolled_count")));
                row.add(rs.getString("semester"));
                String desc = rs.getString("description");
                row.add(desc != null ? desc : "--");
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);

        VBox card = new VBox(16,
                new Label("My Courses") {{
                    getStyleClass().add("section-title");
                }},
                table);
        card.getStyleClass().add("section-card");
        card.setPadding(
                new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
        updateStatus(rows.size() + " courses loaded.");
    }

    // ══════════════════════════════════════════════════════════════
    // NAVIGATION
    // ══════════════════════════════════════════════════════════════

    private void setActiveNav(Button btn) {
        if (activeNavButton != null)
            activeNavButton.getStyleClass()
                    .remove("nav-active");
        btn.getStyleClass().add("nav-active");
        activeNavButton = btn;
    }

    @FXML
    private void handleNavOverview() {
        pageTitle.setText("Overview");
        setActiveNav(navOverview);
        contentArea.getChildren().clear();
        setupOverviewTable();
        setupAnnouncements();
        setupStatCards();
        updateStatus("Overview");
    }

    @FXML
    private void handleNavMyCourses() {
        pageTitle.setText("My Courses");
        setActiveNav(navMyCourses);
        showCoursesView();
    }

    @FXML
    private void handleNavGrades() {
        pageTitle.setText("Enter Grades");
        setActiveNav(navGrades);
        showEnterGradesView();
        updateStatus("Grade Entry");
    }

    @FXML
    private void handleNavAssignments() {
        pageTitle.setText("Assignments");
        setActiveNav(navAssignments);
        showAssignmentsView();
        updateStatus("Assignments");
    }

    @FXML
    private void handleNavQuizzes() {
        pageTitle.setText("Quizzes");
        setActiveNav(navQuizzes);
        showQuizzesView();
        updateStatus("Quizzes");
    }

    @FXML
    private void handleNavStudents() {
        pageTitle.setText("Students");
        setActiveNav(navStudents);
        showStudentsView();
        updateStatus("Students");
    }

    @FXML
    private void handleNavAnnouncements() {
        pageTitle.setText("Announcements");
        setActiveNav(navAnnouncements);
        showAnnouncementsView();
        updateStatus("Announcements");
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText(
                "Are you sure you want to logout?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                SessionManager.getInstance()
                        .clearSession();
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/Login.fxml"));
                    Scene scene = new Scene(
                            loader.load(), 1100, 700);
                    scene.getStylesheets().add(
                            getClass().getResource(
                                            "/css/style.css")
                                    .toExternalForm());
                    Stage stage =
                            (Stage) navOverview
                                    .getScene()
                                    .getWindow();
                    stage.setScene(scene);
                    stage.centerOnScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    private void updateStatus(String msg) {
        if (statusLabel != null)
            statusLabel.setText(msg);
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION,
                msg, ButtonType.OK).showAndWait();
    }
}