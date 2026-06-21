package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class TimetableController {

    @FXML private TableView<ObservableList<String>> timetableTable;
    @FXML private TableColumn<ObservableList<String>, String> ttTypeCol;
    @FXML private TableColumn<ObservableList<String>, String> ttDateCol;
    @FXML private TableColumn<ObservableList<String>, String> ttStartCol;
    @FXML private TableColumn<ObservableList<String>, String> ttEndCol;
    @FXML private TableColumn<ObservableList<String>, String> ttCodeCol;
    @FXML private TableColumn<ObservableList<String>, String> ttNameCol;
    @FXML private TableColumn<ObservableList<String>, String> ttVenueCol;
    @FXML private TableColumn<ObservableList<String>, String> ttCountdownCol;

    @FXML private ComboBox<String> examTypeFilter;
    @FXML private Label            examCountLabel;
    @FXML private VBox             upcomingAlert;
    @FXML private Label            upcomingAlertLabel;
    @FXML private Label            upcomingAlertSub;

    private DatabaseManager                        db;
    private int                                    studentId;
    private ObservableList<ObservableList<String>> allRows;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        studentId = SessionManager.getInstance()
                .getRoleSpecificId();

        setupTable();
        setupFilter();
        loadExams();
    }

    // ── Table Setup ───────────────────────────────────────────────

    private void setupTable() {
        ttTypeCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        ttDateCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        ttStartCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        ttEndCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));
        ttCodeCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(4)));
        ttNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(5)));
        ttVenueCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(6)));
        ttCountdownCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(7)));
    }

    // ── Filter ────────────────────────────────────────────────────

    private void setupFilter() {
        examTypeFilter.getItems().addAll(
                "All", "Mid-Semester", "Final", "Resit");
        examTypeFilter.setValue("All");
        examTypeFilter.setOnAction(e -> applyFilter());
    }

    private void applyFilter() {
        String filter = examTypeFilter.getValue();
        if (filter == null || filter.equals("All")) {
            timetableTable.setItems(allRows);
            examCountLabel.setText(
                    allRows.size() + " exams");
            return;
        }
        ObservableList<ObservableList<String>> filtered =
                FXCollections.observableArrayList();
        allRows.forEach(row -> {
            if (row.get(0).contains(filter))
                filtered.add(row);
        });
        timetableTable.setItems(filtered);
        examCountLabel.setText(filtered.size() + " exams");
    }

    // ── Load Exams ────────────────────────────────────────────────

    private void loadExams() {
        allRows = FXCollections.observableArrayList();
        LocalDate today     = LocalDate.now();
        String    nextExam  = null;
        String    nextCode  = null;
        long      minDays   = Long.MAX_VALUE;

        try {
            ResultSet rs = db.getExamsForStudent(studentId);
            while (rs.next()) {
                String dateStr = rs.getString("exam_date");
                LocalDate examDate = LocalDate.parse(dateStr);
                long daysUntil =
                        ChronoUnit.DAYS.between(today, examDate);

                String countdown = daysUntil < 0
                        ? "Completed"
                        : daysUntil == 0
                        ? "TODAY!"
                        : daysUntil == 1
                        ? "Tomorrow"
                        : "In " + daysUntil + " days";

                // track nearest upcoming exam
                if (daysUntil >= 0 && daysUntil < minDays) {
                    minDays  = daysUntil;
                    nextExam = dateStr + " at "
                            + rs.getString("start_time")
                            + " — " + rs.getString("venue");
                    nextCode = rs.getString("code")
                            + " " + rs.getString("name");
                }

                String typeIcon =
                        "Final".equals(rs.getString("type"))
                                ? " Final"
                                : "Mid-Semester".equals(
                                rs.getString("type"))
                                ? " Mid Sem"
                                : " Resit";

                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(typeIcon);
                row.add(dateStr);
                row.add(rs.getString("start_time"));
                row.add(rs.getString("end_time"));
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(rs.getString("venue"));
                row.add(countdown);
                allRows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        timetableTable.setItems(allRows);
        examCountLabel.setText(allRows.size() + " exams");

        // show upcoming alert
        if (nextExam != null && minDays <= 7) {
            upcomingAlert.setVisible(true);
            upcomingAlert.setManaged(true);
            upcomingAlertLabel.setText(
                    "  Upcoming Exam: " + nextCode);
            upcomingAlertSub.setText(
                    nextExam + (minDays == 0
                            ? " — TODAY!"
                            : " — in " + minDays + " day(s)"));
        }
    }
}