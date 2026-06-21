package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;

import java.sql.ResultSet;

public class ProfileController {

    @FXML private Circle       avatarCircle;
    @FXML private Label        profileNameLabel;
    @FXML private Label        profileRoleLabel;
    @FXML private Label        profileNumLabel;

    @FXML private TextField    nameField;
    @FXML private TextField    emailField;
    @FXML private TextField    majorField;
    @FXML private ComboBox<String> yearCombo;
    @FXML private Label        profileFeedback;

    @FXML private PasswordField currentPassField;
    @FXML private PasswordField newPassField;
    @FXML private PasswordField confirmPassField;
    @FXML private ProgressBar   strengthBar;
    @FXML private Label         strengthLabel;
    @FXML private Label         passwordFeedback;

    @FXML private Label infoRole;
    @FXML private Label infoSession;
    @FXML private Label infoEmail;

    private DatabaseManager db;
    private SessionManager  session;
    private int             studentId;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        session   = SessionManager.getInstance();
        studentId = session.getRoleSpecificId();

        yearCombo.getItems().addAll(
                "Year 1 — Freshman",
                "Year 2 — Sophomore",
                "Year 3 — Junior",
                "Year 4 — Senior");

        loadProfile();
        setupPasswordStrength();
        setupAccountInfo();
    }

    // ── Load Profile ──────────────────────────────────────────────

    private void loadProfile() {
        try {
            ResultSet rs = db.getStudentByEmail(
                    session.getEmail());
            if (rs != null && rs.next()) {
                String name = rs.getString("name");
                String major = rs.getString("major");
                int    year  = rs.getInt("year");
                String num   = rs.getString("student_number");

                profileNameLabel.setText(name);
                profileRoleLabel.setText("BSc. Computer Science");
                profileNumLabel.setText(num);

                nameField.setText(name);
                emailField.setText(rs.getString("email"));
                majorField.setText(major);
                yearCombo.getSelectionModel()
                        .select(Math.max(0, year - 1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Save Profile ──────────────────────────────────────────────

    @FXML
    private void handleSaveProfile() {
        String name  = nameField.getText().trim();
        String email = emailField.getText().trim();
        String major = majorField.getText().trim();
        int    year  = yearCombo.getSelectionModel()
                .getSelectedIndex() + 1;

        if (name.isEmpty() || email.isEmpty()
                || major.isEmpty()) {
            showFeedback(profileFeedback,
                    "⚠  All fields are required.", false);
            return;
        }

        if (!email.contains("@")) {
            showFeedback(profileFeedback,
                    "⚠  Invalid email address.", false);
            return;
        }

        try {
            db.updateUserInfo(session.getUserId(), name, email);
            db.updateStudentProfile(studentId, major, year, null);

            profileNameLabel.setText(name);
            showFeedback(profileFeedback,
                    "Profile updated successfully!", true);

        } catch (Exception e) {
            e.printStackTrace();
            showFeedback(profileFeedback,
                    "Error: " + e.getMessage(), false);
        }
    }

    // ── Change Password ───────────────────────────────────────────

    @FXML
    private void handleChangePassword() {
        String current = currentPassField.getText();
        String newPass = newPassField.getText();
        String confirm = confirmPassField.getText();

        if (current.isEmpty() || newPass.isEmpty()
                || confirm.isEmpty()) {
            showFeedback(passwordFeedback,
                    "⚠  All password fields are required.",
                    false);
            return;
        }

        if (newPass.length() < 8) {
            showFeedback(passwordFeedback,
                    "⚠  Password must be at least 8 characters.",
                    false);
            return;
        }

        if (!newPass.equals(confirm)) {
            showFeedback(passwordFeedback,
                    "⚠  Passwords do not match.", false);
            return;
        }

        try {
            boolean success = db.changePassword(
                    session.getUserId(), current, newPass);
            if (success) {
                currentPassField.clear();
                newPassField.clear();
                confirmPassField.clear();
                strengthBar.setProgress(0);
                strengthLabel.setText("");
                showFeedback(passwordFeedback,
                        "Password changed successfully!",
                        true);
            } else {
                showFeedback(passwordFeedback,
                        "⚠  Current password is incorrect.",
                        false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showFeedback(passwordFeedback,
                    "Error: " + e.getMessage(), false);
        }
    }

    // ── Password Strength ─────────────────────────────────────────

    private void setupPasswordStrength() {
        newPassField.textProperty().addListener(
                (obs, o, val) -> updateStrength(val));
    }

    private void updateStrength(String password) {
        if (password == null || password.isEmpty()) {
            strengthBar.setProgress(0);
            strengthLabel.setText("");
            return;
        }

        int score = 0;
        if (password.length() >= 8)  score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[!@#$%^&*].*")) score++;

        double progress = score / 5.0;
        strengthBar.setProgress(progress);

        String[] labels = {
                "", "Very Weak", "Weak", "Fair", "Strong",
                "Very Strong"
        };
        String[] colors = {
                "", "#e74c3c", "#e67e22", "#f1c40f",
                "#2ecc71", "#27ae60"
        };

        strengthLabel.setText(labels[score]);
        strengthBar.setStyle(
                "-fx-accent: " + colors[score] + ";");
    }

    // ── Account Info ──────────────────────────────────────────────

    private void setupAccountInfo() {
        infoRole.setText(session.getRole());
        infoEmail.setText(session.getEmail());
        infoSession.setText(
                session.getSessionDurationMinutes()
                        + " minutes");
    }

    // ── Helper ────────────────────────────────────────────────────

    private void showFeedback(Label label,
                              String message, boolean success) {
        label.setText(message);
        label.setStyle(success
                ? "-fx-text-fill: #2ecc71;"
                : "-fx-text-fill: #e74c3c;");
    }
}