package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.model.Admin;
import com.dashboard.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminController {

    // Sidebar
    @FXML private Label  adminNameLabel;
    @FXML private Button navDashboard;
    @FXML private Button navStudents;
    @FXML private Button navLecturers;
    @FXML private Button navCourses;
    @FXML private Button navEnrollments;
    @FXML private Button navReports;
    @FXML private Button navApprovals;
    @FXML private Button navFeeManagement;
    @FXML private Button navBackup;

    // Top bar
    @FXML private Label pageTitle;

    // Welcome
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;

    // Stat cards
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalLecturersLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label totalEnrollmentsLabel;

    // Tables
    @FXML private TableView<ObservableList<String>> recentStudentsTable;
    @FXML private TableColumn<ObservableList<String>, String> rsNumCol;
    @FXML private TableColumn<ObservableList<String>, String> rsNameCol;
    @FXML private TableColumn<ObservableList<String>, String> rsMajorCol;
    @FXML private TableColumn<ObservableList<String>, String> rsYearCol;
    @FXML private TableColumn<ObservableList<String>, String> rsEmailCol;

    @FXML private TableView<ObservableList<String>> recentLecturersTable;
    @FXML private TableColumn<ObservableList<String>, String> rlNameCol;
    @FXML private TableColumn<ObservableList<String>, String> rlTitleCol;
    @FXML private TableColumn<ObservableList<String>, String> rlDeptCol;
    @FXML private TableColumn<ObservableList<String>, String> rlEmailCol;

    // Content & status
    @FXML private VBox  contentArea;
    @FXML private Label statusLabel;
    @FXML private Label statsLabel;

    private Admin           currentAdmin;
    private DatabaseManager db;
    private Button          activeNavButton;
    @FXML private void handleNavApprovals() {
        pageTitle.setText("Grade Approvals");
        setActiveNav(navApprovals);
        showGradeApprovalsView();
    }

    @FXML private void handleNavFeeManagement() {
        pageTitle.setText("Fee Management");
        setActiveNav(navFeeManagement);
        showFeeManagementView();
    }

    @FXML private void handleNavBackup() {
        pageTitle.setText("Backup & Restore");
        setActiveNav(navBackup);
        showBackupView();
    }

// ── Grade Approvals View ──────────────────────────────────────

    private void showGradeApprovalsView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        TableView<ObservableList<String>> table =
                new TableView<>();
        table.setPrefHeight(400);

        TableColumn<ObservableList<String>, String> idCol =
                new TableColumn<>("ID");
        TableColumn<ObservableList<String>, String> courseCol =
                new TableColumn<>("Course");
        TableColumn<ObservableList<String>, String> lecturerCol =
                new TableColumn<>("Lecturer");
        TableColumn<ObservableList<String>, String> semCol =
                new TableColumn<>("Semester");
        TableColumn<ObservableList<String>, String> gradesCol =
                new TableColumn<>("Grades");
        TableColumn<ObservableList<String>, String> statusCol =
                new TableColumn<>("Status");
        TableColumn<ObservableList<String>, String> dateCol =
                new TableColumn<>("Submitted");

        idCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        courseCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        lecturerCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        semCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));
        gradesCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(4)));
        statusCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(5)));
        dateCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(6)));

        idCol.setPrefWidth(40);
        courseCol.setPrefWidth(80);
        lecturerCol.setPrefWidth(180);
        semCol.setPrefWidth(180);
        gradesCol.setPrefWidth(70);
        statusCol.setPrefWidth(100);
        dateCol.setPrefWidth(110);

        table.getColumns().addAll(idCol, courseCol,
                lecturerCol, semCol, gradesCol,
                statusCol, dateCol);

        loadApprovalsTable(table);

        // Approve button
        Button approveBtn = new Button("Approve & Release");
        approveBtn.getStyleClass().add("primary-button");
        approveBtn.setOnAction(e -> {
            ObservableList<String> sel =
                    table.getSelectionModel()
                            .getSelectedItem();
            if (sel == null) {
                showAlert("Please select a submission.");
                return;
            }
            if (!"PENDING".equals(sel.get(5))) {
                showAlert("Only pending submissions "
                        + "can be approved.");
                return;
            }
            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Approve Grades");
            confirm.setHeaderText("Approve grades for "
                    + sel.get(1) + "?");
            confirm.setContentText(
                    "Students will be able to see "
                            + "their grades immediately.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    try {
                        db.approveGradeSubmission(
                                Integer.parseInt(sel.get(0)),
                                SessionManager.getInstance()
                                        .getUserId());
                        showAlert("Grades approved "
                                + "and released to students!");
                        loadApprovalsTable(table);
                        updateStatus("Approved: "
                                + sel.get(1));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        showAlert("Error: "
                                + ex.getMessage());
                    }
                }
            });
        });

        // Reject button
        Button rejectBtn = new Button("Reject");
        rejectBtn.getStyleClass().add("danger-button");
        rejectBtn.setOnAction(e -> {
            ObservableList<String> sel =
                    table.getSelectionModel()
                            .getSelectedItem();
            if (sel == null) {
                showAlert("Please select a submission.");
                return;
            }

            TextInputDialog dialog =
                    new TextInputDialog();
            dialog.setTitle("Reject Grades");
            dialog.setHeaderText(
                    "Reject grades for " + sel.get(1) + "?");
            dialog.setContentText(
                    "Enter reason for rejection:");
            dialog.showAndWait().ifPresent(reason -> {
                try {
                    db.rejectGradeSubmission(
                            Integer.parseInt(sel.get(0)),
                            reason);
                    showAlert("Submission rejected. "
                            + "Lecturer will be notified.");
                    loadApprovalsTable(table);
                    updateStatus("Rejected: " + sel.get(1));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Error: " + ex.getMessage());
                }
            });
        });

        Button refreshBtn = new Button("🔄  Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(
                e -> loadApprovalsTable(table));

        HBox toolbar = new HBox(12,
                approveBtn, rejectBtn, refreshBtn);
        toolbar.setAlignment(
                javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                new Label("Grade Submission Approvals") {{
                    getStyleClass().add("section-title");
                }},
                new Label(
                        "Lecturers submit grades here. "
                                + "Approve to release to students, "
                                + "or reject to send back for revision.")
                {{
                    setStyle("-fx-text-fill: #5a8a6a; "
                            + "-fx-font-size: 12px;");
                }},
                toolbar, table);
        card.getStyleClass().add("section-card");
        card.setPadding(new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
    }

    private void loadApprovalsTable(
            TableView<ObservableList<String>> table) {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getPendingGradeSubmissions();
            while (rs.next()) {
                String status = rs.getString("status");
                String statusLabel =
                        "APPROVED".equals(status)
                                ? "APPROVED"
                                : "REJECTED".equals(status)
                                ? "REJECTED"
                                : "PENDING";

                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("code"));
                row.add(rs.getString("lecturer_title")
                        + " " + rs.getString("lecturer_name"));
                row.add(rs.getString("semester"));
                row.add(rs.getInt("grades_count") + " grades");
                row.add(statusLabel);
                row.add(rs.getString("submitted_at"));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);
        updateStatus(rows.size() + " submissions loaded.");
    }

// ── Fee Management View ───────────────────────────────────────

    private void showFeeManagementView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        // summary cards
        HBox summaryRow = new HBox(16);
        Label billedLabel  = new Label("Loading...");
        Label collectedLabel = new Label("Loading...");
        Label outstandingLabel = new Label("Loading...");

        try {
            ResultSet sum = db.getFeesSummaryAdmin();
            if (sum.next()) {
                billedLabel.setText(String.format(
                        "GH₵ %.2f",
                        sum.getDouble("total_billed")));
                collectedLabel.setText(String.format(
                        "GH₵ %.2f",
                        sum.getDouble("total_collected")));
                outstandingLabel.setText(String.format(
                        "GH₵ %.2f",
                        sum.getDouble("total_outstanding")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        VBox billedCard = makeFeeCard(
                "", "Total Billed", billedLabel);
        VBox collectedCard = makeFeeCard(
                "", "Collected", collectedLabel);
        VBox outstandingCard = makeFeeCard(
                "", "Outstanding", outstandingLabel);

        summaryRow.getChildren().addAll(
                billedCard, collectedCard, outstandingCard);
        HBox.setHgrow(billedCard,      javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(collectedCard,   javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(outstandingCard, javafx.scene.layout.Priority.ALWAYS);

        // fees table
        TableView<ObservableList<String>> table =
                new TableView<>();
        table.setPrefHeight(350);

        TableColumn<ObservableList<String>, String> numCol =
                new TableColumn<>("Student No.");
        TableColumn<ObservableList<String>, String> nameCol =
                new TableColumn<>("Name");
        TableColumn<ObservableList<String>, String> descCol =
                new TableColumn<>("Description");
        TableColumn<ObservableList<String>, String> amtCol =
                new TableColumn<>("Amount");
        TableColumn<ObservableList<String>, String> paidCol =
                new TableColumn<>("Paid");
        TableColumn<ObservableList<String>, String> balCol =
                new TableColumn<>("Balance");
        TableColumn<ObservableList<String>, String> statCol =
                new TableColumn<>("Status");
        TableColumn<ObservableList<String>, String> dueCol =
                new TableColumn<>("Due Date");

        numCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        nameCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        descCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        amtCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));
        paidCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(4)));
        balCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(5)));
        statCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(6)));
        dueCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(7)));

        numCol.setPrefWidth(110);
        nameCol.setPrefWidth(150);
        descCol.setPrefWidth(150);
        amtCol.setPrefWidth(90);
        paidCol.setPrefWidth(90);
        balCol.setPrefWidth(90);
        statCol.setPrefWidth(90);
        dueCol.setPrefWidth(100);

        table.getColumns().addAll(numCol, nameCol,
                descCol, amtCol, paidCol,
                balCol, statCol, dueCol);

        loadFeesAdminTable(table);

        // Record Payment button
        Button payBtn = new Button("Record Payment");
        payBtn.getStyleClass().add("primary-button");
        payBtn.setOnAction(e -> {
            ObservableList<String> sel =
                    table.getSelectionModel()
                            .getSelectedItem();
            if (sel == null) {
                showAlert("Please select a fee record.");
                return;
            }
            if ("PAID".equals(sel.get(6))) {
                showAlert("This fee is already fully paid.");
                return;
            }
            showRecordPaymentDialog(table, sel);
        });

        // Add Fee button
        Button addFeeBtn = new Button("➕  Add Fee Record");
        addFeeBtn.getStyleClass().add("secondary-button");
        addFeeBtn.setOnAction(
                e -> showAddFeeDialog(table));

        Button refreshBtn = new Button("🔄Refresh");
        refreshBtn.getStyleClass().add("secondary-button");
        refreshBtn.setOnAction(
                e -> loadFeesAdminTable(table));

        HBox toolbar = new HBox(12,
                payBtn, addFeeBtn, refreshBtn);
        toolbar.setAlignment(
                javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                new Label("Student Fee Records") {{
                    getStyleClass().add("section-title");
                }},
                toolbar, table);
        card.getStyleClass().add("section-card");
        card.setPadding(new javafx.geometry.Insets(20));

        view.getChildren().addAll(summaryRow, card);
        contentArea.getChildren().add(view);
    }

    private VBox makeFeeCard(String icon,
                             String title, Label valueLabel) {
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle(
                "-fx-font-size: 18px; "
                        + "-fx-font-weight: bold;");
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setPadding(new javafx.geometry.Insets(16));
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        card.getChildren().addAll(
                iconLabel, valueLabel, titleLabel);
        return card;
    }

    private void loadFeesAdminTable(
            TableView<ObservableList<String>> table) {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getAllFeesAdmin();
            while (rs.next()) {
                String status = rs.getString("status");
                String statusLabel =
                        "PAID".equals(status)    ? "PAID"
                                : "PARTIAL".equals(status)
                                ? "PARTIAL"
                                : "UNPAID";

                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("student_number"));
                row.add(rs.getString("student_name"));
                row.add(rs.getString("description"));
                row.add(String.format("%.2f",
                        rs.getDouble("amount")));
                row.add(String.format("%.2f",
                        rs.getDouble("paid")));
                row.add(String.format("%.2f",
                        rs.getDouble("balance")));
                row.add(statusLabel);
                row.add(rs.getString("due_date"));
                // store fee id at index 8
                row.add(String.valueOf(rs.getInt("id")));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);
        updateStatus(rows.size() + " fee records loaded.");
    }

    private void showRecordPaymentDialog(
            TableView<ObservableList<String>> table,
            ObservableList<String> fee) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Record Payment");
        dialog.setHeaderText("Record payment for "
                + fee.get(1) + " — " + fee.get(2));

        ButtonType saveBtn = new ButtonType(
                "Record", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField amountField = new TextField();
        amountField.setPromptText(
                "Max: GH₵ " + fee.get(5));

        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll(
                "Cash", "Mobile Money",
                "Bank Transfer", "Cheque");
        methodCombo.setValue("Cash");

        TextField refField = new TextField();
        refField.setPromptText("e.g. MM-2025-001234");

        javafx.scene.layout.GridPane grid =
                new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.add(new Label("Amount (GH₵):"),  0, 0);
        grid.add(amountField,                  1, 0);
        grid.add(new Label("Method:"),         0, 1);
        grid.add(methodCombo,                  1, 1);
        grid.add(new Label("Reference:"),      0, 2);
        grid.add(refField,                     1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    double amount = Double.parseDouble(
                            amountField.getText().trim());
                    double balance = Double.parseDouble(
                            fee.get(5));

                    if (amount <= 0 || amount > balance) {
                        showAlert("Amount must be "
                                + "between 0 and GH₵ "
                                + fee.get(5));
                        return null;
                    }

                    int feeId = Integer.parseInt(
                            fee.get(8));
                    db.adminRecordPayment(
                            feeId, amount,
                            methodCombo.getValue(),
                            refField.getText().trim());

                    loadFeesAdminTable(table);
                    showAlert("Payment of GH₵ "
                            + String.format("%.2f", amount)
                            + " recorded successfully!");
                    updateStatus("Payment recorded for "
                            + fee.get(1));
                } catch (NumberFormatException ex) {
                    showAlert("Please enter a "
                            + "valid amount.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Error: " + ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showAddFeeDialog(
            TableView<ObservableList<String>> table) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Fee Record");
        dialog.setHeaderText("Add New Fee Record");

        ButtonType saveBtn = new ButtonType(
                "Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        ComboBox<String> studentCombo = new ComboBox<>();
        studentCombo.setPromptText("Select student...");
        studentCombo.setPrefWidth(250);

        java.util.Map<String, Integer> studentMap =
                new java.util.LinkedHashMap<>();
        try {
            ResultSet rs = db.getAllStudents();
            while (rs.next()) {
                String label = rs.getString("student_number")
                        + " — " + rs.getString("name");
                studentMap.put(label,
                        rs.getInt("student_id"));
                studentCombo.getItems().add(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextField descField = new TextField();
        descField.setPromptText("e.g. Tuition Fees");

        TextField amountField = new TextField();
        amountField.setPromptText("e.g. 2500.00");

        TextField semField = new TextField(
                "2025/2026 Second Semester");

        javafx.scene.control.DatePicker duePicker =
                new javafx.scene.control.DatePicker(
                        java.time.LocalDate.now()
                                .plusDays(30));

        javafx.scene.layout.GridPane grid =
                new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.add(new Label("Student:"),     0, 0);
        grid.add(studentCombo,              1, 0);
        grid.add(new Label("Description:"),  0, 1);
        grid.add(descField,                 1, 1);
        grid.add(new Label("Amount (GH₵):"),0, 2);
        grid.add(amountField,               1, 2);
        grid.add(new Label("Semester:"),    0, 3);
        grid.add(semField,                  1, 3);
        grid.add(new Label("Due Date:"),    0, 4);
        grid.add(duePicker,                 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    if (studentCombo.getValue() == null) {
                        showAlert("Please select a student.");
                        return null;
                    }
                    int studentId = studentMap.get(
                            studentCombo.getValue());
                    double amount = Double.parseDouble(
                            amountField.getText().trim());
                    String desc = descField.getText().trim();
                    String sem  = semField.getText().trim();
                    String due  = duePicker.getValue()
                            .toString();

                    if (desc.isEmpty()) {
                        showAlert("Description is required.");
                        return null;
                    }

                    db.addFeeRecord(studentId, sem,
                            amount, desc, due);
                    loadFeesAdminTable(table);
                    showAlert("Fee record added!");
                    updateStatus("Fee added for "
                            + studentCombo.getValue());
                } catch (NumberFormatException ex) {
                    showAlert("Please enter a valid amount.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Error: " + ex.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

// ── Backup & Restore View ─────────────────────────────────────

    private void showBackupView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(20);

        // Backup card
        VBox backupCard = new VBox(14);
        backupCard.getStyleClass().add("section-card");
        backupCard.setPadding(
                new javafx.geometry.Insets(24));

        Label backupTitle = new Label("Database Backup");
        backupTitle.getStyleClass().add("section-title");

        Label backupDesc = new Label(
                "Export the entire database to a .sql file. "
                        + "Keep this file safe — it contains all "
                        + "student records, grades and fees.");
        backupDesc.setWrapText(true);
        backupDesc.setStyle("-fx-text-fill: #5a8a6a; "
                + "-fx-font-size: 12px;");

        Label backupStatus = new Label("");
        backupStatus.setWrapText(true);

        Button exportBtn = new Button(
                "Export Database Backup");
        exportBtn.getStyleClass().add("primary-button");
        exportBtn.setOnAction(e -> {
            javafx.stage.FileChooser chooser =
                    new javafx.stage.FileChooser();
            chooser.setTitle("Save Database Backup");
            chooser.setInitialFileName(
                    "ucc_portal_backup_"
                            + java.time.LocalDate.now()
                            + ".sql");
            chooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser
                            .ExtensionFilter(
                            "SQL Files", "*.sql"));

            java.io.File file = chooser.showSaveDialog(
                    navBackup.getScene().getWindow());
            if (file != null) {
                try {
                    String sql = db.exportDatabaseSQL();
                    java.nio.file.Files.writeString(
                            file.toPath(), sql);
                    backupStatus.setText(
                            "Backup saved to: "
                                    + file.getAbsolutePath());
                    backupStatus.setStyle(
                            "-fx-text-fill: #006B3F;");
                    updateStatus("Backup exported: "
                            + file.getName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    backupStatus.setText(
                            "❌  Error: " + ex.getMessage());
                    backupStatus.setStyle(
                            "-fx-text-fill: #c0392b;");
                }
            }
        });

        backupCard.getChildren().addAll(
                backupTitle, backupDesc,
                exportBtn, backupStatus);

        // Restore card
        VBox restoreCard = new VBox(14);
        restoreCard.getStyleClass().add("section-card");
        restoreCard.setPadding(
                new javafx.geometry.Insets(24));

        Label restoreTitle = new Label(
                "Database Restore");
        restoreTitle.getStyleClass().add("section-title");

        Label restoreDesc = new Label(
                "  WARNING: Restoring will overwrite current "
                        + "data with the backup. This cannot be undone. "
                        + "Only restore from a trusted backup file.");
        restoreDesc.setWrapText(true);
        restoreDesc.setStyle(
                "-fx-text-fill: #c0392b; "
                        + "-fx-font-size: 12px;");

        Label restoreStatus = new Label("");
        restoreStatus.setWrapText(true);

        Button restoreBtn = new Button(
                " Restore from Backup");
        restoreBtn.getStyleClass().add("danger-button");
        restoreBtn.setOnAction(e -> {
            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Restore Database");
            confirm.setHeaderText(
                    "  Are you sure you want to restore?");
            confirm.setContentText(
                    "This will overwrite ALL current data. "
                            + "This action cannot be undone.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    javafx.stage.FileChooser chooser =
                            new javafx.stage.FileChooser();
                    chooser.setTitle("Open Backup File");
                    chooser.getExtensionFilters().add(
                            new javafx.stage.FileChooser
                                    .ExtensionFilter(
                                    "SQL Files",
                                    "*.sql"));

                    java.io.File file =
                            chooser.showOpenDialog(
                                    navBackup.getScene()
                                            .getWindow());
                    if (file != null) {
                        try {
                            String sql =
                                    java.nio.file.Files
                                            .readString(
                                                    file.toPath());
                            db.restoreDatabaseSQL(sql);
                            restoreStatus.setText(
                                    "Database restored "
                                            + "from: "
                                            + file.getName());
                            restoreStatus.setStyle(
                                    "-fx-text-fill: #006B3F;");
                            updateStatus("Database restored.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            restoreStatus.setText(
                                    "❌  Error: "
                                            + ex.getMessage());
                            restoreStatus.setStyle(
                                    "-fx-text-fill: #c0392b;");
                        }
                    }
                }
            });
        });

        restoreCard.getChildren().addAll(
                restoreTitle, restoreDesc,
                restoreBtn, restoreStatus);

        view.getChildren().addAll(backupCard, restoreCard);
        contentArea.getChildren().add(view);
    }
    // ── Init ──────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        db = DatabaseManager.getInstance();
        loadAdminFromSession();
        setupWelcome();
        setupStatCards();
        setupRecentStudentsTable();
        setupRecentLecturersTable();
        setActiveNav(navDashboard);
        updateStatus("Admin portal ready.");
    }

    // ── Load Admin ────────────────────────────────────────────────

    private void loadAdminFromSession() {
        currentAdmin = new Admin();
        currentAdmin.setName(SessionManager.getInstance().getName());
        currentAdmin.setEmail(SessionManager.getInstance().getEmail());
        adminNameLabel.setText(currentAdmin.getName());
    }

    // ── Welcome ───────────────────────────────────────────────────

    private void setupWelcome() {
        welcomeLabel.setText("Welcome, "
                + currentAdmin.getFirstName() + "! ");
        dateLabel.setText(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, MMMM dd yyyy")));
    }

    // ── Stat Cards ────────────────────────────────────────────────

    private void setupStatCards() {
        try {
            // students
            ResultSet s = db.getConnection().createStatement()
                    .executeQuery(
                            "SELECT COUNT(*) as cnt FROM students");
            totalStudentsLabel.setText(
                    String.valueOf(s.getInt("cnt")));

            // lecturers
            ResultSet l = db.getConnection().createStatement()
                    .executeQuery(
                            "SELECT COUNT(*) as cnt FROM lecturers");
            totalLecturersLabel.setText(
                    String.valueOf(l.getInt("cnt")));

            // courses
            ResultSet c = db.getConnection().createStatement()
                    .executeQuery(
                            "SELECT COUNT(*) as cnt FROM courses");
            totalCoursesLabel.setText(
                    String.valueOf(c.getInt("cnt")));

            // enrollments
            ResultSet e = db.getConnection().createStatement()
                    .executeQuery(
                            "SELECT COUNT(*) as cnt FROM enrollments");
            totalEnrollmentsLabel.setText(
                    String.valueOf(e.getInt("cnt")));

            statsLabel.setText("Last updated: "
                    + LocalDate.now());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Recent Students Table ─────────────────────────────────────

    private void setupRecentStudentsTable() {
        rsNumCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        rsNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        rsMajorCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        rsYearCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        rsEmailCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));

        loadStudentsTable(recentStudentsTable);
    }

    private void loadStudentsTable(
            TableView<ObservableList<String>> table) {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getAllStudents();
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("student_number"));
                row.add(rs.getString("name"));
                row.add(rs.getString("major"));
                row.add("Year " + rs.getInt("year"));
                row.add(rs.getString("email"));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);
        updateStatus(rows.size() + " students loaded.");
    }

    // ── Recent Lecturers Table ────────────────────────────────────

    private void setupRecentLecturersTable() {
        rlNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        rlTitleCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        rlDeptCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        rlEmailCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));

        loadLecturersTable(recentLecturersTable);
    }

    private void loadLecturersTable(
            TableView<ObservableList<String>> table) {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getAllLecturers();
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("name"));
                row.add(rs.getString("title"));
                row.add(rs.getString("department"));
                row.add(rs.getString("email"));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);
    }

    // ── Full Students View ────────────────────────────────────────

    private void showStudentsView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        // search bar
        TextField searchField = new TextField();
        searchField.setPromptText("🔍  Search by name or student number...");
        searchField.setPrefWidth(320);

        // table
        TableView<ObservableList<String>> table = new TableView<>();
        table.setPrefHeight(400);

        TableColumn<ObservableList<String>, String> numCol =
                new TableColumn<>("Student No.");
        TableColumn<ObservableList<String>, String> nameCol =
                new TableColumn<>("Name");
        TableColumn<ObservableList<String>, String> majorCol =
                new TableColumn<>("Major");
        TableColumn<ObservableList<String>, String> yearCol =
                new TableColumn<>("Year");
        TableColumn<ObservableList<String>, String> emailCol =
                new TableColumn<>("Email");

        numCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        nameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        majorCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        yearCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        emailCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));

        numCol.setPrefWidth(130);
        nameCol.setPrefWidth(180);
        majorCol.setPrefWidth(150);
        yearCol.setPrefWidth(60);
        emailCol.setPrefWidth(200);

        table.getColumns().addAll(
                numCol, nameCol, majorCol, yearCol, emailCol);
        loadStudentsTable(table);

        // live search
        searchField.textProperty().addListener((obs, o, val) -> {
            if (val == null || val.isBlank()) {
                loadStudentsTable(table);
                return;
            }
            ObservableList<ObservableList<String>> filtered =
                    FXCollections.observableArrayList();
            table.getItems().forEach(row -> {
                if (row.get(0).toLowerCase()
                        .contains(val.toLowerCase())
                        || row.get(1).toLowerCase()
                        .contains(val.toLowerCase()))
                    filtered.add(row);
            });
            table.setItems(filtered);
            updateStatus(filtered.size() + " results found.");
        });

        // buttons
        Button addBtn = new Button("➕  Add Student");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> showAddStudentDialog(table));

        Button editBtn = new Button("  Edit");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setOnAction(e -> {
            ObservableList<String> sel =
                    table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Please select a student.");
                return;
            }
            showEditStudentDialog(table, sel);
        });

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> {
            ObservableList<String> sel =
                    table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Please select a student.");
                return;
            }
            deleteStudent(table, sel.get(0), sel.get(1));
        });

        HBox toolbar = new HBox(12,
                searchField, addBtn, editBtn, deleteBtn);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                new Label("All Students") {{
                    getStyleClass().add("section-title");
                }},
                toolbar, table);
        card.getStyleClass().add("section-card");
        card.setPadding(new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
    }

    private void showAddStudentDialog(
            TableView<ObservableList<String>> table) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Student");
        dialog.setHeaderText("Register New Student");

        ButtonType saveBtn = new ButtonType(
                "Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField nameField   = new TextField();
        TextField emailField  = new TextField();
        TextField passField   = new TextField();
        TextField numField    = new TextField();
        TextField majorField  = new TextField();
        TextField yearField   = new TextField("1");

        nameField.setPromptText("Full name");
        emailField.setPromptText("email@ucc.edu.gh");
        passField.setPromptText("Initial password");
        numField.setPromptText("PS/CSC/23/0001");
        majorField.setPromptText("Computer Science");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.add(new Label("Full Name:"),      0, 0); grid.add(nameField,  1, 0);
        grid.add(new Label("Email:"),          0, 1); grid.add(emailField, 1, 1);
        grid.add(new Label("Password:"),       0, 2); grid.add(passField,  1, 2);
        grid.add(new Label("Student No.:"),    0, 3); grid.add(numField,   1, 3);
        grid.add(new Label("Major:"),          0, 4); grid.add(majorField, 1, 4);
        grid.add(new Label("Year:"),           0, 5); grid.add(yearField,  1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    String name  = nameField.getText().trim();
                    String email = emailField.getText().trim()
                            .toLowerCase();
                    String pass  = passField.getText().trim();
                    String num   = numField.getText().trim();
                    String major = majorField.getText().trim();
                    int    year  = Integer.parseInt(
                            yearField.getText().trim());

                    if (name.isEmpty() || email.isEmpty()
                            || pass.isEmpty() || num.isEmpty()) {
                        showAlert("All fields are required.");
                        return null;
                    }

                    // insert user
                    java.sql.PreparedStatement ups =
                            db.getConnection().prepareStatement(
                                    "INSERT INTO users "
                                            + "(name, email, password, role) "
                                            + "VALUES (?, ?, ?, 'STUDENT')",
                                    java.sql.Statement
                                            .RETURN_GENERATED_KEYS);
                    ups.setString(1, name);
                    ups.setString(2, email);
                    ups.setString(3,
                            org.mindrot.jbcrypt.BCrypt.hashpw(
                                    pass,
                                    org.mindrot.jbcrypt.BCrypt
                                            .gensalt()));
                    ups.executeUpdate();
                    int userId = ups.getGeneratedKeys().getInt(1);

                    // insert student
                    java.sql.PreparedStatement sps =
                            db.getConnection().prepareStatement(
                                    "INSERT INTO students "
                                            + "(user_id, student_number, "
                                            + "major, year) "
                                            + "VALUES (?, ?, ?, ?)");
                    sps.setInt(1, userId);
                    sps.setString(2, num);
                    sps.setString(3, major);
                    sps.setInt(4, year);
                    sps.executeUpdate();

                    loadStudentsTable(table);
                    setupStatCards();
                    updateStatus("Student registered: " + name);
                    showAlert("Student registered successfully!");

                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showEditStudentDialog(
            TableView<ObservableList<String>> table,
            ObservableList<String> student) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Edit Student");
        dialog.setHeaderText("Edit: " + student.get(1));

        ButtonType saveBtn = new ButtonType(
                "Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField majorField = new TextField(student.get(2));
        TextField yearField  = new TextField(
                student.get(3).replace("Year ", ""));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.add(new Label("Major:"), 0, 0); grid.add(majorField, 1, 0);
        grid.add(new Label("Year:"),  0, 1); grid.add(yearField,  1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    db.getConnection().prepareStatement(
                                    "UPDATE students SET major = '"
                                            + majorField.getText().trim()
                                            + "', year = "
                                            + Integer.parseInt(yearField.getText().trim())
                                            + " WHERE student_number = '"
                                            + student.get(0) + "'")
                            .executeUpdate();
                    loadStudentsTable(table);
                    updateStatus("Updated: " + student.get(1));
                    showAlert("Student updated successfully!");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteStudent(
            TableView<ObservableList<String>> table,
            String studentNum, String name) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Student");
        confirm.setHeaderText("Delete " + name + "?");
        confirm.setContentText(
                "This will permanently delete the student and "
                        + "all their records.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    db.getConnection().prepareStatement(
                                    "DELETE FROM users WHERE id = ("
                                            + "SELECT user_id FROM students "
                                            + "WHERE student_number = '"
                                            + studentNum + "')")
                            .executeUpdate();
                    loadStudentsTable(table);
                    setupStatCards();
                    updateStatus("Deleted: " + name);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
        });
    }

    // ── Full Lecturers View ───────────────────────────────────────

    private void showLecturersView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        TableView<ObservableList<String>> table = new TableView<>();
        table.setPrefHeight(380);

        TableColumn<ObservableList<String>, String> nameCol =
                new TableColumn<>("Name");
        TableColumn<ObservableList<String>, String> titleCol =
                new TableColumn<>("Title");
        TableColumn<ObservableList<String>, String> deptCol =
                new TableColumn<>("Department");
        TableColumn<ObservableList<String>, String> emailCol =
                new TableColumn<>("Email");

        nameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        titleCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        deptCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        emailCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));

        nameCol.setPrefWidth(200);
        titleCol.setPrefWidth(80);
        deptCol.setPrefWidth(160);
        emailCol.setPrefWidth(220);

        table.getColumns().addAll(
                nameCol, titleCol, deptCol, emailCol);
        loadLecturersTable(table);

        Button addBtn = new Button("➕  Add Lecturer");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> showAddLecturerDialog(table));

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> {
            ObservableList<String> sel =
                    table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Please select a lecturer.");
                return;
            }
            deleteLecturer(table, sel.get(3), sel.get(0));
        });

        HBox toolbar = new HBox(12, addBtn, deleteBtn);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                new Label("All Lecturers") {{
                    getStyleClass().add("section-title");
                }},
                toolbar, table);
        card.getStyleClass().add("section-card");
        card.setPadding(new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
        updateStatus("Lecturers loaded.");
    }

    private void showAddLecturerDialog(
            TableView<ObservableList<String>> table) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Lecturer");
        dialog.setHeaderText("Register New Lecturer");

        ButtonType saveBtn = new ButtonType(
                "Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField nameField  = new TextField();
        TextField emailField = new TextField();
        TextField passField  = new TextField();
        TextField deptField  = new TextField();
        ComboBox<String> titleCombo = new ComboBox<>();
        titleCombo.getItems().addAll(
                "Mr.", "Mrs.", "Ms.", "Dr.", "Prof.");
        titleCombo.setValue("Mr.");

        nameField.setPromptText("Full name");
        emailField.setPromptText("email@ucc.edu.gh");
        passField.setPromptText("Initial password");
        deptField.setPromptText("Computer Science");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.add(new Label("Full Name:"),   0, 0);
        grid.add(nameField,                 1, 0);
        grid.add(new Label("Email:"),       0, 1);
        grid.add(emailField,                1, 1);
        grid.add(new Label("Password:"),    0, 2);
        grid.add(passField,                 1, 2);
        grid.add(new Label("Title:"),       0, 3);
        grid.add(titleCombo,                1, 3);
        grid.add(new Label("Department:"),  0, 4);
        grid.add(deptField,                 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    String name  = nameField.getText().trim();
                    String email = emailField.getText().trim()
                            .toLowerCase();
                    String pass  = passField.getText().trim();
                    String dept  = deptField.getText().trim();
                    String title = titleCombo.getValue();

                    if (name.isEmpty() || email.isEmpty()
                            || pass.isEmpty()) {
                        showAlert("Name, email and password "
                                + "are required.");
                        return null;
                    }

                    // insert user
                    java.sql.PreparedStatement ups =
                            db.getConnection().prepareStatement(
                                    "INSERT INTO users "
                                            + "(name, email, password, role) "
                                            + "VALUES (?, ?, ?, 'LECTURER')",
                                    java.sql.Statement
                                            .RETURN_GENERATED_KEYS);
                    ups.setString(1, name);
                    ups.setString(2, email);
                    ups.setString(3,
                            org.mindrot.jbcrypt.BCrypt.hashpw(
                                    pass,
                                    org.mindrot.jbcrypt.BCrypt
                                            .gensalt()));
                    ups.executeUpdate();
                    int userId = ups.getGeneratedKeys().getInt(1);

                    // insert lecturer
                    java.sql.PreparedStatement lps =
                            db.getConnection().prepareStatement(
                                    "INSERT INTO lecturers "
                                            + "(user_id, department, title) "
                                            + "VALUES (?, ?, ?)");
                    lps.setInt(1, userId);
                    lps.setString(2, dept);
                    lps.setString(3, title);
                    lps.executeUpdate();

                    loadLecturersTable(table);
                    setupStatCards();
                    updateStatus("Lecturer added: " + name);
                    showAlert("Lecturer registered successfully!");

                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteLecturer(
            TableView<ObservableList<String>> table,
            String email, String name) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Lecturer");
        confirm.setHeaderText("Delete " + name + "?");
        confirm.setContentText(
                "This will remove the lecturer from the system.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    db.getConnection().prepareStatement(
                                    "DELETE FROM users WHERE email = '"
                                            + email + "'")
                            .executeUpdate();
                    loadLecturersTable(table);
                    setupStatCards();
                    updateStatus("Deleted: " + name);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
        });
    }

    // ── Courses View ──────────────────────────────────────────────

    private void showCoursesView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        TableView<ObservableList<String>> table = new TableView<>();
        table.setPrefHeight(380);

        TableColumn<ObservableList<String>, String> codeCol =
                new TableColumn<>("Code");
        TableColumn<ObservableList<String>, String> nameCol =
                new TableColumn<>("Name");
        TableColumn<ObservableList<String>, String> creditsCol =
                new TableColumn<>("Credits");
        TableColumn<ObservableList<String>, String> lecCol =
                new TableColumn<>("Lecturer");
        TableColumn<ObservableList<String>, String> semCol =
                new TableColumn<>("Semester");

        codeCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        nameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        creditsCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        lecCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        semCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));

        codeCol.setPrefWidth(80);
        nameCol.setPrefWidth(200);
        creditsCol.setPrefWidth(70);
        lecCol.setPrefWidth(180);
        semCol.setPrefWidth(180);

        table.getColumns().addAll(
                codeCol, nameCol, creditsCol, lecCol, semCol);
        loadCoursesTable(table);

        Button addBtn = new Button("➕  Add Course");
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> showAddCourseDialog(table));

        Button deleteBtn = new Button("🗑  Delete");
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setOnAction(e -> {
            ObservableList<String> sel =
                    table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Please select a course.");
                return;
            }
            deleteCourse(table, sel.get(0), sel.get(1));
        });

        HBox toolbar = new HBox(12, addBtn, deleteBtn);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox card = new VBox(16,
                new Label("All Courses") {{
                    getStyleClass().add("section-title");
                }},
                toolbar, table);
        card.getStyleClass().add("section-card");
        card.setPadding(new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
    }

    private void loadCoursesTable(
            TableView<ObservableList<String>> table) {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getConnection().createStatement()
                    .executeQuery("""
                SELECT c.code, c.name, c.credits,
                       COALESCE(l.title || ' ' || u.name, 'TBA')
                           as lecturer,
                       c.semester
                FROM courses c
                LEFT JOIN lecturers l ON l.id = c.lecturer_id
                LEFT JOIN users u ON u.id = l.user_id
                ORDER BY c.code
            """);
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(String.valueOf(rs.getInt("credits")));
                row.add(rs.getString("lecturer"));
                row.add(rs.getString("semester"));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);
        updateStatus(rows.size() + " courses loaded.");
    }

    private void showAddCourseDialog(
            TableView<ObservableList<String>> table) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Course");
        dialog.setHeaderText("Add New Course");

        ButtonType saveBtn = new ButtonType(
                "Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes()
                .addAll(saveBtn, ButtonType.CANCEL);

        TextField codeField    = new TextField();
        TextField nameField    = new TextField();
        TextField creditsField = new TextField("3");
        TextField semField     = new TextField(
                "2025/2026 Second Semester");
        TextField descField    = new TextField();

        // lecturer dropdown
        ComboBox<String> lecCombo = new ComboBox<>();
        java.util.Map<String, Integer> lecMap =
                new java.util.LinkedHashMap<>();
        lecMap.put("TBA (no lecturer)", 0);
        lecCombo.getItems().add("TBA (no lecturer)");
        try {
            ResultSet rs = db.getAllLecturers();
            while (rs.next()) {
                String label = rs.getString("title") + " "
                        + rs.getString("name");
                lecMap.put(label, rs.getInt("lecturer_id"));
                lecCombo.getItems().add(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        lecCombo.setValue("TBA (no lecturer)");

        codeField.setPromptText("e.g. CSC401");
        nameField.setPromptText("Course name");
        descField.setPromptText("Brief description");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.add(new Label("Code:"),        0, 0);
        grid.add(codeField,                 1, 0);
        grid.add(new Label("Name:"),        0, 1);
        grid.add(nameField,                 1, 1);
        grid.add(new Label("Credits:"),     0, 2);
        grid.add(creditsField,              1, 2);
        grid.add(new Label("Lecturer:"),    0, 3);
        grid.add(lecCombo,                  1, 3);
        grid.add(new Label("Semester:"),    0, 4);
        grid.add(semField,                  1, 4);
        grid.add(new Label("Description:"), 0, 5);
        grid.add(descField,                 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    String code    = codeField.getText()
                            .trim().toUpperCase();
                    String name    = nameField.getText().trim();
                    int    credits = Integer.parseInt(
                            creditsField.getText().trim());
                    int    lecId   = lecMap.get(
                            lecCombo.getValue());

                    if (code.isEmpty() || name.isEmpty()) {
                        showAlert("Code and name are required.");
                        return null;
                    }

                    java.sql.PreparedStatement ps =
                            db.getConnection().prepareStatement(
                                    "INSERT INTO courses "
                                            + "(code, name, credits, "
                                            + "lecturer_id, semester, "
                                            + "description) "
                                            + "VALUES (?, ?, ?, ?, ?, ?)");
                    ps.setString(1, code);
                    ps.setString(2, name);
                    ps.setInt(3, credits);
                    if (lecId == 0)
                        ps.setNull(4,
                                java.sql.Types.INTEGER);
                    else
                        ps.setInt(4, lecId);
                    ps.setString(5, semField.getText().trim());
                    ps.setString(6, descField.getText().trim());
                    ps.executeUpdate();

                    loadCoursesTable(table);
                    setupStatCards();
                    updateStatus("Course added: " + code);
                    showAlert("Course added successfully!");

                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteCourse(
            TableView<ObservableList<String>> table,
            String code, String name) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Course");
        confirm.setHeaderText("Delete " + code + " — " + name + "?");
        confirm.setContentText(
                "This will remove all enrollments, assignments "
                        + "and grades for this course.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    db.getConnection().prepareStatement(
                            "DELETE FROM courses WHERE code = '"
                                    + code + "'").executeUpdate();
                    loadCoursesTable(table);
                    setupStatCards();
                    updateStatus("Deleted course: " + code);
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
        });
    }

    // ── Enrollments View ──────────────────────────────────────────

    private void showEnrollmentsView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        // enroll section
        ComboBox<String> studentCombo = new ComboBox<>();
        ComboBox<String> courseCombo  = new ComboBox<>();
        studentCombo.setPromptText("Select student...");
        courseCombo.setPromptText("Select course...");
        studentCombo.setPrefWidth(260);
        courseCombo.setPrefWidth(260);

        java.util.Map<String, Integer> studentMap =
                new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> courseMap  =
                new java.util.LinkedHashMap<>();

        try {
            ResultSet rs = db.getAllStudents();
            while (rs.next()) {
                String label = rs.getString("student_number")
                        + " — " + rs.getString("name");
                studentMap.put(label, rs.getInt("student_id"));
                studentCombo.getItems().add(label);
            }
            ResultSet cs = db.getConnection().createStatement()
                    .executeQuery(
                            "SELECT id, code, name FROM courses "
                                    + "ORDER BY code");
            while (cs.next()) {
                String label = cs.getString("code")
                        + " — " + cs.getString("name");
                courseMap.put(label, cs.getInt("id"));
                courseCombo.getItems().add(label);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button enrollBtn = new Button("➕  Enroll Student");
        enrollBtn.getStyleClass().add("primary-button");
        enrollBtn.setOnAction(e -> {
            if (studentCombo.getValue() == null
                    || courseCombo.getValue() == null) {
                showAlert("Please select both a student and a course.");
                return;
            }
            try {
                int studentId = studentMap.get(
                        studentCombo.getValue());
                int courseId  = courseMap.get(
                        courseCombo.getValue());
                db.getConnection().prepareStatement(
                                "INSERT OR IGNORE INTO enrollments "
                                        + "(student_id, course_id) VALUES ("
                                        + studentId + ", " + courseId + ")")
                        .executeUpdate();
                setupStatCards();
                updateStatus("Student enrolled successfully.");
                showAlert("Student enrolled successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error: " + ex.getMessage());
            }
        });

        HBox enrollBar = new HBox(12,
                studentCombo, courseCombo, enrollBtn);
        enrollBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // enrollments table
        TableView<ObservableList<String>> table = new TableView<>();
        table.setPrefHeight(350);

        TableColumn<ObservableList<String>, String> sNumCol =
                new TableColumn<>("Student No.");
        TableColumn<ObservableList<String>, String> sNameCol =
                new TableColumn<>("Student Name");
        TableColumn<ObservableList<String>, String> cCodeCol =
                new TableColumn<>("Course Code");
        TableColumn<ObservableList<String>, String> cNameCol =
                new TableColumn<>("Course Name");
        TableColumn<ObservableList<String>, String> dateCol  =
                new TableColumn<>("Enrolled On");

        sNumCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        sNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        cCodeCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        cNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        dateCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));

        sNumCol.setPrefWidth(120);
        sNameCol.setPrefWidth(160);
        cCodeCol.setPrefWidth(90);
        cNameCol.setPrefWidth(180);
        dateCol.setPrefWidth(110);

        table.getColumns().addAll(
                sNumCol, sNameCol, cCodeCol, cNameCol, dateCol);
        loadEnrollmentsTable(table);

        Button removeBtn = new Button("🗑  Remove Enrollment");
        removeBtn.getStyleClass().add("danger-button");
        removeBtn.setOnAction(e -> {
            ObservableList<String> sel =
                    table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("Please select an enrollment to remove.");
                return;
            }
            removeEnrollment(table, sel.get(0), sel.get(2),
                    sel.get(1));
        });

        VBox card = new VBox(16,
                new Label("Manage Enrollments") {{
                    getStyleClass().add("section-title");
                }},
                new Label("Enroll a Student:"),
                enrollBar,
                new Separator(),
                new Label("Current Enrollments:") {{
                    getStyleClass().add("section-title");
                }},
                removeBtn,
                table);
        card.getStyleClass().add("section-card");
        card.setPadding(new javafx.geometry.Insets(20));

        view.getChildren().add(card);
        contentArea.getChildren().add(view);
    }

    private void loadEnrollmentsTable(
            TableView<ObservableList<String>> table) {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getConnection().createStatement()
                    .executeQuery("""
                SELECT s.student_number, u.name as student_name,
                       c.code, c.name as course_name,
                       e.enrolled_at
                FROM enrollments e
                JOIN students s ON s.id = e.student_id
                JOIN users u ON u.id = s.user_id
                JOIN courses c ON c.id = e.course_id
                ORDER BY u.name, c.code
            """);
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("student_number"));
                row.add(rs.getString("student_name"));
                row.add(rs.getString("code"));
                row.add(rs.getString("course_name"));
                row.add(rs.getString("enrolled_at"));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        table.setItems(rows);
        updateStatus(rows.size() + " enrollments loaded.");
    }

    private void removeEnrollment(
            TableView<ObservableList<String>> table,
            String studentNum, String courseCode,
            String studentName) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Enrollment");
        confirm.setHeaderText("Remove " + studentName
                + " from " + courseCode + "?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    db.getConnection().prepareStatement(
                                    "DELETE FROM enrollments WHERE "
                                            + "student_id = (SELECT id FROM students "
                                            + "WHERE student_number = '"
                                            + studentNum + "') AND "
                                            + "course_id = (SELECT id FROM courses "
                                            + "WHERE code = '"
                                            + courseCode + "')")
                            .executeUpdate();
                    loadEnrollmentsTable(table);
                    setupStatCards();
                    updateStatus("Enrollment removed.");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error: " + e.getMessage());
                }
            }
        });
    }

    // ── Reports View ──────────────────────────────────────────────

    private void showReportsView() {
        contentArea.getChildren().clear();
        VBox view = new VBox(16);

        // GPA report table
        TableView<ObservableList<String>> gpaTable =
                new TableView<>();
        gpaTable.setPrefHeight(300);

        TableColumn<ObservableList<String>, String> numCol =
                new TableColumn<>("Student No.");
        TableColumn<ObservableList<String>, String> nameCol =
                new TableColumn<>("Name");
        TableColumn<ObservableList<String>, String> majorCol =
                new TableColumn<>("Major");
        TableColumn<ObservableList<String>, String> gpaCol =
                new TableColumn<>("GPA");
        TableColumn<ObservableList<String>, String> gradesCol =
                new TableColumn<>("Grades Entered");

        numCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        nameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        majorCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        gpaCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        gradesCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));

        numCol.setPrefWidth(130);
        nameCol.setPrefWidth(180);
        majorCol.setPrefWidth(150);
        gpaCol.setPrefWidth(80);
        gradesCol.setPrefWidth(120);

        gpaTable.getColumns().addAll(
                numCol, nameCol, majorCol, gpaCol, gradesCol);

        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getConnection().createStatement()
                    .executeQuery("""
                SELECT s.student_number, u.name, s.major,
                       ROUND(AVG(CASE
                           WHEN g.score >= 90 THEN 4.0
                           WHEN g.score >= 80 THEN 3.0
                           WHEN g.score >= 70 THEN 2.0
                           WHEN g.score >= 60 THEN 1.0
                           ELSE 0.0
                       END), 2) as gpa,
                       COUNT(g.id) as grade_count
                FROM students s
                JOIN users u ON u.id = s.user_id
                LEFT JOIN grades g ON g.student_id = s.id
                GROUP BY s.id
                ORDER BY gpa DESC
            """);
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("student_number"));
                row.add(rs.getString("name"));
                row.add(rs.getString("major"));
                double gpa = rs.getDouble("gpa");
                row.add(gpa > 0
                        ? String.format("%.2f", gpa) : "N/A");
                row.add(String.valueOf(
                        rs.getInt("grade_count")));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        gpaTable.setItems(rows);

        VBox gpaCard = new VBox(12,
                new Label("GPA Report — All Students") {{
                    getStyleClass().add("section-title");
                }},
                gpaTable);
        gpaCard.getStyleClass().add("section-card");
        gpaCard.setPadding(new javafx.geometry.Insets(20));

        // course enrollment report
        TableView<ObservableList<String>> courseTable =
                new TableView<>();
        courseTable.setPrefHeight(220);

        TableColumn<ObservableList<String>, String> cCodeCol =
                new TableColumn<>("Code");
        TableColumn<ObservableList<String>, String> cNameCol =
                new TableColumn<>("Course");
        TableColumn<ObservableList<String>, String> cEnrolCol =
                new TableColumn<>("Enrolled");
        TableColumn<ObservableList<String>, String> cGradesCol =
                new TableColumn<>("Grades Entered");
        TableColumn<ObservableList<String>, String> cAvgCol =
                new TableColumn<>("Avg Score");

        cCodeCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(0)));
        cNameCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(1)));
        cEnrolCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(2)));
        cGradesCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(3)));
        cAvgCol.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().get(4)));

        cCodeCol.setPrefWidth(80);
        cNameCol.setPrefWidth(200);
        cEnrolCol.setPrefWidth(80);
        cGradesCol.setPrefWidth(120);
        cAvgCol.setPrefWidth(100);

        courseTable.getColumns().addAll(
                cCodeCol, cNameCol, cEnrolCol,
                cGradesCol, cAvgCol);

        ObservableList<ObservableList<String>> courseRows =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getConnection().createStatement()
                    .executeQuery("""
                SELECT c.code, c.name,
                       COUNT(DISTINCT e.student_id) as enrolled,
                       COUNT(DISTINCT g.id) as grades_entered,
                       ROUND(AVG(g.score), 1) as avg_score
                FROM courses c
                LEFT JOIN enrollments e ON e.course_id = c.id
                LEFT JOIN grades g ON g.course_id = c.id
                GROUP BY c.id
                ORDER BY c.code
            """);
            while (rs.next()) {
                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("code"));
                row.add(rs.getString("name"));
                row.add(String.valueOf(rs.getInt("enrolled")));
                row.add(String.valueOf(
                        rs.getInt("grades_entered")));
                double avg = rs.getDouble("avg_score");
                row.add(avg > 0
                        ? String.format("%.1f", avg) : "N/A");
                courseRows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        courseTable.setItems(courseRows);

        VBox courseCard = new VBox(12,
                new Label("Course Enrollment & Grade Report") {{
                    getStyleClass().add("section-title");
                }},
                courseTable);
        courseCard.getStyleClass().add("section-card");
        courseCard.setPadding(new javafx.geometry.Insets(20));

        view.getChildren().addAll(gpaCard, courseCard);
        contentArea.getChildren().add(view);
        updateStatus("Reports generated.");
    }

    // ── Navigation ────────────────────────────────────────────────

    private void setActiveNav(Button btn) {
        if (activeNavButton != null)
            activeNavButton.getStyleClass().remove("nav-active");
        btn.getStyleClass().add("nav-active");
        activeNavButton = btn;
    }

    @FXML private void handleNavDashboard() {
        pageTitle.setText("Admin Dashboard");
        setActiveNav(navDashboard);
        contentArea.getChildren().clear();
        setupStatCards();
        setupRecentStudentsTable();
        setupRecentLecturersTable();
        updateStatus("Dashboard");
    }

    @FXML private void handleNavStudents() {
        pageTitle.setText("Students");
        setActiveNav(navStudents);
        showStudentsView();
    }

    @FXML private void handleNavLecturers() {
        pageTitle.setText("Lecturers");
        setActiveNav(navLecturers);
        showLecturersView();
    }

    @FXML private void handleNavCourses() {
        pageTitle.setText("Courses");
        setActiveNav(navCourses);
        showCoursesView();
    }

    @FXML private void handleNavEnrollments() {
        pageTitle.setText("Enrollments");
        setActiveNav(navEnrollments);
        showEnrollmentsView();
    }

    @FXML private void handleNavReports() {
        pageTitle.setText("Reports");
        setActiveNav(navReports);
        showReportsView();
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Are you sure you want to logout?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                SessionManager.getInstance().clearSession();
                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/Login.fxml"));
                    Scene scene = new Scene(
                            loader.load(), 1100, 700);
                    scene.getStylesheets().add(
                            getClass().getResource(
                                            "/css/style.css")
                                    .toExternalForm());
                    Stage stage = (Stage) navDashboard
                            .getScene().getWindow();
                    stage.setScene(scene);
                    stage.centerOnScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void updateStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg,
                ButtonType.OK).showAndWait();
    }
}