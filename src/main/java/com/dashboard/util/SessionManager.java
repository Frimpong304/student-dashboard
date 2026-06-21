package com.dashboard.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Holds the logged-in user session.
 * Auto-logs out after 30 minutes of inactivity.
 */
public class SessionManager {

    private static final int TIMEOUT_MINUTES = 30;
    private static SessionManager instance;

    private int      userId;
    private String   name;
    private String   role;
    private String   email;
    private int      roleSpecificId;
    private long     loginTimestamp;
    private long     lastActivityTime;
    private Timeline timeoutTimer;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null)
            instance = new SessionManager();
        return instance;
    }

    public void startSession(int userId, String name,
                             String role, String email) {
        this.userId           = userId;
        this.name             = name;
        this.role             = role;
        this.email            = email;
        this.loginTimestamp   = System.currentTimeMillis();
        this.lastActivityTime = System.currentTimeMillis();
        startTimeoutTimer();
        System.out.println("Session started: "
                + name + " [" + role + "]");
    }

    // ── Timeout ───────────────────────────────────────────────────

    private void startTimeoutTimer() {
        if (timeoutTimer != null) timeoutTimer.stop();

        timeoutTimer = new Timeline(new KeyFrame(
                Duration.minutes(1), e -> checkTimeout()));
        timeoutTimer.setCycleCount(Timeline.INDEFINITE);
        timeoutTimer.play();
    }

    private void checkTimeout() {
        long idleMinutes = (System.currentTimeMillis()
                - lastActivityTime) / 60000;
        if (idleMinutes >= TIMEOUT_MINUTES) {
            Platform.runLater(this::autoLogout);
        }
    }

    /**
     * Call this on any user interaction to reset the timer.
     */
    public void recordActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    private void autoLogout() {
        System.out.println("Session timed out: " + name);
        clearSession();
        try {
            for (Window w : Stage.getWindows()) {
                if (w instanceof Stage stage
                        && stage.isShowing()) {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/Login.fxml"));
                    Scene scene = new Scene(
                            loader.load(), 1100, 700);
                    scene.getStylesheets().add(
                            getClass().getResource(
                                            "/css/style.css")
                                    .toExternalForm());
                    stage.setScene(scene);
                    stage.centerOnScreen();

                    // show timeout message
                    javafx.scene.control.Alert alert =
                            new javafx.scene.control.Alert(
                                    javafx.scene.control
                                            .Alert.AlertType.INFORMATION);
                    alert.setTitle("Session Expired");
                    alert.setHeaderText("You have been logged out.");
                    alert.setContentText(
                            "Your session expired after "
                                    + TIMEOUT_MINUTES
                                    + " minutes of inactivity.");
                    alert.showAndWait();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Getters ───────────────────────────────────────────────────

    public int    getUserId()         { return userId; }
    public String getName()           { return name; }
    public String getRole()           { return role; }
    public String getEmail()          { return email; }
    public int    getRoleSpecificId() { return roleSpecificId; }
    public boolean isLoggedIn()       { return name != null; }
    public boolean isStudent()        { return "STUDENT".equals(role); }
    public boolean isLecturer()       { return "LECTURER".equals(role); }
    public boolean isAdmin()          { return "ADMIN".equals(role); }

    public void setRoleSpecificId(int id) {
        this.roleSpecificId = id;
    }

    public String getFirstName() {
        if (name == null || name.isBlank()) return "";
        return name.trim().split("\\s+")[0];
    }

    public long getSessionDurationMinutes() {
        return (System.currentTimeMillis()
                - loginTimestamp) / 60000;
    }

    public void clearSession() {
        if (timeoutTimer != null) {
            timeoutTimer.stop();
            timeoutTimer = null;
        }
        System.out.println("Session cleared: " + name);
        userId         = 0;
        name           = null;
        role           = null;
        email          = null;
        roleSpecificId = 0;
        loginTimestamp = 0;
        lastActivityTime = 0;
    }
}