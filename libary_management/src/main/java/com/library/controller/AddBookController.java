package com.library.controller;

import com.library.model.Author;
import com.library.model.BookDTO;
import com.library.model.Category;
import com.library.model.Publisher;
import com.library.model.User;
import com.library.service.BookService;
import com.library.util.FileUtil;
import com.library.util.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddBookController {

    @FXML private TextField txtIsbn;
    @FXML private TextField txtTitle;
    @FXML private TextField txtPublicationYear;
    @FXML private TextField txtLanguage;
    @FXML private TextField txtPageCount;
    @FXML private TextField txtShelfLocation;
    @FXML private TextField txtCopyCount;
    @FXML private TextField txtBarcode;
    @FXML private TextArea txtDescription;
    
    @FXML private ComboBox<Publisher> cmbPublisher;
    @FXML private ComboBox<Author> cmbAuthor;
    @FXML private ComboBox<Category> cmbCategory;
    @FXML private FlowPane flowAuthors;
    @FXML private FlowPane flowCategories;
    @FXML private ComboBox<Publisher> cboPublisher; // fallback
    @FXML private ListView<Author> listAuthors;
    @FXML private ListView<Category> listCategories;
    
    @FXML private DatePicker dpAcquisitionDate;
    
    @FXML private ImageView imgCover;
    @FXML private Button btnChooseImage;
    @FXML private Button btnSaveBook;
    @FXML private Button btnCancel;

    private final BookService bookService;
    private File selectedImageFile;
    
    private final List<Author> selectedAuthorsList = new ArrayList<>();
    private final List<Category> selectedCategoriesList = new ArrayList<>();

    public AddBookController() {
        this.bookService = new BookService();
    }

    @FXML
    public void initialize() {
        // Cho phép chọn nhiều item
        if (listAuthors != null) {
            listAuthors.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }
        if (listCategories != null) {
            listCategories.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        }
        // Listeners cho Chips UI
        if (cmbAuthor != null) {
            cmbAuthor.setOnAction(e -> {
                Author selected = cmbAuthor.getValue();
                if (selected != null && !selectedAuthorsList.contains(selected)) {
                    selectedAuthorsList.add(selected);
                    addAuthorChip(selected);
                }
                Platform.runLater(() -> cmbAuthor.getSelectionModel().clearSelection());
            });
        }
        
        if (cmbCategory != null) {
            cmbCategory.setOnAction(e -> {
                Category selected = cmbCategory.getValue();
                if (selected != null && !selectedCategoriesList.contains(selected)) {
                    selectedCategoriesList.add(selected);
                    addCategoryChip(selected);
                }
                Platform.runLater(() -> cmbCategory.getSelectionModel().clearSelection());
            });
        }
        
        loadComboBoxData();
    }
    
    private void addAuthorChip(Author author) {
        if (flowAuthors == null) return;
        Button chip = new Button(author.getAuthorName() + "  x");
        chip.getStyleClass().addAll("accent", "button-outlined");
        chip.setStyle("-fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 3 8 3 8; -fx-font-size: 11px; -fx-cursor: hand;");
        chip.setOnAction(e -> {
            selectedAuthorsList.remove(author);
            flowAuthors.getChildren().remove(chip);
        });
        flowAuthors.getChildren().add(chip);
    }

    private void addCategoryChip(Category cat) {
        if (flowCategories == null) return;
        Button chip = new Button(cat.getCategoryName() + "  x");
        chip.getStyleClass().addAll("accent", "button-outlined");
        chip.setStyle("-fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 3 8 3 8; -fx-font-size: 11px; -fx-cursor: hand;");
        chip.setOnAction(e -> {
            selectedCategoriesList.remove(cat);
            flowCategories.getChildren().remove(chip);
        });
        flowCategories.getChildren().add(chip);
    }

    private void loadComboBoxData() {
        Task<Void> loadTask = new Task<>() {
            private List<Author> authors;
            private List<Category> categories;
            private List<Publisher> publishers;

            @Override
            protected Void call() throws Exception {
                authors = bookService.getAllAuthors();
                categories = bookService.getAllCategories();
                publishers = bookService.getAllPublishers();
                return null;
            }

            @Override
            protected void succeeded() {
                if (listAuthors != null && authors != null) listAuthors.getItems().setAll(authors);
                if (cmbAuthor != null && authors != null) cmbAuthor.getItems().setAll(authors);
                if (listCategories != null && categories != null) listCategories.getItems().setAll(categories);
                if (cmbCategory != null && categories != null) cmbCategory.getItems().setAll(categories);
                
                if (cboPublisher != null && publishers != null) cboPublisher.getItems().setAll(publishers);
                if (cmbPublisher != null && publishers != null) cmbPublisher.getItems().setAll(publishers);
            }

            @Override
            protected void failed() {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải dữ liệu khởi tạo: " + getException().getMessage());
            }
        };

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh bìa sách");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        Stage stage = (Stage) btnChooseImage.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            selectedImageFile = file;
            Image image = new Image(file.toURI().toString());
            if (imgCover != null) {
                imgCover.setImage(image);
            }
        }
    }

    @FXML
    private void handleAddAuthor(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm Tác Giả");
        dialog.setHeaderText("Thêm mới tác giả nhanh");
        dialog.setContentText("Tên tác giả:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                Author newAuthor = bookService.addAuthor(name);
                if (listAuthors != null) {
                    listAuthors.getItems().add(newAuthor);
                    listAuthors.getSelectionModel().select(newAuthor);
                }
                if (cmbAuthor != null) {
                    Platform.runLater(() -> {
                        cmbAuthor.getItems().add(newAuthor);
                        if (!selectedAuthorsList.contains(newAuthor)) {
                            selectedAuthorsList.add(newAuthor);
                            addAuthorChip(newAuthor);
                        }
                    });
                }
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm tác giả: " + name);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
            }
        });
    }

    @FXML
    private void handleAddCategory(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm Thể Loại");
        dialog.setHeaderText("Thêm mới thể loại nhanh");
        dialog.setContentText("Tên thể loại:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                Category newCategory = bookService.addCategory(name);
                if (listCategories != null) {
                    listCategories.getItems().add(newCategory);
                    listCategories.getSelectionModel().select(newCategory);
                }
                if (cmbCategory != null) {
                    Platform.runLater(() -> {
                        cmbCategory.getItems().add(newCategory);
                        if (!selectedCategoriesList.contains(newCategory)) {
                            selectedCategoriesList.add(newCategory);
                            addCategoryChip(newCategory);
                        }
                    });
                }
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm thể loại: " + name);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
            }
        });
    }

    @FXML
    private void handleAddPublisher(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Thêm Nhà Xuất Bản");
        dialog.setHeaderText("Thêm mới nhà xuất bản nhanh");
        dialog.setContentText("Tên nhà xuất bản:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            try {
                Publisher newPublisher = bookService.addPublisher(name);
                if (cboPublisher != null) {
                    cboPublisher.getItems().add(newPublisher);
                    cboPublisher.getSelectionModel().select(newPublisher);
                }
                if (cmbPublisher != null) {
                    cmbPublisher.getItems().add(newPublisher);
                    cmbPublisher.getSelectionModel().select(newPublisher);
                }
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm nhà xuất bản: " + name);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
            }
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        // Validation
        String isbn = txtIsbn != null ? txtIsbn.getText() : "";
        String title = txtTitle != null ? txtTitle.getText() : "";
        Publisher publisher = cmbPublisher != null ? cmbPublisher.getValue() : (cboPublisher != null ? cboPublisher.getValue() : null);
        
        List<Author> selectedAuthors = new ArrayList<>(selectedAuthorsList);
        if (listAuthors != null) selectedAuthors.addAll(listAuthors.getSelectionModel().getSelectedItems());
        
        List<Category> selectedCategories = new ArrayList<>(selectedCategoriesList);
        if (listCategories != null) selectedCategories.addAll(listCategories.getSelectionModel().getSelectedItems());
        
        if (isbn == null || isbn.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "ISBN không được để trống!");
            return;
        }
        if (title == null || title.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Tiêu đề không được để trống!");
            return;
        }
        if (selectedAuthors.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn ít nhất 1 Tác giả!");
            return;
        }
        if (selectedCategories.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn ít nhất 1 Thể loại!");
            return;
        }
        
        int year = 0;
        int pageCount = 0;
        int copyCount = 0;
        
        try {
            if (txtPublicationYear != null && txtPublicationYear.getText() != null && !txtPublicationYear.getText().trim().isEmpty()) {
                year = Integer.parseInt(txtPublicationYear.getText().trim());
            }
            if (txtPageCount != null && txtPageCount.getText() != null && !txtPageCount.getText().trim().isEmpty()) {
                pageCount = Integer.parseInt(txtPageCount.getText().trim());
                if (pageCount <= 0) throw new NumberFormatException();
            } else {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập số trang > 0!");
                return;
            }
            if (txtCopyCount != null && txtCopyCount.getText() != null && !txtCopyCount.getText().trim().isEmpty()) {
                copyCount = Integer.parseInt(txtCopyCount.getText().trim());
                if (copyCount <= 0) throw new NumberFormatException();
            } else {
                copyCount = 1; // Fallback to 1 if user FXML doesn't have copyCount field
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Năm xuất bản, Số trang, và Số lượng bản sao phải là số nguyên dương hợp lệ!");
            return;
        }

        // Map UI to DTO
        BookDTO bookDTO = new BookDTO();
        bookDTO.setIsbn(isbn.trim());
        bookDTO.setTitle(title.trim());
        bookDTO.setPublisherId(publisher != null ? publisher.getPublisherId() : 0);
        bookDTO.setPublicationYear(year);
        bookDTO.setLanguage(txtLanguage != null && txtLanguage.getText() != null ? txtLanguage.getText().trim() : "");
        bookDTO.setDescription(txtDescription != null && txtDescription.getText() != null ? txtDescription.getText().trim() : "");
        bookDTO.setPageCount(pageCount);
        bookDTO.setCopyCount(copyCount);
        bookDTO.setShelfLocation(txtShelfLocation != null && txtShelfLocation.getText() != null ? txtShelfLocation.getText().trim() : "");
        if (dpAcquisitionDate != null) {
            bookDTO.setAcquisitionDate(dpAcquisitionDate.getValue());
        }
        
        // Extract IDs for mapping
        List<Integer> authorIds = new ArrayList<>();
        for (Author a : selectedAuthors) authorIds.add(a.getAuthorId());
        bookDTO.setAuthorIds(authorIds);

        List<Integer> categoryIds = new ArrayList<>();
        for (Category c : selectedCategories) categoryIds.add(c.getCategoryId());
        bookDTO.setCategoryIds(categoryIds);
        
        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser != null) {
            bookDTO.setCreatedBy(currentUser.getUserId());
        }

        if (btnSaveBook != null) btnSaveBook.setDisable(true);
        
        final int finalCopyCount = copyCount;

        // Run database operation in background Task (Anti-freeze rule)
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // 1. Process Image upload first
                String coverImagePath = null;
                if (selectedImageFile != null) {
                    coverImagePath = FileUtil.saveImage(selectedImageFile);
                }
                bookDTO.setCoverImage(coverImagePath);
                
                // 2. Start full transaction (Books, BookAuthors, BookCategories, BookCopies)
                bookService.addBook(bookDTO);
                
                return null;
            }

            @Override
            protected void succeeded() {
                if (btnSaveBook != null) btnSaveBook.setDisable(false);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã thêm mới " + finalCopyCount + " bản sao của sách thành công!");
                closeWindow();
            }

            @Override
            protected void failed() {
                if (btnSaveBook != null) btnSaveBook.setDisable(false);
                Throwable ex = getException();
                if (ex.getMessage() != null && (ex.getMessage().contains("UNIQUE KEY") || ex.getMessage().contains("violation"))) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Dữ Liệu", "Mã ISBN đã tồn tại trong hệ thống!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi Hệ Thống", "Thêm sách thất bại: " + (ex.getMessage() != null ? ex.getMessage() : "Không xác định"));
                }
                ex.printStackTrace();
            }
        };

        Thread thread = new Thread(saveTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void closeWindow() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/librarian/Dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Node source = btnCancel != null ? btnCancel : (btnSaveBook != null ? btnSaveBook : null);
            if (source != null && source.getScene() != null) {
                javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) source.getScene().lookup("#contentArea");
                if (contentArea != null) {
                    contentArea.getChildren().setAll(root);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback (if window mode)
            if (btnSaveBook != null && btnSaveBook.getScene() != null) {
                Stage stage = (Stage) btnSaveBook.getScene().getWindow();
                stage.close();
            }
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
