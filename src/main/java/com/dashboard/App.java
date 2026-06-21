package com.dashboard;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static final String APP_TITLE  = "UCC Student Portal";
    private static final double MIN_WIDTH  = 1100;
    private static final double MIN_HEIGHT = 700;

    @Override
    public void start(Stage stage) {
        try {
            // initialize SQLite database first
            DatabaseManager.getInstance().initialize();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load(), MIN_WIDTH, MIN_HEIGHT);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css")
                            .toExternalForm());

            stage.setTitle(APP_TITLE);
            stage.setScene(scene);
            stage.setMinWidth(MIN_WIDTH);
            stage.setMinHeight(MIN_HEIGHT);
            stage.centerOnScreen();
            stage.setOnCloseRequest(e -> {
                SessionManager.getInstance().clearSession();
                DatabaseManager.getInstance().closeConnection();
            });
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Startup failed: " + e.getMessage())
                    .showAndWait();
        }
    }

    public static void main(String[] args) { launch(args); }
}