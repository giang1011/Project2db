package com.library.controller;

import com.library.model.FineDTO;
import com.library.service.FineService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FinesManagementController {

    @FXML private TableView<FineDTO> tableFines;
    @FXML private TableColumn<FineDTO, Long> colFineId;
    @FXML private TableColumn<FineDTO, String> colMemberCode;
    @FXML private TableColumn<FineDTO, String> colMemberName;
    @FXML private TableColumn<FineDTO, String> colBookTitle;
    @FXML private TableColumn<FineDTO, String> colFineType;
    @FXML private TableColumn<FineDTO, BigDecimal> colAmount;
    @FXML private TableColumn<FineDTO, BigDecimal> colPaidAmount;
    @FXML private TableColumn<FineDTO, String> colStatus;
    @FXML private TableColumn<FineDTO, LocalDateTime> colIssuedAt;
    @FXML private TableColumn<FineDTO, Void> colActions;
    @FXML private TextField txtSearch;
    @FXML private Label lblTotalCollected;
    @FXML private Label lblUnpaidMembers;

    private final FineService fineService;
    private final ObservableList<FineDTO> masterData = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.of("vi", "VN"));
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public FinesManagementController() {
        this.fineService = new FineService();
    }

    @FXML
    public void initialize() {
        setupTable();
        loadFines();
        
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filterData(newValue);
        });
    }

    private void setupTable() {
        colFineId.setCellValueFactory(new PropertyValueFactory<>("fineId"));
        colMemberCode.setCellValueFactory(new PropertyValueFactory<>("memberCode"));
        colMemberName.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        
        colBookTitle.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        colBookTitle.setCellFactory(column -> new TableCell<FineDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else if (item == null || item.isEmpty()) {
                    setText("-");
                } else {
                    setText(item);
                }
            }
        });

        colFineType.setCellValueFactory(new PropertyValueFactory<>("fineType"));
        colFineType.setCellFactory(column -> new TableCell<FineDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    switch (item) {
                        case "OVERDUE": setText("Quá hạn"); break;
                        case "DAMAGED": setText("Hư hỏng"); break;
                        case "LOST": setText("Mất sách"); break;
                        default: setText(item);
                    }
                }
            }
        });

        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colAmount.setCellFactory(column -> new TableCell<FineDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(item));
                }
            }
        });

        colPaidAmount.setCellValueFactory(new PropertyValueFactory<>("paidAmount"));
        colPaidAmount.setCellFactory(column -> new TableCell<FineDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormat.format(item));
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<FineDTO, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label();
                    statusLabel.setStyle("-fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 12;");
                    switch (item) {
                        case "PAID":
                            statusLabel.setText("Đã thu");
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;");
                            break;
                        case "PARTIAL":
                            statusLabel.setText("Đã thu một phần");
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;");
                            break;
                        case "UNPAID":
                        default:
                            statusLabel.setText("Chưa thu");
                            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;");
                            break;
                    }
                    setGraphic(statusLabel);
                }
            }
        });

        colIssuedAt.setCellValueFactory(new PropertyValueFactory<>("issuedAt"));
        colIssuedAt.setCellFactory(column -> new TableCell<FineDTO, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateFormatter.format(item));
                }
            }
        });

        colActions.setCellFactory(column -> new TableCell<FineDTO, Void>() {
            private final Button btnPay = new Button("Thu tiền");
            private final HBox pane = new HBox(5, btnPay);

            {
                btnPay.getStyleClass().addAll("button-outlined", "success");
                btnPay.setOnAction(e -> {
                    FineDTO fine = getTableView().getItems().get(getIndex());
                    if ("PAID".equals(fine.getStatus())) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Thông báo");
                        alert.setHeaderText(null);
                        alert.setContentText("Khoản phạt này đã được thanh toán đầy đủ.");
                        alert.showAndWait();
                        return;
                    }
                    handlePayFine(fine);
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
        });
    }

    private void handlePayFine(FineDTO fine) {
        BigDecimal remainingAmount = fine.getAmount().subtract(fine.getPaidAmount());
        TextInputDialog dialog = new TextInputDialog(remainingAmount.toString());
        dialog.setTitle("Thu tiền phạt");
        dialog.setHeaderText("Thu tiền cho độc giả: " + fine.getMemberName());
        dialog.setContentText("Nhập số tiền thu:");

        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                BigDecimal amountToPay = new BigDecimal(amountStr.trim());
                if (amountToPay.compareTo(BigDecimal.ZERO) <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Số tiền phải lớn hơn 0");
                    return;
                }
                if (amountToPay.compareTo(remainingAmount) > 0) {
                    showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Số tiền không được lớn hơn số nợ còn lại.");
                    return;
                }
                
                Task<Void> payTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        fineService.payFine(fine.getFineId(), amountToPay);
                        return null;
                    }

                    @Override
                    protected void succeeded() {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thu tiền thành công.");
                        loadFines();
                    }

                    @Override
                    protected void failed() {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", getException().getMessage());
                    }
                };
                new Thread(payTask).start();
                
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ.");
            }
        });
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadFines();
    }

    private void loadFines() {
        Task<List<FineDTO>> loadTask = new Task<>() {
            @Override
            protected List<FineDTO> call() throws Exception {
                return fineService.getAllFines();
            }

            @Override
            protected void succeeded() {
                masterData.setAll(getValue());
                filterData(txtSearch.getText());
                updateSummary();
            }

            @Override
            protected void failed() {
                showAlert(Alert.AlertType.ERROR, "Lỗi", getException().getMessage());
            }
        };

        new Thread(loadTask).start();
    }

    private void updateSummary() {
        BigDecimal totalCollected = BigDecimal.ZERO;
        Set<Long> unpaidMembers = new HashSet<>();
        
        for (FineDTO fine : masterData) {
            if (fine.getPaidAmount() != null) {
                totalCollected = totalCollected.add(fine.getPaidAmount());
            }
            if ("UNPAID".equals(fine.getStatus()) || "PARTIAL".equals(fine.getStatus())) {
                unpaidMembers.add(fine.getMemberId());
            }
        }
        
        if (lblTotalCollected != null) {
            lblTotalCollected.setText(currencyFormat.format(totalCollected));
        }
        if (lblUnpaidMembers != null) {
            lblUnpaidMembers.setText(String.valueOf(unpaidMembers.size()));
        }
    }

    private void filterData(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            tableFines.setItems(masterData);
            return;
        }
        
        String lowerCaseFilter = keyword.toLowerCase();
        ObservableList<FineDTO> filteredData = FXCollections.observableArrayList();
        
        for (FineDTO fine : masterData) {
            if ((fine.getMemberName() != null && fine.getMemberName().toLowerCase().contains(lowerCaseFilter)) ||
                (fine.getMemberCode() != null && fine.getMemberCode().toLowerCase().contains(lowerCaseFilter)) ||
                (fine.getBookTitle() != null && fine.getBookTitle().toLowerCase().contains(lowerCaseFilter))) {
                filteredData.add(fine);
            }
        }
        tableFines.setItems(filteredData);
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
