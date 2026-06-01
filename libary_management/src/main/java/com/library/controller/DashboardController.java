package com.library.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.library.model.DashboardMetrics;
import com.library.model.DashboardTransactionDTO;
import com.library.service.DashboardService;

public class DashboardController {
    
    @FXML private Label lblBorrowing;
    @FXML private Label lblOverdue;
    @FXML private Label lblPendingProfiles;
    @FXML private Label lblUnpaidFines;

    @FXML private TableView<DashboardTransactionDTO> recentTransactionsTable;
    @FXML private TableColumn<DashboardTransactionDTO, Long> colTransId;
    @FXML private TableColumn<DashboardTransactionDTO, String> colMemberCode;
    @FXML private TableColumn<DashboardTransactionDTO, String> colMemberName;
    @FXML private TableColumn<DashboardTransactionDTO, Integer> colBooksCount;
    @FXML private TableColumn<DashboardTransactionDTO, LocalDateTime> colBorrowDate;
    @FXML private TableColumn<DashboardTransactionDTO, String> colProcessedBy;

    private DashboardService dashboardService;

    public DashboardController() {
        dashboardService = new DashboardService();
    }

    @FXML
    public void initialize() {
        setupTable();
        loadDashboardData();
    }

    private void setupTable() {
        colTransId.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        colMemberCode.setCellValueFactory(new PropertyValueFactory<>("memberCode"));
        colMemberName.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        colBooksCount.setCellValueFactory(new PropertyValueFactory<>("booksBorrowedCount"));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colBorrowDate.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        colBorrowDate.setCellFactory(column -> new TableCell<DashboardTransactionDTO, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });
        
        colProcessedBy.setCellValueFactory(new PropertyValueFactory<>("processedBy"));
    }

    private void loadDashboardData() {
        Task<DashboardData> loadTask = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                DashboardMetrics metrics = dashboardService.getMetrics();
                List<DashboardTransactionDTO> transactions = dashboardService.getTodaysTransactions();
                return new DashboardData(metrics, transactions);
            }

            @Override
            protected void succeeded() {
                DashboardData data = getValue();
                if (lblBorrowing != null) lblBorrowing.setText(String.valueOf(data.metrics.getBorrowingBooks()));
                if (lblOverdue != null) lblOverdue.setText(String.valueOf(data.metrics.getOverdueBooks()));
                if (lblPendingProfiles != null) lblPendingProfiles.setText(String.valueOf(data.metrics.getPendingProfiles()));
                if (lblUnpaidFines != null) lblUnpaidFines.setText(String.valueOf(data.metrics.getUnpaidFines()));
                
                if (recentTransactionsTable != null) {
                    recentTransactionsTable.setItems(FXCollections.observableArrayList(data.transactions));
                }
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lỗi");
                    alert.setHeaderText("Không thể tải dữ liệu Dashboard");
                    alert.setContentText(getException().getMessage());
                    alert.showAndWait();
                });
            }
        };

        new Thread(loadTask).start();
    }

    private static class DashboardData {
        DashboardMetrics metrics;
        List<DashboardTransactionDTO> transactions;
        DashboardData(DashboardMetrics metrics, List<DashboardTransactionDTO> transactions) {
            this.metrics = metrics;
            this.transactions = transactions;
        }
    }

    @FXML
    private void handleAddMember(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_member.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Thêm Độc Giả");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Không thể mở cửa sổ Thêm Độc Giả: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleAddBook(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/add_book.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Node source = (javafx.scene.Node) event.getSource();
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) source.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Không thể chuyển sang màn hình Thêm Sách: " + e.getMessage());
            alert.showAndWait();
        }
    }
}
