package com.library.controller;

import com.library.model.Book;
import com.library.service.BookService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
public class BookManagementController {

    @FXML private TableView<Book> tableBooks;
    @FXML private TableColumn<Book, Long> colId;
    @FXML private TableColumn<Book, String> colCover;
    @FXML private TableColumn<Book, String> colIsbn;
    @FXML private TableColumn<Book, String> colTitle;
    @FXML private TableColumn<Book, String> colAuthors;
    @FXML private TableColumn<Book, String> colCategories;
    @FXML private TableColumn<Book, String> colPublisher;
    @FXML private TableColumn<Book, String> colCopies;
    @FXML private TableColumn<Book, String> colStatus;
    @FXML private TableColumn<Book, Void> colActions;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbCategoryFilter;

    private final BookService bookService;
    private final ObservableList<Book> masterData = FXCollections.observableArrayList();

    public BookManagementController() {
        this.bookService = new BookService();
    }

    @FXML
    public void initialize() {
        setupTable();
        loadBooks();
        
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterData());
        if (cmbCategoryFilter != null) {
            cmbCategoryFilter.valueProperty().addListener((observable, oldValue, newValue) -> filterData());
        }
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        colCover.setCellValueFactory(new PropertyValueFactory<>("coverImage"));
        
        colCover.setCellFactory(column -> {
            return new TableCell<Book, String>() {
                private final ImageView imageView = new ImageView();
                {
                    imageView.setFitWidth(80);
                    imageView.setFitHeight(110);
                    imageView.setPreserveRatio(true);
                }

                @Override
                protected void updateItem(String imagePath, boolean empty) {
                    super.updateItem(imagePath, empty);
                    if (empty || imagePath == null || imagePath.trim().isEmpty()) {
                        setGraphic(null);
                    } else {
                        File file = new File(imagePath);
                        if (file.exists()) {
                            Image image = new Image(file.toURI().toString(), 80, 110, true, true);
                            imageView.setImage(image);
                            setGraphic(imageView);
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            };
        });

        colIsbn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colAuthors.setCellValueFactory(new PropertyValueFactory<>("authors"));
        colCategories.setCellValueFactory(new PropertyValueFactory<>("categories"));
        colPublisher.setCellValueFactory(new PropertyValueFactory<>("publisherName"));
        
        colCopies.setCellFactory(column -> {
            return new TableCell<Book, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                    } else {
                        Book book = (Book) getTableRow().getItem();
                        setText(book.getAvailableCopies() + " / " + book.getTotalCopies());
                    }
                }
            };
        });

        if (colStatus != null) {
            colStatus.setCellFactory(column -> new TableCell<Book, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        Book book = (Book) getTableRow().getItem();
                        Label statusLabel = new Label();
                        statusLabel.setStyle("-fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12;");
                        if (book.getAvailableCopies() > 0) {
                            statusLabel.setText("Còn sách");
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;");
                        } else if (book.getTotalCopies() > 0 && book.getLostCopies() == book.getTotalCopies()) {
                            statusLabel.setText("Lost");
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #E5E7EB; -fx-text-fill: #374151;");
                        } else if (book.getLostCopies() > 0 && book.getAvailableCopies() == 0) {
                            statusLabel.setText("Lost / Đang mượn");
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FEF3C7; -fx-text-fill: #B45309;");
                        } else {
                            statusLabel.setText("Đang mượn hết");
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;");
                        }
                        setGraphic(statusLabel);
                    }
                }
            });
        }

        colActions.setCellFactory(column -> {
            return new TableCell<Book, Void>() {
                private final Button btnEdit = new Button("Sửa");
                private final HBox pane = new HBox(5, btnEdit);

                {
                    btnEdit.getStyleClass().addAll("button-outlined", "accent");
                    
                    btnEdit.setOnAction(e -> {
                        Book book = getTableView().getItems().get(getIndex());
                        openEditDialog(book);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(pane);
                    }
                }
            };
        });
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadBooks();
    }

    private void loadBooks() {
        Task<List<Book>> loadTask = new Task<>() {
            @Override
            protected List<Book> call() throws Exception {
                return bookService.getAllBooksDetails();
            }

            @Override
            protected void succeeded() {
                masterData.setAll(getValue());
                populateFilters();
                filterData();
            }

            @Override
            protected void failed() {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Không thể tải danh sách sách");
                alert.setContentText(getException().getMessage());
                alert.showAndWait();
            }
        };

        new Thread(loadTask).start();
    }

    private void populateFilters() {
        Set<String> categories = new TreeSet<>();
        
        for (Book book : masterData) {
            if (book.getCategories() != null) {
                String[] cats = book.getCategories().split(",");
                for (String c : cats) categories.add(c.trim());
            }
        }
        
        if (cmbCategoryFilter != null) {
            cmbCategoryFilter.getItems().clear();
            cmbCategoryFilter.getItems().add("All Categories");
            cmbCategoryFilter.getItems().addAll(categories);
            cmbCategoryFilter.getSelectionModel().selectFirst();
        }
    }

    private void filterData() {
        String keyword = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String selectedCategory = cmbCategoryFilter != null ? cmbCategoryFilter.getValue() : null;
        
        boolean isCategoryFilterActive = selectedCategory != null && !"All Categories".equals(selectedCategory);
        
        if (keyword.isEmpty() && !isCategoryFilterActive) {
            tableBooks.setItems(masterData);
            return;
        }
        
        ObservableList<Book> filteredData = FXCollections.observableArrayList();
        
        for (Book book : masterData) {
            boolean matchesSearch = keyword.isEmpty() ||
                (book.getTitle() != null && book.getTitle().toLowerCase().contains(keyword)) ||
                (book.getIsbn() != null && book.getIsbn().toLowerCase().contains(keyword)) ||
                (book.getAuthors() != null && book.getAuthors().toLowerCase().contains(keyword)) ||
                (book.getCategories() != null && book.getCategories().toLowerCase().contains(keyword));
                
            boolean matchesCategory = !isCategoryFilterActive || 
                (book.getCategories() != null && book.getCategories().contains(selectedCategory));
                
            if (matchesSearch && matchesCategory) {
                filteredData.add(book);
            }
        }
        tableBooks.setItems(filteredData);
    }

    private void openEditDialog(Book book) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/librarian/edit_book.fxml"));
            javafx.scene.Parent root = loader.load();
            
            EditBookController controller = loader.getController();
            controller.initData(book, this);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Sửa thông tin sách - " + book.getIsbn());
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể mở cửa sổ sửa sách");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleRecoverLostBook(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Khôi phục sách báo mất");
        dialog.setHeaderText("Nhập mã vạch (Barcode) của cuốn sách bạn vừa tìm thấy:");
        dialog.setContentText("Mã vạch:");

        dialog.showAndWait().ifPresent(barcode -> {
            if (!barcode.trim().isEmpty()) {
                Task<Boolean> recoverTask = new Task<>() {
                    @Override
                    protected Boolean call() throws Exception {
                        return bookService.recoverLostBookCopy(barcode.trim());
                    }

                    @Override
                    protected void succeeded() {
                        if (getValue()) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Thành công");
                            alert.setHeaderText(null);
                            alert.setContentText("Đã khôi phục thành công mã vạch: " + barcode);
                            alert.showAndWait();
                            loadBooks();
                        } else {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Thất bại");
                            alert.setHeaderText(null);
                            alert.setContentText("Không tìm thấy mã vạch này hoặc cuốn sách không ở trạng thái LOST.");
                            alert.showAndWait();
                        }
                    }

                    @Override
                    protected void failed() {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Lỗi");
                        alert.setHeaderText("Không thể khôi phục sách");
                        alert.setContentText(getException().getMessage());
                        alert.showAndWait();
                    }
                };
                new Thread(recoverTask).start();
            }
        });
    }
}
