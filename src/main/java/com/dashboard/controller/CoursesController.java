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

public class CoursesController {

    @FXML private TableView<ObservableList<String>> coursesTable;
    @FXML private TableColumn<ObservableList<String>, String> codeCol;
    @FXML private TableColumn<ObservableList<String>, String> nameCol;
    @FXML private TableColumn<ObservableList<String>, String> creditsCol;
    @FXML private TableColumn<ObservableList<String>, String> instructorCol;
    @FXML private TableColumn<ObservableList<String>, String> scheduleCol;
    @FXML private TableColumn<ObservableList<String>, String> progressCol;

    @FXML private VBox  detailCard;
    @FXML private Label detailCode;
    @FXML private Label detailName;
    @FXML private Label detailLecturer;
    @FXML private Label detailCredits;
    @FXML private Label detailDesc;
    @FXML private Label semesterLabel;

    private DatabaseManager db;
    private int             studentId;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        studentId = SessionManager.getInstance().getRoleSpecificId();

        setupTable();
        loadCourses();
    }

    private void setupTable() {
        codeCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        nameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        creditsCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        instructorCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        scheduleCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));
        progressCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(5)));

        // show detail card on row click
        coursesTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, old, selected) -> {
                    if (selected != null)
                        showDetail(selected);
                });
    }

    private void loadCourses() {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getCoursesForStudent(studentId);
            while (rs.next()) {
                // calculate assignment progress
                int courseId = rs.getInt("id");
                int total = 0, submitted = 0;
                ResultSet as = db.getAssignmentsForCourse(
                        courseId, studentId);
                while (as.next()) {
                    total++;
                    if (as.getInt("submitted") == 1) submitted++;
                }
                String progress = total == 0 ? "No assignments"
                        : submitted + "/" + total + " done";

                String lecturer =
                        (rs.getString("lecturer_title") != null
                                ? rs.getString("lecturer_title") + " "
                                : "")
                                + (rs.getString("lecturer_name") != null
                                ? rs.getString("lecturer_name")
                                : "TBA");

                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(rs.getInt("credits") + " cr");
                row.add(lecturer);
                row.add(rs.getString("semester"));
                row.add(progress);
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        coursesTable.setItems(rows);
    }

    private void showDetail(ObservableList<String> row) {
        detailCode.setText(row.get(0));
        detailName.setText(row.get(1));
        detailLecturer.setText(row.get(3));
        detailCredits.setText(row.get(2));

        // fetch description
        try {
            ResultSet rs = db.getConnection().prepareStatement(
                    "SELECT description FROM courses WHERE code = '"
                            + row.get(0) + "'").executeQuery();
            if (rs.next())
                detailDesc.setText(rs.getString("description"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        detailCard.setVisible(true);
        detailCard.setManaged(true);
    }
}