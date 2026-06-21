package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.sql.ResultSet;

public class GradesController {

    @FXML private TableView<ObservableList<String>> gradesTable;
    @FXML private TableColumn<ObservableList<String>, String> gCodeCol;
    @FXML private TableColumn<ObservableList<String>, String> gNameCol;
    @FXML private TableColumn<ObservableList<String>, String> gScoreCol;
    @FXML private TableColumn<ObservableList<String>, String> gLetterCol;
    @FXML private TableColumn<ObservableList<String>, String> gPointsCol;
    @FXML private TableColumn<ObservableList<String>, String> gSemCol;

    @FXML private BarChart<String, Number> gradesChart;
    @FXML private Label gpaLabel;
    @FXML private Label gpaRemark;
    @FXML private Label coursesGradedLabel;
    @FXML private Label highestLabel;
    @FXML private Label lowestLabel;

    private DatabaseManager db;
    private int             studentId;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        studentId = SessionManager.getInstance().getRoleSpecificId();

        setupTable();
        loadGrades();
    }

    private void setupTable() {
        gCodeCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        gNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        gScoreCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        gLetterCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        gPointsCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));
        gSemCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(5)));
    }

    private void loadGrades() {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        XYChart.Series<String, Number> series =
                new XYChart.Series<>();

        double totalPoints = 0;
        double highest     = -1;
        double lowest      = 101;
        int    count       = 0;

        try {
            ResultSet rs = db.getGradesForStudent(studentId);
            while (rs.next()) {
                double score  = rs.getDouble("score");
                String letter = rs.getString("letter_grade");
                double points = score >= 90 ? 4.0
                        : score >= 80 ? 3.0
                        : score >= 70 ? 2.0
                        : score >= 60 ? 1.0 : 0.0;

                totalPoints += points;
                count++;
                if (score > highest) highest = score;
                if (score < lowest)  lowest  = score;

                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(String.format("%.1f", score));
                row.add(letter);
                row.add(String.format("%.1f", points));
                row.add(rs.getString("semester"));
                rows.add(row);

                series.getData().add(new XYChart.Data<>(
                        rs.getString("code"), score));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        gradesTable.setItems(rows);
        coursesGradedLabel.setText(String.valueOf(count));

        if (count > 0) {
            double gpa = Math.round(
                    (totalPoints / count) * 100.0) / 100.0;
            gpaLabel.setText(String.format("%.2f", gpa));
            gpaRemark.setText(getGPARemark(gpa));
            highestLabel.setText(
                    String.format("%.1f", highest));
            lowestLabel.setText(
                    String.format("%.1f", lowest));
        }

        gradesChart.getData().clear();
        gradesChart.getData().add(series);

        // color bars after render
        Platform.runLater(() -> {
            for (XYChart.Data<String, Number> d
                    : series.getData()) {
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

    private String getGPARemark(double gpa) {
        if (gpa >= 3.7) return "First Class ";
        if (gpa >= 3.0) return "Second Class Upper ";
        if (gpa >= 2.0) return "Second Class Lower";
        if (gpa >= 1.0) return "Third Class";
        return "Fail";
    }
}