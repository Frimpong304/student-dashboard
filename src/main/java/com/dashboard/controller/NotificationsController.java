package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.sql.ResultSet;
import java.time.LocalDate;

public class NotificationsController {

    @FXML private ListView<String> overdueList;
    @FXML private ListView<String> upcomingList;
    @FXML private ListView<String> announcementsList;

    private DatabaseManager db;
    private int             studentId;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        studentId = SessionManager.getInstance().getRoleSpecificId();

        loadOverdue();
        loadUpcoming();
        loadAnnouncements();
    }

    private void loadOverdue() {
        ObservableList<String> items =
                FXCollections.observableArrayList();
        try {
            ResultSet courses = db.getCoursesForStudent(studentId);
            while (courses.next()) {
                int    courseId   = courses.getInt("id");
                String courseCode = courses.getString("code");

                // overdue assignments
                ResultSet as = db.getAssignmentsForCourse(
                        courseId, studentId);
                while (as.next()) {
                    if (as.getInt("submitted") == 0) {
                        String due = as.getString("due_date");
                        if (due != null && LocalDate.parse(due)
                                .isBefore(LocalDate.now())) {
                            items.add("  [" + courseCode + "]  "
                                    + as.getString("title")
                                    + "  — Due: " + due);
                        }
                    }
                }

                // overdue quizzes
                ResultSet qs = db.getQuizzesForCourse(
                        courseId, studentId);
                while (qs.next()) {
                    if (qs.getInt("completed") == 0) {
                        String due = qs.getString("due_date");
                        if (due != null && LocalDate.parse(due)
                                .isBefore(LocalDate.now())) {
                            items.add("  [" + courseCode + "]  "
                                    + qs.getString("title")
                                    + "  — Due: " + due);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (items.isEmpty())
            items.add(" No overdue items. Great work!");
        overdueList.setItems(items);
    }

    private void loadUpcoming() {
        ObservableList<String> items =
                FXCollections.observableArrayList();
        LocalDate today   = LocalDate.now();
        LocalDate in7Days = today.plusDays(7);

        try {
            ResultSet courses = db.getCoursesForStudent(studentId);
            while (courses.next()) {
                int    courseId   = courses.getInt("id");
                String courseCode = courses.getString("code");

                ResultSet as = db.getAssignmentsForCourse(
                        courseId, studentId);
                while (as.next()) {
                    if (as.getInt("submitted") == 0) {
                        String dueStr = as.getString("due_date");
                        if (dueStr == null) continue;
                        LocalDate due = LocalDate.parse(dueStr);
                        if (!due.isBefore(today)
                                && !due.isAfter(in7Days)) {
                            long days = today.until(due,
                                    java.time.temporal.ChronoUnit.DAYS);
                            items.add("  [" + courseCode + "]  "
                                    + as.getString("title")
                                    + "  — "
                                    + (days == 0 ? "Due today!"
                                    : days == 1 ? "Due tomorrow"
                                    : "Due in " + days + " days"));
                        }
                    }
                }

                ResultSet qs = db.getQuizzesForCourse(
                        courseId, studentId);
                while (qs.next()) {
                    if (qs.getInt("completed") == 0) {
                        String dueStr = qs.getString("due_date");
                        if (dueStr == null) continue;
                        LocalDate due = LocalDate.parse(dueStr);
                        if (!due.isBefore(today)
                                && !due.isAfter(in7Days)) {
                            long days = today.until(due,
                                    java.time.temporal.ChronoUnit.DAYS);
                            items.add("  [" + courseCode + "]  "
                                    + qs.getString("title")
                                    + "  — "
                                    + (days == 0 ? "Due today!"
                                    : days == 1 ? "Due tomorrow"
                                    : "Due in " + days + " days"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (items.isEmpty())
            items.add("  Nothing due in the next 7 days!");
        upcomingList.setItems(items);
    }

    private void loadAnnouncements() {
        ObservableList<String> items =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getAnnouncementsForStudent(studentId);
            while (rs.next()) {
                items.add(" ["
                        + rs.getString("course_code") + "]  "
                        + rs.getString("lecturer_title") + " "
                        + rs.getString("lecturer_name")
                        + "  —  "
                        + rs.getString("title")
                        + "\n        "
                        + rs.getString("body")
                        + "  (" + rs.getString("posted_at") + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (items.isEmpty())
            items.add("No announcements yet.");
        announcementsList.setItems(items);
    }
}