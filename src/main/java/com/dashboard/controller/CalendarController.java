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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CalendarController {

    @FXML private TableView<ObservableList<String>> calendarTable;
    @FXML private TableColumn<ObservableList<String>, String> calDateCol;
    @FXML private TableColumn<ObservableList<String>, String> calCourseCol;
    @FXML private TableColumn<ObservableList<String>, String> calTitleCol;
    @FXML private TableColumn<ObservableList<String>, String> calTypeCol;
    @FXML private TableColumn<ObservableList<String>, String> calStatusCol;

    @FXML private ListView<String> thisWeekList;
    @FXML private Label            monthLabel;

    private DatabaseManager db;
    private int             studentId;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        studentId = SessionManager.getInstance().getRoleSpecificId();

        monthLabel.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("MMMM yyyy")));

        setupTable();
        loadDeadlines();
    }

    private void setupTable() {
        calDateCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        calCourseCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        calTitleCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        calTypeCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        calStatusCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));
    }

    private void loadDeadlines() {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        ObservableList<String> weekItems =
                FXCollections.observableArrayList();

        LocalDate today   = LocalDate.now();
        LocalDate in7Days = today.plusDays(7);

        List<ObservableList<String>> allDeadlines = new ArrayList<>();

        try {
            ResultSet courses = db.getCoursesForStudent(studentId);
            while (courses.next()) {
                int    courseId   = courses.getInt("id");
                String courseCode = courses.getString("code");

                // assignments
                ResultSet as = db.getAssignmentsForCourse(
                        courseId, studentId);
                while (as.next()) {
                    String dueStr = as.getString("due_date");
                    if (dueStr == null) continue;
                    LocalDate due = LocalDate.parse(dueStr);
                    boolean submitted =
                            as.getInt("submitted") == 1;

                    String status = submitted
                            ? " Submitted"
                            : due.isBefore(today)
                            ? " Overdue"
                            : " Pending";

                    ObservableList<String> row =
                            FXCollections.observableArrayList();
                    row.add(dueStr);
                    row.add(courseCode);
                    row.add(as.getString("title"));
                    row.add("Assignment");
                    row.add(status);
                    allDeadlines.add(row);

                    // this week
                    if (!submitted
                            && !due.isBefore(today)
                            && !due.isAfter(in7Days)) {
                        long days = ChronoUnit.DAYS
                                .between(today, due);
                        weekItems.add("  "
                                + courseCode + " — "
                                + as.getString("title")
                                + " ("
                                + (days == 0 ? "today"
                                : days == 1 ? "tomorrow"
                                : "in " + days + "d") + ")");
                    }
                }

                // quizzes
                ResultSet qs = db.getQuizzesForCourse(
                        courseId, studentId);
                while (qs.next()) {
                    String dueStr = qs.getString("due_date");
                    if (dueStr == null) continue;
                    LocalDate due = LocalDate.parse(dueStr);
                    boolean completed =
                            qs.getInt("completed") == 1;

                    String status = completed
                            ? " Completed"
                            : due.isBefore(today)
                            ? "️ Overdue"
                            : " Pending";

                    ObservableList<String> row =
                            FXCollections.observableArrayList();
                    row.add(dueStr);
                    row.add(courseCode);
                    row.add(qs.getString("title"));
                    row.add("Quiz");
                    row.add(status);
                    allDeadlines.add(row);

                    if (!completed
                            && !due.isBefore(today)
                            && !due.isAfter(in7Days)) {
                        long days = ChronoUnit.DAYS
                                .between(today, due);
                        weekItems.add("  "
                                + courseCode + " — "
                                + qs.getString("title")
                                + " ("
                                + (days == 0 ? "today"
                                : days == 1 ? "tomorrow"
                                : "in " + days + "d") + ")");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // sort by date
        allDeadlines.sort(Comparator.comparing(
                r -> r.get(0)));
        rows.addAll(allDeadlines);
        calendarTable.setItems(rows);

        if (weekItems.isEmpty())
            weekItems.add(" Nothing due this week!");
        thisWeekList.setItems(weekItems);
    }
}