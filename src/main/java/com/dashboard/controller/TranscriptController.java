package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.ResultSet;

public class TranscriptController {

    // Header labels
    @FXML private Label transcriptStudentLabel;
    @FXML private Label transcriptNumLabel;
    @FXML private Label transcriptMajorLabel;
    @FXML private Label cgpaLabel;
    @FXML private Label cgpaRemarkLabel;

    // Semester GPA table
    @FXML private TableView<ObservableList<String>> semesterTable;
    @FXML private TableColumn<ObservableList<String>, String> semCol;
    @FXML private TableColumn<ObservableList<String>, String> semCoursesCol;
    @FXML private TableColumn<ObservableList<String>, String> semPointsCol;
    @FXML private TableColumn<ObservableList<String>, String> semGpaCol;
    @FXML private TableColumn<ObservableList<String>, String> semRemarkCol;

    // Transcript table
    @FXML private TableView<ObservableList<String>> transcriptTable;
    @FXML private TableColumn<ObservableList<String>, String> tSemCol;
    @FXML private TableColumn<ObservableList<String>, String> tCodeCol;
    @FXML private TableColumn<ObservableList<String>, String> tNameCol;
    @FXML private TableColumn<ObservableList<String>, String> tCreditsCol;
    @FXML private TableColumn<ObservableList<String>, String> tScoreCol;
    @FXML private TableColumn<ObservableList<String>, String> tLetterCol;
    @FXML private TableColumn<ObservableList<String>, String> tPointsCol;

    private DatabaseManager db;
    private int             studentId;
    private SessionManager  session;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        session   = SessionManager.getInstance();
        studentId = session.getRoleSpecificId();

        setupTables();
        loadStudentHeader();
        loadSemesterGPA();
        loadTranscript();
    }

    // ── Setup ─────────────────────────────────────────────────────

    private void setupTables() {
        // semester table
        semCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        semCoursesCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        semPointsCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        semGpaCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        semRemarkCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));

        // transcript table
        tSemCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        tCodeCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        tNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        tCreditsCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        tScoreCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));
        tLetterCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(5)));
        tPointsCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(6)));
    }

    // ── Student Header ────────────────────────────────────────────

    private void loadStudentHeader() {
        try {
            ResultSet rs = db.getStudentByEmail(
                    session.getEmail());
            if (rs != null && rs.next()) {
                transcriptStudentLabel.setText(
                        "Student: " + rs.getString("name"));
                transcriptNumLabel.setText(
                        "Student No: "
                                + rs.getString("student_number"));
                transcriptMajorLabel.setText(
                        "Programme: BSc. "
                                + rs.getString("major")
                                + " (Year " + rs.getInt("year") + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Semester GPA ──────────────────────────────────────────────

    private void loadSemesterGPA() {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getSemesterGPAForStudent(studentId);
            while (rs.next()) {
                double gpa = rs.getDouble("semester_gpa");
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("semester"));
                row.add(String.valueOf(
                        rs.getInt("courses_taken")));
                row.add(String.format("%.1f",
                        rs.getDouble("total_points")));
                row.add(String.format("%.2f", gpa));
                row.add(getGPARemark(gpa));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        semesterTable.setItems(rows);
    }

    // ── Full Transcript ───────────────────────────────────────────

    private void loadTranscript() {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        double totalPoints = 0;
        int    count       = 0;

        try {
            ResultSet rs = db.getTranscriptForStudent(studentId);
            while (rs.next()) {
                double points = rs.getDouble("grade_points");
                totalPoints += points;
                count++;

                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("semester"));
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(String.valueOf(rs.getInt("credits")));
                row.add(String.format("%.1f",
                        rs.getDouble("score")));
                row.add(rs.getString("letter_grade"));
                row.add(String.format("%.1f", points));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        transcriptTable.setItems(rows);

        if (count > 0) {
            double cgpa = Math.round(
                    (totalPoints / count) * 100.0) / 100.0;
            cgpaLabel.setText(
                    String.format("CGPA: %.2f", cgpa));
            cgpaRemarkLabel.setText(getGPARemark(cgpa));
        }
    }

    private String getGPARemark(double gpa) {
        if (gpa >= 3.7) return "First Class";
        if (gpa >= 3.0) return "Second Class Upper";
        if (gpa >= 2.0) return "Second Class Lower";
        if (gpa >= 1.0) return "Third Class";
        return "Fail";
    }
}