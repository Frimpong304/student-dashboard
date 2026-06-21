package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.sql.ResultSet;

public class CourseRegistrationController {

    // Available courses table
    @FXML private TableView<ObservableList<String>> availableTable;
    @FXML private TableColumn<ObservableList<String>, String> avCodeCol;
    @FXML private TableColumn<ObservableList<String>, String> avNameCol;
    @FXML private TableColumn<ObservableList<String>, String> avCreditsCol;
    @FXML private TableColumn<ObservableList<String>, String> avLecturerCol;
    @FXML private TableColumn<ObservableList<String>, String> avStatusCol;

    // Registered courses table
    @FXML private TableView<ObservableList<String>> registeredTable;
    @FXML private TableColumn<ObservableList<String>, String> rgCodeCol;
    @FXML private TableColumn<ObservableList<String>, String> rgNameCol;
    @FXML private TableColumn<ObservableList<String>, String> rgStatusCol;

    // Summary & status
    @FXML private Label   windowStatusLabel;
    @FXML private Label   windowDatesLabel;
    @FXML private HBox    statusBanner;
    @FXML private Label   bannerLabel;
    @FXML private Label   creditSummaryLabel;
    @FXML private Label   totalCreditsLabel;
    @FXML private Label   totalCoursesLabel;
    @FXML private Label   regFeedbackLabel;
    @FXML private TextField searchField;

    private static final String SEMESTER =
            "2025/2026 Second Semester";

    private DatabaseManager                        db;
    private int                                    studentId;
    private boolean                                regOpen = false;
    private ObservableList<ObservableList<String>> allAvailable;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        studentId = SessionManager.getInstance()
                .getRoleSpecificId();

        setupTables();
        checkRegistrationWindow();
        loadAvailableCourses();
        loadRegisteredCourses();
        setupSearch();
    }

    // ── Table Setup ───────────────────────────────────────────────

    private void setupTables() {
        avCodeCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        avNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        avCreditsCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        avLecturerCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));
        avStatusCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(4)));

        rgCodeCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        rgNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        rgStatusCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
    }

    // ── Registration Window ───────────────────────────────────────

    private void checkRegistrationWindow() {
        try {
            ResultSet rs = db.getRegistrationWindow(SEMESTER);
            if (rs.next()) {
                regOpen = true;
                String open  = rs.getString("open_date");
                String close = rs.getString("close_date");
                windowStatusLabel.setText(
                        "  Registration Open");
                windowStatusLabel.setStyle(
                        "-fx-text-fill: #006B3F; "
                                + "-fx-font-weight: bold;");
                windowDatesLabel.setText(
                        open + "  →  " + close);
                statusBanner.setVisible(true);
                statusBanner.setManaged(true);
                bannerLabel.setText(
                        "Course registration is open for "
                                + SEMESTER);
            } else {
                regOpen = false;
                windowStatusLabel.setText(
                        "  Registration Closed");
                windowStatusLabel.setStyle(
                        "-fx-text-fill: #c0392b; "
                                + "-fx-font-weight: bold;");
                windowDatesLabel.setText(
                        "Contact the Academic Office.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Available Courses ─────────────────────────────────────────

    private void loadAvailableCourses() {
        allAvailable = FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getAvailableCoursesForRegistration(
                    studentId, SEMESTER);
            while (rs.next()) {
                String lecturer =
                        (rs.getString("lecturer_title") != null
                                ? rs.getString("lecturer_title")
                                + " " : "")
                                + (rs.getString("lecturer_name") != null
                                ? rs.getString("lecturer_name")
                                : "TBA");

                int enrolled =
                        rs.getInt("already_enrolled");
                String regStatus =
                        rs.getString("reg_status");

                String status = enrolled == 1
                        ? "Enrolled"
                        : "APPROVED".equals(regStatus)
                        ? " Registered"
                        : "DROPPED".equals(regStatus)
                        ? " Dropped"
                        : "Available";

                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(rs.getInt("credits") + " cr");
                row.add(lecturer);
                row.add(status);
                // store course id at index 5
                row.add(String.valueOf(rs.getInt("id")));
                allAvailable.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        availableTable.setItems(allAvailable);
    }

    // ── Registered Courses ────────────────────────────────────────

    private void loadRegisteredCourses() {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        int totalCredits = 0;

        try {
            ResultSet rs = db.getRegisteredCourses(
                    studentId, SEMESTER);
            while (rs.next()) {
                if ("DROPPED".equals(rs.getString("status")))
                    continue;

                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(" " + rs.getString("status"));
                rows.add(row);
                totalCredits += rs.getInt("credits");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        registeredTable.setItems(rows);
        totalCreditsLabel.setText(
                String.valueOf(totalCredits));
        totalCoursesLabel.setText(
                String.valueOf(rows.size()));
        creditSummaryLabel.setText(
                totalCredits + " credits registered");
    }

    // ── Search ────────────────────────────────────────────────────

    private void setupSearch() {
        searchField.textProperty().addListener(
                (obs, o, val) -> {
                    if (val == null || val.isBlank()) {
                        availableTable.setItems(allAvailable);
                        return;
                    }
                    ObservableList<ObservableList<String>> filtered =
                            FXCollections.observableArrayList();
                    allAvailable.forEach(row -> {
                        if (row.get(0).toLowerCase()
                                .contains(val.toLowerCase())
                                || row.get(1).toLowerCase()
                                .contains(val.toLowerCase()))
                            filtered.add(row);
                    });
                    availableTable.setItems(filtered);
                });
    }

    // ── Register ──────────────────────────────────────────────────

    @FXML
    private void handleRegister() {
        if (!regOpen) {
            showFeedback(
                    " Registration is currently closed.",
                    false);
            return;
        }

        ObservableList<String> selected =
                availableTable.getSelectionModel()
                        .getSelectedItem();
        if (selected == null) {
            showFeedback(
                    "Please select a course to register.",
                    false);
            return;
        }

        if (selected.get(4).contains("Enrolled")
                || selected.get(4).contains("Registered")) {
            showFeedback(
                    "Already registered for "
                            + selected.get(0) + ".", false);
            return;
        }

        try {
            int courseId = Integer.parseInt(selected.get(5));
            db.registerCourse(studentId, courseId, SEMESTER);
            showFeedback(
                    "  Successfully registered for "
                            + selected.get(0) + " — "
                            + selected.get(1), true);
            loadAvailableCourses();
            loadRegisteredCourses();
        } catch (Exception e) {
            e.printStackTrace();
            showFeedback("Error: " + e.getMessage(), false);
        }
    }

    // ── Drop ──────────────────────────────────────────────────────

    @FXML
    private void handleDrop() {
        if (!regOpen) {
            showFeedback(
                    "  Registration is closed. "
                            + "Cannot drop courses.", false);
            return;
        }

        ObservableList<String> selected =
                availableTable.getSelectionModel()
                        .getSelectedItem();
        if (selected == null) {
            showFeedback(
                    "  Please select a course to drop.",
                    false);
            return;
        }

        if (!selected.get(4).contains("Registered")
                && !selected.get(4).contains("Enrolled")) {
            showFeedback(
                    "  You are not registered for "
                            + selected.get(0) + ".", false);
            return;
        }

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Drop Course");
        confirm.setHeaderText("Drop "
                + selected.get(0) + " — "
                + selected.get(1) + "?");
        confirm.setContentText(
                "This will remove the course from "
                        + "your registration.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    int courseId = Integer.parseInt(
                            selected.get(5));
                    db.dropCourse(studentId, courseId,
                            SEMESTER);
                    showFeedback(
                            "  Dropped: "
                                    + selected.get(0), false);
                    loadAvailableCourses();
                    loadRegisteredCourses();
                } catch (Exception e) {
                    e.printStackTrace();
                    showFeedback(
                            "Error: " + e.getMessage(),
                            false);
                }
            }
        });
    }

    // ── Helper ────────────────────────────────────────────────────

    private void showFeedback(String msg, boolean success) {
        regFeedbackLabel.setText(msg);
        regFeedbackLabel.setStyle(success
                ? "-fx-text-fill: #006B3F;"
                : "-fx-text-fill: #c0392b;");
    }
}