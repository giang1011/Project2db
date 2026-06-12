package com.library.controller;

import com.library.model.Book;
import com.library.service.BookService;
import com.library.util.DatabaseConnection;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class EditBookController {

    @FXML private TextField txtTitle;
    @FXML private TextField txtPublicationYear;
    @FXML private TextField txtLanguage;
    @FXML private TextField txtPageCount;
    @FXML private TextArea txtDescription;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    
    @FXML private javafx.scene.control.TableView<com.library.model.BookCopyDTO> tableCopies;
    @FXML private javafx.scene.control.TableColumn<com.library.model.BookCopyDTO, String> colBarcode;
    @FXML private javafx.scene.control.TableColumn<com.library.model.BookCopyDTO, String> colShelf;
    @FXML private javafx.scene.control.TableColumn<com.library.model.BookCopyDTO, String> colCondition;
    @FXML private javafx.scene.control.TableColumn<com.library.model.BookCopyDTO, String> colCirculation;
    @FXML private javafx.scene.control.TableColumn<com.library.model.BookCopyDTO, Void> colAction;

    private Book currentBook;
    private final BookService bookService = new BookService();
    private BookManagementController parentController;

    public void initData(Book book, BookManagementController parent) {
        this.currentBook = book;
        this.parentController = parent;
        
        txtTitle.setText(book.getTitle());
        txtPublicationYear.setText(String.valueOf(book.getPublicationYear()));
        txtLanguage.setText(book.getLanguage() != null ? book.getLanguage() : "");
        txtPageCount.setText(String.valueOf(book.getPageCount()));
        
        // Fetch description from DB because Book model in table doesn't have it
        loadDescription(book.getBookId());
        
        setupCopiesTable();
        loadCopies();
    }

    private void loadDescription(long bookId) {
        Task<String> loadDesc = new Task<>() {
            @Override
            protected String call() throws Exception {
                String desc = "";
                String query = "SELECT Description FROM Books WHERE BookID = ?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setLong(1, bookId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            desc = rs.getString("Description");
                        }
                    }
                }
                return desc != null ? desc : "";
            }
            @Override
            protected void succeeded() {
                txtDescription.setText(getValue());
            }
        };
        new Thread(loadDesc).start();
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String title = txtTitle.getText().trim();
        if (title.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Tiêu đề không được để trống!");
            return;
        }
        
        int year = 0;
        int pages = 0;
        try {
            if (!txtPublicationYear.getText().trim().isEmpty()) {
                year = Integer.parseInt(txtPublicationYear.getText().trim());
            }
            if (!txtPageCount.getText().trim().isEmpty()) {
                pages = Integer.parseInt(txtPageCount.getText().trim());
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Năm xuất bản và số trang phải là số hợp lệ!");
            return;
        }

        btnSave.setDisable(true);
        
        final String fTitle = title;
        final int fYear = year;
        final int fPages = pages;
        final String fLanguage = txtLanguage.getText().trim();
        final String fDesc = txtDescription.getText().trim();
        
        Task<Void> updateTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String query = "UPDATE Books SET Title=?, PublicationYear=?, Language=?, PageCount=?, Description=? WHERE BookID=?";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, fTitle);
                    stmt.setInt(2, fYear);
                    stmt.setString(3, fLanguage);
                    stmt.setInt(4, fPages);
                    stmt.setString(5, fDesc);
                    stmt.setLong(6, currentBook.getBookId());
                    stmt.executeUpdate();
                }
                
                // Log action
                try {
                    com.library.service.ActivityLogService logService = new com.library.service.ActivityLogService();
                    long userId = 1;
                    com.library.model.User user = com.library.util.UserSession.getInstance().getLoggedInUser();
                    if (user != null) userId = user.getUserId();
                    logService.logAction(userId, "Update Book", null, "Updated Book ID: " + currentBook.getBookId());
                } catch (Exception ignored) {}
                
                return null;
            }

            @Override
            protected void succeeded() {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Cập nhật thông tin sách thành công!");
                if (parentController != null) {
                    try {
                        java.lang.reflect.Method method = BookManagementController.class.getDeclaredMethod("loadBooks");
                        method.setAccessible(true);
                        method.invoke(parentController);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                closeWindow();
            }

            @Override
            protected void failed() {
                btnSave.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật: " + getException().getMessage());
            }
        };

        new Thread(updateTask).start();
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
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

    private void setupCopiesTable() {
        if (colAction == null) return;
        colAction.setCellFactory(param -> new javafx.scene.control.TableCell<>() {
            private final Button btnRecover = new Button("Phục hồi");
            {
                btnRecover.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 4 8; -fx-border-radius: 4; -fx-background-radius: 4;");
                btnRecover.setOnAction(e -> {
                    com.library.model.BookCopyDTO copy = getTableView().getItems().get(getIndex());
                    if ("LOST".equals(copy.getCirculationStatus())) {
                        recoverCopy(copy.getBarcode());
                    } else {
                        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Bản sao này không ở trạng thái LOST.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    com.library.model.BookCopyDTO copy = (com.library.model.BookCopyDTO) getTableRow().getItem();
                    if ("LOST".equals(copy.getCirculationStatus())) {
                        setGraphic(btnRecover);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    @FXML
    private void loadCopies() {
        if (currentBook == null) return;
        Task<java.util.List<com.library.model.BookCopyDTO>> task = new Task<>() {
            @Override
            protected java.util.List<com.library.model.BookCopyDTO> call() throws Exception {
                return bookService.getBookCopies(currentBook.getBookId());
            }

            @Override
            protected void succeeded() {
                if (tableCopies != null) {
                    tableCopies.getItems().setAll(getValue());
                }
            }
        };
        new Thread(task).start();
    }

    private void recoverCopy(String barcode) {
        Task<Boolean> recoverTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return bookService.recoverLostBookCopy(barcode);
            }

            @Override
            protected void succeeded() {
                if (getValue()) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã khôi phục thành công mã vạch: " + barcode);
                    loadCopies(); // reload table
                    if (parentController != null) {
                        try {
                            java.lang.reflect.Method method = BookManagementController.class.getDeclaredMethod("loadBooks");
                            method.setAccessible(true);
                            method.invoke(parentController);
                        } catch (Exception ignored) {}
                    }
                } else {
                    showAlert(Alert.AlertType.WARNING, "Thất bại", "Không thể khôi phục mã vạch này.");
                }
            }
        };
        new Thread(recoverTask).start();
    }
}

