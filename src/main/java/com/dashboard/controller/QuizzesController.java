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

public class QuizzesController {

    @FXML private TableView<ObservableList<String>> quizzesTable;
    @FXML private TableColumn<ObservableList<String>, String> qCourseCol;
    @FXML private TableColumn<ObservableList<String>, String> qTitleCol;
    @FXML private TableColumn<ObservableList<String>, String> qDueCol;
    @FXML private TableColumn<ObservableList<String>, String> qDurationCol;
    @FXML private TableColumn<ObservableList<String>, String> qQuestionsCol;
    @FXML private TableColumn<ObservableList<String>, String> qStatusCol;

    @FXML private ComboBox<String> quizFilterCombo;
    @FXML private Label qTotalLabel;
    @FXML private Label qCompletedLabel;
    @FXML private Label qPendingLabel;
    @FXML private Label qAvgLabel;

    private DatabaseManager                        db;
    private int                                    studentId;
    private ObservableList<ObservableList<String>> allRows;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        studentId = SessionManager.getInstance().getRoleSpecificId();

        setupTable();
        setupFilter();
        loadQuizzes();
    }

    private void setupTable() {
        qCourseCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        qTitleCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        qDueCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        qDurationCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        qQuestionsCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));
        qStatusCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(5)));
    }

    private void setupFilter() {
        quizFilterCombo.getItems().addAll(
                "All", "Completed", "Pending", "Overdue");
        quizFilterCombo.setValue("All");
        quizFilterCombo.setOnAction(e -> applyFilter());
    }

    private void loadQuizzes() {
        allRows = FXCollections.observableArrayList();
        int    total = 0, completed = 0, pending = 0;
        double totalScore = 0;
        int    scoredCount = 0;

        try {
            ResultSet courses = db.getCoursesForStudent(studentId);
            while (courses.next()) {
                int    courseId   = courses.getInt("id");
                String courseCode = courses.getString("code");

                ResultSet qs = db.getQuizzesForCourse(
                        courseId, studentId);
                while (qs.next()) {
                    total++;
                    boolean isCompleted =
                            qs.getInt("completed") == 1;
                    String  dueStr      =
                            qs.getString("due_date");
                    LocalDate due = dueStr != null
                            ? LocalDate.parse(dueStr) : null;
                    boolean isOverdue =
                            !isCompleted && due != null
                                    && due.isBefore(LocalDate.now());

                    if (isCompleted) {
                        completed++;
                        double score = qs.getDouble("score");
                        if (score > 0) {
                            totalScore += score;
                            scoredCount++;
                        }
                    } else {
                        pending++;
                    }

                    double score = qs.getDouble("score");
                    String status = isCompleted
                            ? "Completed — "
                            + String.format("%.1f%%", score)
                            : isOverdue ? " Overdue"
                            : due != null
                            && due.equals(LocalDate.now())
                            ? "Due Today"
                            : "Pending";

                    ObservableList<String> row =
                            FXCollections.observableArrayList();
                    row.add(courseCode);
                    row.add(qs.getString("title"));
                    row.add(dueStr != null ? dueStr : "N/A");
                    row.add(qs.getInt("duration_mins") + " min");
                    row.add(qs.getInt("total_questions") + " Qs");
                    row.add(status);
                    allRows.add(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        qTotalLabel.setText(String.valueOf(total));
        qCompletedLabel.setText(String.valueOf(completed));
        qPendingLabel.setText(String.valueOf(pending));
        qAvgLabel.setText(scoredCount > 0
                ? String.format("%.1f%%",
                totalScore / scoredCount)
                : "--");

        quizzesTable.setItems(allRows);
    }

    private void applyFilter() {
        String filter = quizFilterCombo.getValue();
        if (filter == null || filter.equals("All")) {
            quizzesTable.setItems(allRows);
            return;
        }
        ObservableList<ObservableList<String>> filtered =
                FXCollections.observableArrayList();
        allRows.forEach(row -> {
            String status = row.get(5);
            switch (filter) {
                case "Completed" -> {
                    if (status.contains("Completed"))
                        filtered.add(row);
                }
                case "Pending"   -> {
                    if (status.contains("Pending")
                            || status.contains("Due Today"))
                        filtered.add(row);
                }
                case "Overdue"   -> {
                    if (status.contains("Overdue"))
                        filtered.add(row);
                }
            }
        });
        quizzesTable.setItems(filtered);
    }
}