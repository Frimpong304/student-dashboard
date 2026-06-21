package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;

import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        passwordField.setOnAction(e -> handleLogin());
    }

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter your email and password.");
            return;
        }

        try {
            // authenticate against database
            ResultSet rs = DatabaseManager.getInstance()
                    .authenticateUser(email, password);

            if (rs == null) {
                showError("Invalid email or password.");
                passwordField.clear();
                return;
            }

            // get role and route to correct dashboard
            String role = rs.getString("role");
            int    userId = rs.getInt("id");
            String name   = rs.getString("name");

            // store session
            SessionManager.getInstance().startSession(userId, name, role, email);

            // route based on role
            switch (role) {
                case "STUDENT"  -> loadScreen("/fxml/Dashboard.fxml",
                        "UCC Portal — Student");
                case "LECTURER" -> loadScreen("/fxml/Lecturer.fxml",
                        "UCC Portal — Lecturer");
                case "ADMIN"    -> loadScreen("/fxml/Admin.fxml",
                        "UCC Portal — Admin");
                default         -> showError("Unknown role. Contact admin.");
            }

        } catch (Exception e) {
            showError("Login error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadScreen(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1100, 700);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css")
                            .toExternalForm());
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
        } catch (Exception e) {
            showError("Failed to load screen. " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText("⚠  " + message);
        errorLabel.setVisible(true);
        TranslateTransition shake =
                new TranslateTransition(Duration.millis(60), errorLabel);
        shake.setByX(8);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.play();
    }
}