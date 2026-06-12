package com.library.controller;

import com.library.model.AdminStatisticsDTO;
import com.library.service.AdminStatisticsService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

public class AdminDashboardController {

    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;
    @FXML private Button btnFilter;

    @FXML private Label lblTotalBorrow;
    @FXML private Label lblTotalReturned;
    @FXML private Label lblTotalFines;

    @FXML private LineChart<String, Number> lineChart;
    @FXML private PieChart pieChart;

    @FXML private TableView<AdminStatisticsDTO.TopItem> tblTopBooks;
    @FXML private TableColumn<AdminStatisticsDTO.TopItem, String> colBookName;
    @FXML private TableColumn<AdminStatisticsDTO.TopItem, Integer> colBookCount;

    @FXML private TableView<AdminStatisticsDTO.TopItem> tblTopMembers;
    @FXML private TableColumn<AdminStatisticsDTO.TopItem, String> colMemberName;
    @FXML private TableColumn<AdminStatisticsDTO.TopItem, Integer> colMemberCount;

    private final AdminStatisticsService statisticsService;
    private final NumberFormat currencyFormat;

    public AdminDashboardController() {
        this.statisticsService = new AdminStatisticsService();
        this.currencyFormat = NumberFormat.getInstance(Locale.of("vi", "VN"));
    }

    @FXML
    public void initialize() {
        // Setup table columns using lambdas to cleanly support Java Records
        colBookName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().name()));
        colBookCount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().count()));
        
        colMemberName.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().name()));
        colMemberCount.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue().count()));

        // Default date range: 30 days ago to today
        dpTo.setValue(LocalDate.now());
        dpFrom.setValue(LocalDate.now().minusDays(30));

        loadData();
    }

    @FXML
    void handleFilter(ActionEvent event) {
        if (dpFrom.getValue() == null || dpTo.getValue() == null) {
            showAlert("Warning", "Please select both from and to dates.");
            return;
        }
        if (dpFrom.getValue().isAfter(dpTo.getValue())) {
            showAlert("Warning", "'From' date must be before 'To' date.");
            return;
        }
        loadData();
    }

    private void loadData() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        
        btnFilter.setDisable(true);

        Task<AdminStatisticsDTO> task = new Task<>() {
            @Override
            protected AdminStatisticsDTO call() throws Exception {
                return statisticsService.getStatistics(from, to);
            }
        };

        task.setOnSucceeded(e -> {
            AdminStatisticsDTO data = task.getValue();
            updateUI(data);
            btnFilter.setDisable(false);
        });

        task.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            showAlert("Error", "Failed to load statistics: " + e.getSource().getException().getMessage());
            btnFilter.setDisable(false);
        });

        // Use Java 21 Virtual Threads
        Thread.ofVirtual().name("AdminStatsLoader").start(task);
    }

    private void updateUI(AdminStatisticsDTO data) {
        // Update labels
        lblTotalBorrow.setText(String.valueOf(data.totalBorrow()));
        lblTotalReturned.setText(String.valueOf(data.totalReturned()));
        lblTotalFines.setText(currencyFormat.format(data.totalFines()) + " ₫");

        // Update Line Chart
        lineChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Borrowed Books");
        for (Map.Entry<String, Integer> entry : data.borrowedBooksByDate().entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        lineChart.getData().add(series);

        // Update Pie Chart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : data.booksByCategory().entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        pieChart.setData(pieData);

        // Update Tables
        tblTopBooks.setItems(FXCollections.observableArrayList(data.topBooks()));
        tblTopMembers.setItems(FXCollections.observableArrayList(data.topMembers()));
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
