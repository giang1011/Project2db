package com.library.controller;

import com.library.model.User;
import com.library.service.AuthService;
import com.library.util.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Button btnClear;

    private final AuthService authService;

    public LoginController() {
        this.authService = new AuthService();
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Canh bao", "Vui long nhap tai khoan va mat khau!");
            return;
        }

        disableUI(true);

        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                return authService.authenticate(username, password);
            }
        };

        loginTask.setOnSucceeded(e -> {
            disableUI(false);
            User user = loginTask.getValue();
            if (user == null) {
                showAlert(Alert.AlertType.ERROR, "Loi dang nhap", "Sai tai khoan hoac mat khau!");
            } else if ("INACTIVE".equals(user.getStatus())) {
                showAlert(Alert.AlertType.ERROR, "Khoa tai khoan", "Tai khoan da bi khoa, khong cho phep vao he thong!");
            } else {
                // Dang nhap thanh cong, luu thong tin vao Session
                UserSession.getInstance().setLoggedInUser(user);
                
                // Chuyen huong den man hinh Dashboard theo role
                if ("ADMIN".equals(user.getRole())) {
                    switchToAdminDashboard();
                } else {
                    switchToDashboard();
                }
            }
        });

        loginTask.setOnFailed(e -> {
            disableUI(false);
            showAlert(Alert.AlertType.ERROR, "Loi he thong", "Co loi xay ra khi kiem tra tai khoan: " + loginTask.getException().getMessage());
        });

        Thread thread = new Thread(loginTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    void handleClear(ActionEvent event) {
        txtUsername.clear();
        txtPassword.clear();
    }

    private void switchToDashboard() {
        try {
            // Dong cua so Login hien tai
            Stage currentStage = (Stage) btnLogin.getScene().getWindow();
            currentStage.close();

            // Khoi tao va hien thi Scene Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/MainLayout.fxml"));
            Parent root = loader.load();
            
            Stage dashboardStage = new Stage();
            dashboardStage.setTitle("Library Management System - Librarian Portal");
            dashboardStage.setScene(new Scene(root, 1280, 800));
            dashboardStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loi chuyen man hinh", "Khong the tai man hinh Dashboard: " + e.getMessage());
        }
    }

    private void switchToAdminDashboard() {
        try {
            // Dong cua so Login hien tai
            Stage currentStage = (Stage) btnLogin.getScene().getWindow();
            currentStage.close();

            // Khoi tao va hien thi Scene Admin Dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/layout/AdminLayout.fxml"));
            Parent root = loader.load();
            
            Stage dashboardStage = new Stage();
            dashboardStage.setTitle("Library Management System - Admin Portal");
            dashboardStage.setScene(new Scene(root, 1280, 800));
            dashboardStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loi chuyen man hinh", "Khong the tai man hinh Admin: " + e.getMessage());
        }
    }

    private void disableUI(boolean disable) {
        btnLogin.setDisable(disable);
        btnClear.setDisable(disable);
        txtUsername.setDisable(disable);
        txtPassword.setDisable(disable);
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
