package com.library.controller;

import com.library.model.User;
import com.library.util.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminController {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnSettings;
    @FXML private Label lblAdminName;

    @FXML
    public void initialize() {
        User loggedInUser = UserSession.getInstance().getLoggedInUser();
        if (loggedInUser != null) {
            lblAdminName.setText(loggedInUser.getFullName() != null ? loggedInUser.getFullName() : loggedInUser.getUsername());
        }

        // Load default screen
        showDashboard(null);
    }

    @FXML
    private void showDashboard(ActionEvent event) {
        setActiveButton(btnDashboard);
        loadScreen("/fxml/admin/Dashboard.fxml");
    }

    @FXML
    private void showUsers(ActionEvent event) {
        setActiveButton(btnUsers);
        loadScreen("/fxml/admin/UserManagement.fxml");
    }

    @FXML
    private void showSettings(ActionEvent event) {
        setActiveButton(btnSettings);
        // loadScreen("/fxml/admin/Settings.fxml");
    }

    private void setActiveButton(Button activeButton) {
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("selected");
        if (btnUsers != null) btnUsers.getStyleClass().remove("selected");
        if (btnSettings != null) btnSettings.getStyleClass().remove("selected");

        if (activeButton != null && !activeButton.getStyleClass().contains("selected")) {
            activeButton.getStyleClass().add("selected");
        }
    }

    private void loadScreen(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node screen = loader.load();
            contentArea.getChildren().setAll(screen);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot load screen: " + fxmlPath);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Clear user session
        UserSession.getInstance().clearSession();

        // Switch to Login screen
        try {
            Stage currentStage = (Stage) contentArea.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Stage loginStage = new Stage();
            loginStage.setTitle("Library Management System - Login");
            loginStage.setScene(new Scene(root, 800, 600));
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot load login screen.");
        }
    }
}
