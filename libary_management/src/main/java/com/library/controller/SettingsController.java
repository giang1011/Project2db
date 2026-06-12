package com.library.controller;

import com.library.model.SystemSetting;
import com.library.repository.SystemSettingRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SettingsController {

    @FXML private TableView<SystemSetting> settingsTable;
    @FXML private TableColumn<SystemSetting, String> colKey;
    @FXML private TableColumn<SystemSetting, String> colValue;
    @FXML private TableColumn<SystemSetting, String> colDataType;
    @FXML private TableColumn<SystemSetting, String> colDescription;
    @FXML private TableColumn<SystemSetting, LocalDateTime> colUpdatedAt;

    @FXML private TextField txtSettingKey;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtDataType;
    @FXML private TextField txtSettingValue;
    @FXML private Button btnSave;

    private final SystemSettingRepository settingRepo;
    private ObservableList<SystemSetting> settingsList = FXCollections.observableArrayList();
    private SystemSetting selectedSetting;

    public SettingsController() {
        this.settingRepo = new SystemSettingRepository();
    }

    @FXML
    public void initialize() {
        setupTable();
        loadSettings();

        settingsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showSettingDetails(newVal);
            }
        });
    }

    private void setupTable() {
        colKey.setCellValueFactory(new PropertyValueFactory<>("settingKey"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("settingValue"));
        colDataType.setCellValueFactory(new PropertyValueFactory<>("dataType"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        colUpdatedAt.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        colUpdatedAt.setCellFactory(column -> new TableCell<SystemSetting, LocalDateTime>() {
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
    }

    private void loadSettings() {
        Task<java.util.List<SystemSetting>> loadTask = new Task<>() {
            @Override
            protected java.util.List<SystemSetting> call() throws Exception {
                return settingRepo.findAll();
            }
        };

        loadTask.setOnSucceeded(e -> {
            settingsList.setAll(loadTask.getValue());
            settingsTable.setItems(settingsList);
        });

        loadTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải cấu hình hệ thống: " + loadTask.getException().getMessage());
        });

        Thread.ofVirtual().start(loadTask);
    }

    private void showSettingDetails(SystemSetting setting) {
        this.selectedSetting = setting;
        txtSettingKey.setText(setting.getSettingKey());
        txtDescription.setText(setting.getDescription());
        txtDataType.setText(setting.getDataType());
        txtSettingValue.setText(setting.getSettingValue());
        
        // Only Value is editable
        txtSettingKey.setEditable(false);
        txtDescription.setEditable(false);
        txtDataType.setEditable(false);
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (selectedSetting == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn cấu hình cần thay đổi từ bảng.");
            return;
        }

        String newValue = txtSettingValue.getText().trim();
        
        // Validate Data Type
        if (!validateDataType(newValue, selectedSetting.getDataType())) {
            return;
        }

        btnSave.setDisable(true);
        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return settingRepo.updateSettingValue(selectedSetting.getSettingId(), newValue);
            }
        };

        saveTask.setOnSucceeded(e -> {
            btnSave.setDisable(false);
            if (saveTask.getValue()) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật cấu hình thành công!");
                loadSettings(); // Reload to get fresh data
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Cập nhật thất bại, vui lòng thử lại.");
            }
        });

        saveTask.setOnFailed(e -> {
            btnSave.setDisable(false);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Có lỗi xảy ra: " + saveTask.getException().getMessage());
        });

        Thread.ofVirtual().start(saveTask);
    }

    private boolean validateDataType(String value, String dataType) {
        try {
            switch (dataType) {
                case "INTEGER":
                    Integer.parseInt(value);
                    break;
                case "DECIMAL":
                    new BigDecimal(value);
                    break;
                case "BOOLEAN":
                    if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                        showAlert(Alert.AlertType.WARNING, "Sai định dạng", "Giá trị BOOLEAN chỉ chấp nhận 'true' hoặc 'false'.");
                        return false;
                    }
                    break;
                case "STRING":
                    if (value.isEmpty()) {
                        showAlert(Alert.AlertType.WARNING, "Sai định dạng", "Giá trị chuỗi không được để trống.");
                        return false;
                    }
                    break;
            }
            return true;
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Sai định dạng", "Giá trị không đúng với kiểu dữ liệu " + dataType + ".");
            return false;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}
