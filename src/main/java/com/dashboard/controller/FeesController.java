package com.dashboard.controller;

import com.dashboard.db.DatabaseManager;
import com.dashboard.util.SessionManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.ResultSet;

public class FeesController {

    @FXML private TableView<ObservableList<String>> feesTable;
    @FXML private TableColumn<ObservableList<String>, String> fDescCol;
    @FXML private TableColumn<ObservableList<String>, String> fSemCol;
    @FXML private TableColumn<ObservableList<String>, String> fAmountCol;
    @FXML private TableColumn<ObservableList<String>, String> fPaidCol;
    @FXML private TableColumn<ObservableList<String>, String> fBalanceCol;
    @FXML private TableColumn<ObservableList<String>, String> fStatusCol;
    @FXML private TableColumn<ObservableList<String>, String> fDueCol;

    @FXML private ListView<String> paymentHistoryList;
    @FXML private Label totalFeesLabel;
    @FXML private Label totalPaidLabel;
    @FXML private Label outstandingLabel;
    @FXML private Label feeStatusLabel;
    @FXML private Label feedbackLabel;

    private DatabaseManager db;
    private int             studentId;

    @FXML
    public void initialize() {
        db        = DatabaseManager.getInstance();
        studentId = SessionManager.getInstance()
                .getRoleSpecificId();

        setupTable();
        loadFees();
        loadPaymentHistory();
    }

    // ── Table Setup ───────────────────────────────────────────────

    private void setupTable() {
        fDescCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(0)));
        fSemCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(1)));
        fAmountCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(2)));
        fPaidCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(3)));
        fBalanceCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(4)));
        fStatusCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(5)));
        fDueCol.setCellValueFactory(
                d -> new SimpleStringProperty(
                        d.getValue().get(6)));
    }

    // ── Load Fees ─────────────────────────────────────────────────

    private void loadFees() {
        ObservableList<ObservableList<String>> rows =
                FXCollections.observableArrayList();
        double totalFees    = 0;
        double totalPaid    = 0;
        double totalBalance = 0;

        try {
            ResultSet rs = db.getFeesForStudent(studentId);
            while (rs.next()) {
                double amount  = rs.getDouble("amount");
                double paid    = rs.getDouble("paid");
                double balance = rs.getDouble("balance");
                String status  = rs.getString("status");

                totalFees    += amount;
                totalPaid    += paid;
                totalBalance += balance;

                String statusLabel =
                        "PAID".equals(status)    ? " Paid"
                                : "PARTIAL".equals(status)
                                ? "Partial"
                                : " Unpaid";

                ObservableList<String> row =
                        FXCollections.observableArrayList();
                row.add(rs.getString("description"));
                row.add(rs.getString("semester"));
                row.add(String.format("%.2f", amount));
                row.add(String.format("%.2f", paid));
                row.add(String.format("%.2f", balance));
                row.add(statusLabel);
                row.add(rs.getString("due_date"));
                // store fee id at index 7
                row.add(String.valueOf(rs.getInt("id")));
                rows.add(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        feesTable.setItems(rows);

        totalFeesLabel.setText(
                String.format("GH₵ %.2f", totalFees));
        totalPaidLabel.setText(
                String.format("GH₵ %.2f", totalPaid));
        outstandingLabel.setText(
                String.format("GH₵ %.2f", totalBalance));

        if (totalBalance <= 0) {
            feeStatusLabel.setText("CLEARED");
            feeStatusLabel.setStyle(
                    "-fx-text-fill: #006B3F; "
                            + "-fx-font-weight: bold;");
        } else if (totalPaid > 0) {
            feeStatusLabel.setText("PARTIAL");
            feeStatusLabel.setStyle(
                    "-fx-text-fill: #f39c12; "
                            + "-fx-font-weight: bold;");
        } else {
            feeStatusLabel.setText("OWING");
            feeStatusLabel.setStyle(
                    "-fx-text-fill: #c0392b; "
                            + "-fx-font-weight: bold;");
        }
    }

    // ── Payment History ───────────────────────────────────────────

    private void loadPaymentHistory() {
        ObservableList<String> items =
                FXCollections.observableArrayList();
        try {
            ResultSet rs = db.getPaymentHistoryForStudent(
                    studentId);
            while (rs.next()) {
                items.add(
                        String.format(
                                "  GH₵ %.2f  —  %s\n"
                                        + "        %s  |  %s  |  Ref: %s",
                                rs.getDouble("amount"),
                                rs.getString("description"),
                                rs.getString("paid_at"),
                                rs.getString("method"),
                                rs.getString("reference") != null
                                        ? rs.getString("reference")
                                        : "N/A"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (items.isEmpty())
            items.add("No payment records found.");

        paymentHistoryList.setItems(items);
    }
}