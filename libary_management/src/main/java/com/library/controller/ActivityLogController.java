package com.library.controller;

import com.library.model.ActivityLogDTO;
import com.library.service.ActivityLogService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ActivityLogController {

    @FXML private TextField txtSearch;
    @FXML private TableView<ActivityLogDTO> tblLogs;
    @FXML private TableColumn<ActivityLogDTO, String> colTime;
    @FXML private TableColumn<ActivityLogDTO, String> colUser;
    @FXML private TableColumn<ActivityLogDTO, String> colAction;
    @FXML private TableColumn<ActivityLogDTO, String> colOldValue;
    @FXML private TableColumn<ActivityLogDTO, String> colNewValue;

    private final ActivityLogService activityLogService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ActivityLogController() {
        this.activityLogService = new ActivityLogService();
    }

    @FXML
    public void initialize() {
        // Setup columns using lambda for Java Records compatibility
        colTime.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().createdAt().format(formatter)));
        colUser.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().userName()));
        colAction.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().action()));
        colOldValue.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().oldValue() != null ? cellData.getValue().oldValue() : ""));
        colNewValue.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().newValue() != null ? cellData.getValue().newValue() : ""));

        loadLogs("");
    }

    @FXML
    void handleSearch(ActionEvent event) {
        loadLogs(txtSearch.getText().trim());
    }

    @FXML
    void handleRefresh(ActionEvent event) {
        txtSearch.clear();
        loadLogs("");
    }

    private void loadLogs(String keyword) {
        tblLogs.setPlaceholder(new Label("Loading data..."));

        Task<List<ActivityLogDTO>> task = new Task<>() {
            @Override
            protected List<ActivityLogDTO> call() throws Exception {
                return activityLogService.getAllLogs(keyword);
            }
        };

        task.setOnSucceeded(e -> {
            List<ActivityLogDTO> logs = task.getValue();
            tblLogs.setItems(FXCollections.observableArrayList(logs));
            if (logs.isEmpty()) {
                tblLogs.setPlaceholder(new Label("No logs found."));
            }
        });

        task.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Cannot load activity logs: " + e.getSource().getException().getMessage());
                alert.showAndWait();
            });
        });

        // Use Java 21 Virtual Threads
        Thread.ofVirtual().name("ActivityLogLoader").start(task);
    }
}
