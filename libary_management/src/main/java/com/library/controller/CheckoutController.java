package com.library.controller;

import com.library.model.BorrowItemDTO;
import com.library.model.Member;
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
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class CheckoutController {
    @FXML
    private TextField txtMemberCode;
    @FXML
    private Button btnSearchMember;
    @FXML
    private Label lblAvatar;
    @FXML
    private Label lblMemberName;
    @FXML
    private Label lblMemberType;
    @FXML
    private Label lblMemberStatus;
    @FXML
    private Label lblMaxBooks;
    @FXML
    private Label lblMemberWarning;

    @FXML
    private VBox boxBookScanning;
    @FXML
    private TextField txtBookBarcode;
    @FXML
    private ListView<BorrowItemDTO> listBorrowBooks;

    @FXML
    private DatePicker dpBorrowDate;
    @FXML
    private DatePicker dpDueDate;
    @FXML
    private TextArea txtNotes;

    @FXML
    private Button btnCancel;
    @FXML
    private Button btnConfirmBorrow;

    private BorrowService borrowService;
    private Member currentMember;
    private int remainingLimit = 0;
    private ObservableList<BorrowItemDTO> borrowItems = FXCollections.observableArrayList();

    public CheckoutController() {
        borrowService = new BorrowService();
    }

    @FXML
    public void initialize() {
        dpBorrowDate.setValue(LocalDate.now());
        dpBorrowDate.setDisable(true); // Usually borrow date is today and fixed

        listBorrowBooks.setItems(borrowItems);
        listBorrowBooks.setCellFactory(param -> new javafx.scene.control.ListCell<BorrowItemDTO>() {
            @Override
            protected void updateItem(BorrowItemDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(15);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    // Bỏ background-color cứng để ListCell có thể hiển thị màu khi được chọn
                    // (selected)
                    hbox.setStyle(
                            "-fx-padding: 10; -fx-background-radius: 5; -fx-border-color: -color-border-default; -fx-border-radius: 5;");

                    // Image
                    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
                    imageView.setFitWidth(40);
                    imageView.setFitHeight(55);
                    if (item.getCoverImage() != null && !item.getCoverImage().isEmpty()) {
                        try {
                            javafx.scene.image.Image img = new javafx.scene.image.Image("file:" + item.getCoverImage());
                            imageView.setImage(img);
                        } catch (Exception e) {
                            // ignore
                        }
                    } else {
                        // placeholder style or image
                        javafx.scene.layout.Region placeholder = new javafx.scene.layout.Region();
                        placeholder.setPrefSize(40, 55);
                        placeholder.setStyle("-fx-background-color: -color-bg-subtle;");
                        hbox.getChildren().add(placeholder);
                    }
                    if (imageView.getImage() != null) {
                        hbox.getChildren().add(imageView);
                    }

                    // Details
                    javafx.scene.layout.VBox detailsBox = new javafx.scene.layout.VBox(3);
                    detailsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    String statusStr = item.getStatus() != null ? " (" + item.getStatus() + ")" : " (Available)";
                    javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(
                            item.getBarcode() + " - " + item.getTitle() + statusStr);
                    titleLabel
                            .setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -color-fg-default;");

                    String shelfStr = item.getShelfLocation() != null ? item.getShelfLocation() : "N/A";
                    javafx.scene.control.Label shelfLabel = new javafx.scene.control.Label("Shelf: " + shelfStr);
                    shelfLabel.setStyle("-fx-text-fill: -color-fg-muted; -fx-font-size: 12px;");

                    detailsBox.getChildren().addAll(titleLabel, shelfLabel);

                    // Spacer
                    javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                    javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    // Remove Button
                    javafx.scene.control.Button btnRemove = new javafx.scene.control.Button("✕");
                    btnRemove.setStyle(
                            "-fx-background-color: transparent; -fx-text-fill: -color-fg-muted; -fx-cursor: hand;");
                    btnRemove.setOnAction(e -> borrowItems.remove(item));

                    hbox.getChildren().addAll(detailsBox, spacer, btnRemove);

                    setGraphic(hbox);
                    setStyle("-fx-background-color: transparent; -fx-padding: 5 0 5 0;");
                }
            }
        });

        // Khi chọn một sách trong danh sách, hiển thị thông tin Ngày Trả của sách đó
        listBorrowBooks.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                dpDueDate.setValue(newVal.getDueDate());
            }
        });

        // Khi người dùng đổi Ngày Trả, cập nhật lại cho sách đang được chọn
        dpDueDate.valueProperty().addListener((obs, oldVal, newVal) -> {
            BorrowItemDTO selected = listBorrowBooks.getSelectionModel().getSelectedItem();
            if (selected != null && newVal != null) {
                selected.setDueDate(newVal);
            }
        });

        if (btnSearchMember != null)
            btnSearchMember.setOnAction(e -> handleSearchMember());
        if (txtMemberCode != null)
            txtMemberCode.setOnAction(e -> handleSearchMember()); // Enter key

        if (txtBookBarcode != null)
            txtBookBarcode.setOnAction(e -> handleScanBook());

        if (btnCancel != null)
            btnCancel.setOnAction(e -> resetForm());
        if (btnConfirmBorrow != null)
            btnConfirmBorrow.setOnAction(e -> handleConfirmBorrow());
    }

    private void handleSearchMember() {
        String memberCode = txtMemberCode.getText();
        if (memberCode == null || memberCode.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập mã độc giả.");
            return;
        }

        Task<MemberInfo> searchTask = new Task<>() {
            @Override
            protected MemberInfo call() throws Exception {
                Member member = borrowService.validateAndGetMember(memberCode);
                int limit = borrowService.getAvailableBorrowLimit(member);
                return new MemberInfo(member, limit);
            }

            @Override
            protected void succeeded() {
                MemberInfo info = getValue();
                currentMember = info.member;
                remainingLimit = info.limit;

                lblMemberName.setText(currentMember.getFullName() + " (" + currentMember.getMemberCode() + ")");
                lblMemberType.setText(currentMember.getMemberType());
                lblMemberStatus.setText(currentMember.getStatus());
                lblMaxBooks.setText(String.valueOf(remainingLimit));

                if (lblAvatar != null) {
                    lblAvatar.setVisible(true);
                    lblAvatar.setManaged(true);
                }

                lblMemberWarning.setVisible(false);
                lblMemberWarning.setManaged(false);

                if (remainingLimit <= 0) {
                    lblMemberWarning.setText(
                            "Độc giả đã đạt giới hạn mượn sách (" + currentMember.getMaxBorrowBooks() + " cuốn).");
                    lblMemberWarning.setVisible(true);
                    lblMemberWarning.setManaged(true);
                    boxBookScanning.setDisable(true);
                } else {
                    boxBookScanning.setDisable(false);
                    if (dpDueDate != null) {
                        dpDueDate.setValue(LocalDate.now().plusDays(currentMember.getBorrowDurationDays()));
                    }
                    borrowItems.clear(); // clear previous
                    if (txtBookBarcode != null)
                        txtBookBarcode.requestFocus();
                }
            }

            @Override
            protected void failed() {
                resetForm();
                Throwable ex = getException();
                lblMemberWarning.setText("Lỗi: " + ex.getMessage());
                lblMemberWarning.setVisible(true);
                lblMemberWarning.setManaged(true);
                boxBookScanning.setDisable(true);
            }
        };

        new Thread(searchTask).start();
    }

    private void handleScanBook() {
        if (currentMember == null)
            return;

        String barcode = txtBookBarcode.getText();
        if (barcode == null || barcode.trim().isEmpty())
            return;

        if (borrowItems.size() >= remainingLimit) {
            showAlert(Alert.AlertType.WARNING, "Giới hạn", "Không thể mượn thêm. Độc giả đã đạt số lượng tối đa.");
            txtBookBarcode.clear();
            return;
        }

        // Kiểm tra xem đã quét chưa
        for (BorrowItemDTO item : borrowItems) {
            if (item.getBarcode().equalsIgnoreCase(barcode.trim())) {
                showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Sách này đã có trong danh sách.");
                txtBookBarcode.clear();
                return;
            }
        }

        // Lấy và kiểm tra ngày trả
        LocalDate dueDate = dpDueDate.getValue();
        if (dueDate == null || dueDate.isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn ngày trả hợp lệ (từ hôm nay trở đi).");
            return;
        }
        int borrowDays = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);

        Task<BorrowItemDTO> scanTask = new Task<>() {
            @Override
            protected BorrowItemDTO call() throws Exception {
                return borrowService.validateAndGetBookCopy(barcode, borrowDays);
            }

            @Override
            protected void succeeded() {
                BorrowItemDTO dto = getValue();
                borrowItems.add(dto);
                if (txtBookBarcode != null) {
                    txtBookBarcode.clear();
                    txtBookBarcode.requestFocus();
                }
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                showAlert(Alert.AlertType.ERROR, "Lỗi quét sách", ex.getMessage());
                if (txtBookBarcode != null) {
                    txtBookBarcode.clear();
                    txtBookBarcode.requestFocus();
                }
            }
        };
        new Thread(scanTask).start();
    }

    private void handleConfirmBorrow() {
        if (currentMember == null || borrowItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn độc giả và quét sách cần mượn.");
            return;
        }

        User currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng đăng nhập lại.");
            return;
        }

        btnConfirmBorrow.setDisable(true);

        Task<Void> borrowTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                borrowService.checkoutBooks(currentMember.getMemberId(), currentUser.getUserId(), borrowItems);
                return null;
            }

            @Override
            protected void succeeded() {
                btnConfirmBorrow.setDisable(false);
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đã cho mượn " + borrowItems.size() + " cuốn sách.");
                resetForm();
            }

            @Override
            protected void failed() {
                btnConfirmBorrow.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", getException().getMessage());
            }
        };
        new Thread(borrowTask).start();
    }

    private void resetForm() {
        currentMember = null;
        remainingLimit = 0;
        if (txtMemberCode != null)
            txtMemberCode.clear();
        if (lblMemberName != null)
            lblMemberName.setText("-");
        if (lblMemberType != null)
            lblMemberType.setText("-");
        if (lblMemberStatus != null)
            lblMemberStatus.setText("-");
        if (lblMaxBooks != null)
            lblMaxBooks.setText("-");
        if (lblMemberWarning != null) {
            lblMemberWarning.setVisible(false);
            lblMemberWarning.setManaged(false);
        }
        if (lblAvatar != null) {
            lblAvatar.setVisible(false);
            lblAvatar.setManaged(false);
        }
        if (boxBookScanning != null)
            boxBookScanning.setDisable(true);
        borrowItems.clear();
        if (dpDueDate != null)
            dpDueDate.setValue(null);
        if (txtNotes != null)
            txtNotes.clear();
        if (txtBookBarcode != null)
            txtBookBarcode.clear();
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

    private static class MemberInfo {
        Member member;
        int limit;

        MemberInfo(Member member, int limit) {
            this.member = member;
            this.limit = limit;
        }
    }
}
