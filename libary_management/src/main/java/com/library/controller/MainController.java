package com.library.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnCheckout;
    @FXML private Button btnCheckin;
    @FXML private Button btnMembers;
    @FXML private Button btnAddBook;
    @FXML private Button btnManageBooks;
    @FXML private Button btnFines;
    @FXML
    public void initialize() {
        // Mặc định load màn hình Dashboard
        showDashboard(null);
    }

    @FXML
    private void showDashboard(ActionEvent event) {
        setActiveButton(btnDashboard);
        loadScreen("/fxml/librarian/Dashboard.fxml");
    }

    @FXML
    private void showCheckout(ActionEvent event) {
        setActiveButton(btnCheckout);
        loadScreen("/fxml/librarian/Checkout.fxml");
    }

    @FXML
    private void showCheckin(ActionEvent event) {
        setActiveButton(btnCheckin);
        loadScreen("/fxml/librarian/Checkin.fxml");
    }

    @FXML
    private void showMembers(ActionEvent event) {
        setActiveButton(btnMembers);
        loadScreen("/fxml/librarian/MemberManagement.fxml");
    }

    @FXML
    private void showAddBook(ActionEvent event) {
        setActiveButton(btnAddBook);
        loadScreen("/fxml/add_book.fxml");
    }

    @FXML
    private void showManageBooks(ActionEvent event) {
        setActiveButton(btnManageBooks);
        loadScreen("/fxml/manage_books.fxml");
    }

    @FXML
    private void showFines(ActionEvent event) {
        setActiveButton(btnFines);
        loadScreen("/fxml/manage_fines.fxml");
    }

    private void setActiveButton(Button activeButton) {
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("selected");
        if (btnCheckout != null) btnCheckout.getStyleClass().remove("selected");
        if (btnCheckin != null) btnCheckin.getStyleClass().remove("selected");
        if (btnMembers != null) btnMembers.getStyleClass().remove("selected");
        if (btnAddBook != null) btnAddBook.getStyleClass().remove("selected");
        if (btnManageBooks != null) btnManageBooks.getStyleClass().remove("selected");
        if (btnFines != null) btnFines.getStyleClass().remove("selected");

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
        com.library.util.UserSession.getInstance().clearSession();

        // Switch to Login screen
        try {
            Stage currentStage = (Stage) contentArea.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            javafx.scene.Parent root = loader.load();
            
            Stage loginStage = new Stage();
            loginStage.setTitle("Library Management System - Login");
            loginStage.setScene(new javafx.scene.Scene(root, 800, 600));
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Cannot load login screen.");
        }
    }
}
