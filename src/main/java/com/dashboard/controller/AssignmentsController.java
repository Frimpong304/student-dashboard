package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.ResultSet;
import java.time.LocalDate;

public class AssignmentsController {

    @FXML private TableView<ObservableList<String>> assignmentsTable;
    @FXML private TableColumn<ObservableList<String>, String> aCourseCol;
    @FXML private TableColumn<ObservableList<String>, String> aTitleCol;
    @FXML private TableColumn<ObservableList<String>, String> aDueCol;
    @FXML private TableColumn<ObservableList<String>, String> aPriorityCol;
    @FXML private TableColumn<ObservableList<String>, String> aStatusCol;
    @FXML private TableColumn<ObservableList<String>, String> aScoreCol;

    @FXML private ComboBox<String> filterCombo;
    @FXML private TextField        searchField;
    @FXML private Label            submitFeedbackLabel;

    @FXML private Label totalLabel;
    @FXML private Label submittedLabel;
    @FXML private Label pendingLabel;
    @FXML private Label overdueLabel;

    private DatabaseManager                         db;
    private int                                     studentId;
    private ObservableList<ObservableList<String>>  allRows;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        studentId = SessionManager.getInstance().getRoleSpecificId();

        setupTable();
        setupFilter();
        loadAssignments();
        setupSearch();
    }

    private void setupTable() {
        aCourseCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        aTitleCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        aDueCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        aPriorityCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        aStatusCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));
        aScoreCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(5)));
    }

    private void setupFilter() {
        filterCombo.getItems().addAll(
                "All", "Pending", "Submitted", "Overdue");
        filterCombo.setValue("All");
        filterCombo.setOnAction(e -> applyFilter());
    }

    private void loadAssignments() {
        allRows = FXCollections.observableArrayList();
        int total = 0, submitted = 0, pending = 0, overdue = 0;

        try {
            ResultSet courses = db.getCoursesForStudent(studentId);
            while (courses.next()) {
                int    courseId   = courses.getInt("id");
                String courseCode = courses.getString("code");

                ResultSet as = db.getAssignmentsForCourse(
                        courseId, studentId);
                while (as.next()) {
                    total++;
                    boolean isSubmitted =
                            as.getInt("submitted") == 1;
                    String  dueStr      =
                            as.getString("due_date");
                    LocalDate due       =
                            dueStr != null
                                    ? LocalDate.parse(dueStr)
                                    : null;
                    boolean isOverdue   =
                            !isSubmitted && due != null
                                    && due.isBefore(LocalDate.now());

                    if (isSubmitted) submitted++;
                    else if (isOverdue) overdue++;
                    else pending++;

                    String status = isSubmitted
                            ? " Submitted"
                            : isOverdue
                            ? " Overdue"
                            : due != null
                            && due.equals(LocalDate.now())
                            ? " Due Today"
                            : " Pending";

                    double score = as.getDouble("score");
                    String scoreStr = isSubmitted && score > 0
                            ? String.format("%.1f", score)
                            : "--";

                    ObservableList<String> row =
                            FXCollections.observableArrayList();
                    row.add(courseCode);
                    row.add(as.getString("title"));
                    row.add(dueStr != null ? dueStr : "N/A");
                    row.add(as.getString("priority"));
                    row.add(status);
                    row.add(scoreStr);
                    // store assignment id and submission status
                    // at index 6 and 7 for submit action
                    row.add(String.valueOf(as.getInt("id")));
                    row.add(String.valueOf(isSubmitted));
                    allRows.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        totalLabel.setText(String.valueOf(total));
        submittedLabel.setText(String.valueOf(submitted));
        pendingLabel.setText(String.valueOf(pending));
        overdueLabel.setText(String.valueOf(overdue));

        assignmentsTable.setItems(allRows);
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, val) -> {
            if (val == null || val.isBlank()) {
                applyFilter();
                return;
            }
            ObservableList<ObservableList<String>> filtered =
                    FXCollections.observableArrayList();
            allRows.forEach(row -> {
                if (row.get(1).toLowerCase()
                        .contains(val.toLowerCase())
                        || row.get(0).toLowerCase()
                        .contains(val.toLowerCase()))
                    filtered.add(row);
            });
            assignmentsTable.setItems(filtered);
        });
    }

    private void applyFilter() {
        String filter = filterCombo.getValue();
        if (filter == null || filter.equals("All")) {
            assignmentsTable.setItems(allRows);
            return;
        }
        ObservableList<ObservableList<String>> filtered =
                FXCollections.observableArrayList();
        allRows.forEach(row -> {
            String status = row.get(4);
            switch (filter) {
                case "Submitted" -> {
                    if (status.contains("Submitted"))
                        filtered.add(row);
                }
                case "Pending" -> {
                    if (status.contains("Pending")
                            || status.contains("Due Today"))
                        filtered.add(row);
                }
                case "Overdue" -> {
                    if (status.contains("Overdue"))
                        filtered.add(row);
                }
            }
        });
        assignmentsTable.setItems(filtered);
    }

    @FXML
    private void handleSubmit() {
        ObservableList<String> selected =
                assignmentsTable.getSelectionModel()
                        .getSelectedItem();
        if (selected == null) {
            submitFeedbackLabel.setText(
                    "⚠  Please select an assignment.");
            submitFeedbackLabel.setStyle(
                    "-fx-text-fill: #e74c3c;");
            return;
        }
        boolean alreadySubmitted =
                Boolean.parseBoolean(selected.get(7));
        if (alreadySubmitted) {
            submitFeedbackLabel.setText(
                    "⚠  Already submitted.");
            submitFeedbackLabel.setStyle(
                    "-fx-text-fill: #e67e22;");
            return;
        }
        try {
            int assignmentId = Integer.parseInt(selected.get(6));
            db.submitAssignment(assignmentId, studentId);
            submitFeedbackLabel.setText(
                    "Submitted successfully!");
            submitFeedbackLabel.setStyle(
                    "-fx-text-fill: #2ecc71;");
            loadAssignments();
        } catch (Exception e) {
            e.printStackTrace();
            submitFeedbackLabel.setText(
                    "Error: " + e.getMessage());
        }
    }
}