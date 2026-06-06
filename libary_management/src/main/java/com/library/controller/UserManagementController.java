package com.library.controller;

import com.library.model.User;
import com.library.service.UserService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class UserManagementController {

    @FXML private Label lblFormTitle;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> cbRole;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Button btnSave;
    
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Long> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colStatus;

    private final UserService userService;
    private ObservableList<User> usersList = FXCollections.observableArrayList();

    private User selectedUser = null;

    public UserManagementController() {
        this.userService = new UserService();
    }

    @FXML
    public void initialize() {
        cbRole.setItems(FXCollections.observableArrayList("ADMIN", "LIBRARIAN"));
        cbRole.getSelectionModel().select("LIBRARIAN");

        cbStatus.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));
        cbStatus.getSelectionModel().select("ACTIVE");

        setupTable();
        loadUsers();

        // Listen for selection changes in table
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
            }
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        usersTable.setItems(usersList);
    }

    private void loadUsers() {
        Task<List<User>> loadTask = new Task<>() {
            @Override
            protected List<User> call() throws Exception {
                return userService.getAllUsers();
            }
        };

        loadTask.setOnSucceeded(e -> {
            usersList.setAll(loadTask.getValue());
        });

        loadTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load users: " + loadTask.getException().getMessage());
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void populateForm(User user) {
        selectedUser = user;
        lblFormTitle.setText("Edit User Details");
        btnSave.setText("Update User");
        
        txtUsername.setText(user.getUsername());
        txtUsername.setDisable(true); // Don't allow changing username
        txtPassword.clear();
        txtPassword.setPromptText("Leave blank to keep current");
        
        txtFullName.setText(user.getFullName());
        txtEmail.setText(user.getEmail() != null ? user.getEmail() : "");
        cbRole.getSelectionModel().select(user.getRole());
        cbStatus.getSelectionModel().select(user.getStatus());
    }

    @FXML
    private void handleSaveUser(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String role = cbRole.getValue();
        String status = cbStatus.getValue();

        if (username.isEmpty() || fullName.isEmpty() || role == null || status == null) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Please fill in all required fields.");
            return;
        }

        if (selectedUser == null && password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Warning", "Password is required for new users.");
            return;
        }

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (selectedUser == null) {
                    // Add new user
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setFullName(fullName);
                    newUser.setEmail(email.isEmpty() ? null : email);
                    newUser.setRole(role);
                    newUser.setStatus(status);
                    userService.addUser(newUser, password);
                } else {
                    // Update existing user
                    selectedUser.setFullName(fullName);
                    selectedUser.setEmail(email.isEmpty() ? null : email);
                    selectedUser.setRole(role);
                    selectedUser.setStatus(status);
                    userService.updateUser(selectedUser, password);
                }
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Success", selectedUser == null ? "User added successfully." : "User updated successfully.");
            handleClear(null);
            loadUsers(); // reload table
        });

        saveTask.setOnFailed(e -> {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save user: " + saveTask.getException().getMessage());
        });

        Thread thread = new Thread(saveTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleClear(ActionEvent event) {
        selectedUser = null;
        lblFormTitle.setText("Add New User");
        btnSave.setText("Add User");
        
        txtUsername.clear();
        txtUsername.setDisable(false);
        txtPassword.clear();
        txtPassword.setPromptText("Enter password");
        txtFullName.clear();
        txtEmail.clear();
        cbRole.getSelectionModel().select("LIBRARIAN");
        cbStatus.getSelectionModel().select("ACTIVE");
        
        usersTable.getSelectionModel().clearSelection();
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
