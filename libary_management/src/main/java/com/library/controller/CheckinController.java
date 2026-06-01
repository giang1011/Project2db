package com.library.controller;

import com.library.model.ReturnItemDTO;
import com.library.model.User;
import com.library.service.BorrowService;
import com.library.util.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.io.File;
import java.util.List;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class CheckinController {

    @FXML private TextField txtMemberCode;
    @FXML private Button btnSearchMember;
    @FXML private Label lblBorrowedCount;
    @FXML private TableView<ReturnItemDTO> tblBorrowedBooks;
    @FXML private TableColumn<ReturnItemDTO, String> colImage;
    @FXML private TableColumn<ReturnItemDTO, String> colBarcode;
    @FXML private TableColumn<ReturnItemDTO, String> colTitle;
    @FXML private TableColumn<ReturnItemDTO, LocalDate> colDueDate;
    
    private ObservableList<ReturnItemDTO> borrowedBooksList = FXCollections.observableArrayList();
    
    @FXML private VBox boxBookDetails;
    @FXML private Label lblBookTitle;
    @FXML private Label lblBorrowerName;
    @FXML private Label lblDueDate;
    @FXML private Label lblOverdueStatus;
    
    @FXML private ComboBox<String> cmbCondition;
    @FXML private VBox boxDamageNotes;
    @FXML private TextArea txtNotes;
    
    @FXML private VBox boxFines;
    @FXML private Label lblFineAmount;
    @FXML private Label lblFineReason;
    @FXML private Button btnPayFine;
    @FXML private Button btnConfirmCheckin;

    private BorrowService borrowService;
    private ReturnItemDTO currentItem;
    private BigDecimal currentFineAmount = BigDecimal.ZERO;
    private boolean isFinePaid = false;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CheckinController() {
        borrowService = new BorrowService();
    }

    @FXML
    public void initialize() {
        if (cmbCondition != null) {
            cmbCondition.getItems().addAll("GOOD", "DAMAGED", "LOST");
            cmbCondition.valueProperty().addListener((obs, oldVal, newVal) -> {
                handleConditionChange(newVal);
            });
        }

        if (btnSearchMember != null) btnSearchMember.setOnAction(e -> handleSearchMember());
        if (txtMemberCode != null) txtMemberCode.setOnAction(e -> handleSearchMember()); // Enter

        if (colImage != null) {
            colImage.setCellValueFactory(new PropertyValueFactory<>("coverImage"));
            colImage.setCellFactory(column -> {
                return new TableCell<ReturnItemDTO, String>() {
                    private final ImageView imageView = new ImageView();
                    {
                        imageView.setFitWidth(80);
                        imageView.setFitHeight(120);
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
                                Image image = new Image(file.toURI().toString(), 80, 120, true, true);
                                imageView.setImage(image);
                                setGraphic(imageView);
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
            });
        }

        if (colBarcode != null) colBarcode.setCellValueFactory(new PropertyValueFactory<>("barcode"));
        if (colTitle != null) colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        if (colDueDate != null) {
            colDueDate.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
            colDueDate.setCellFactory(column -> new TableCell<ReturnItemDTO, LocalDate>() {
                @Override
                protected void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(dateFormatter.format(item));
                    }
                }
            });
        }
        
        if (tblBorrowedBooks != null) {
            tblBorrowedBooks.setItems(borrowedBooksList);
            tblBorrowedBooks.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    currentItem = newSelection;
                    populateBookDetails();
                } else {
                    resetBookDetails();
                }
            });
        }

        if (btnPayFine != null) btnPayFine.setOnAction(e -> handlePayFine());
        if (btnConfirmCheckin != null) btnConfirmCheckin.setOnAction(e -> handleConfirmCheckin());
    }

    private void handleSearchMember() {
        String memberCode = txtMemberCode.getText();
        if (memberCode == null || memberCode.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập mã độc giả.");
            return;
        }

        Task<List<ReturnItemDTO>> searchTask = new Task<>() {
            @Override
            protected List<ReturnItemDTO> call() throws Exception {
                return borrowService.findActiveBorrowItemsByMemberCode(memberCode);
            }

            @Override
            protected void succeeded() {
                List<ReturnItemDTO> results = getValue();
                borrowedBooksList.setAll(results);
                if (lblBorrowedCount != null) {
                    lblBorrowedCount.setText("Currently borrowing: " + results.size() + " books");
                }
                resetBookDetails();
            }

            @Override
            protected void failed() {
                resetForm();
                Throwable ex = getException();
                showAlert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
            }
        };

        new Thread(searchTask).start();
    }

    private void populateBookDetails() {
        lblBookTitle.setText(currentItem.getTitle());
        lblBorrowerName.setText(currentItem.getBorrowerName());
        lblDueDate.setText(currentItem.getDueDate().format(dateFormatter));

        long overdueDays = ChronoUnit.DAYS.between(currentItem.getDueDate(), LocalDate.now());
        if (overdueDays > 0) {
            lblOverdueStatus.setText("Quá hạn " + overdueDays + " ngày");
            lblOverdueStatus.setStyle("-fx-text-fill: -color-danger-emphasis; -fx-font-weight: bold;");
        } else {
            lblOverdueStatus.setText("Trong hạn");
            lblOverdueStatus.setStyle("-fx-text-fill: -color-success-emphasis; -fx-font-weight: bold;");
        }

        boxBookDetails.setDisable(false);
        boxFines.setDisable(false);

        cmbCondition.getSelectionModel().select("GOOD"); // Triggers calculation
    }

    private void handleConditionChange(String condition) {
        if (condition == null) return;

        if ("DAMAGED".equals(condition) || "LOST".equals(condition)) {
            boxDamageNotes.setVisible(true);
            boxDamageNotes.setManaged(true);
        } else {
            boxDamageNotes.setVisible(false);
            boxDamageNotes.setManaged(false);
            txtNotes.clear();
        }

        recalculateFines(condition);
    }

    private void recalculateFines(String condition) {
        if (currentItem == null) return;

        BorrowService.FineCalculationResult result = borrowService.calculateFines(currentItem, condition);
        currentFineAmount = result.amount;
        isFinePaid = false;

        lblFineAmount.setText(currencyFormat.format(currentFineAmount));
        lblFineReason.setText(result.reason);

        if (currentFineAmount.compareTo(BigDecimal.ZERO) > 0) {
            btnPayFine.setDisable(false);
            btnPayFine.setText("Thu tiền phạt trực tiếp");
        } else {
            btnPayFine.setDisable(true);
            btnPayFine.setText("Không có phạt");
        }
    }

    private void handlePayFine() {
        isFinePaid = true;
        btnPayFine.setDisable(true);
        btnPayFine.setText("Đã thu tiền");
        lblFineAmount.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: -color-success-emphasis;");
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã ghi nhận thu tiền phạt.");
    }

    private void handleConfirmCheckin() {
        if (currentItem == null) return;

        String condition = cmbCondition.getValue();
        if (condition == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn tình trạng sách.");
            return;
        }

        String notes = txtNotes.getText();
        if (("DAMAGED".equals(condition) || "LOST".equals(condition)) && (notes == null || notes.trim().isEmpty())) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập ghi chú tình trạng hư hại/mất mát.");
            txtNotes.requestFocus();
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng đăng nhập lại.");
            return;
        }

        btnConfirmCheckin.setDisable(true);

        Task<Void> checkinTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                borrowService.checkinBook(currentItem, currentUser.getUserId(), condition, notes, currentFineAmount, isFinePaid);
                return null;
            }

            @Override
            protected void succeeded() {
                btnConfirmCheckin.setDisable(false);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xác nhận trả sách thành công!");
                handleSearchMember(); // Refresh list
            }

            @Override
            protected void failed() {
                btnConfirmCheckin.setDisable(false);
                Throwable ex = getException();
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", ex.getMessage());
            }
        };
        new Thread(checkinTask).start();
    }

    private void resetForm() {
        borrowedBooksList.clear();
        if (lblBorrowedCount != null) lblBorrowedCount.setText("Currently borrowing: 0 books");
        resetBookDetails();
    }

    private void resetBookDetails() {
        currentItem = null;
        currentFineAmount = BigDecimal.ZERO;
        isFinePaid = false;

        if (lblBookTitle != null) lblBookTitle.setText("-");
        if (lblBorrowerName != null) lblBorrowerName.setText("-");
        if (lblDueDate != null) lblDueDate.setText("-");
        if (lblOverdueStatus != null) {
            lblOverdueStatus.setText("-");
            lblOverdueStatus.setStyle("");
        }

        if (cmbCondition != null) cmbCondition.getSelectionModel().clearSelection();
        if (boxDamageNotes != null) {
            boxDamageNotes.setVisible(false);
            boxDamageNotes.setManaged(false);
        }
        if (txtNotes != null) txtNotes.clear();

        if (lblFineAmount != null) {
            lblFineAmount.setText("0 VND");
            lblFineAmount.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: -color-danger-emphasis;");
        }
        if (lblFineReason != null) lblFineReason.setText("-");
        if (btnPayFine != null) {
            btnPayFine.setDisable(true);
            btnPayFine.setText("Thu tiền phạt trực tiếp");
        }

        if (boxBookDetails != null) boxBookDetails.setDisable(true);
        if (boxFines != null) boxFines.setDisable(true);
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
