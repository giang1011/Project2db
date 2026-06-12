package com.library.controller;

import com.library.model.Member;
import com.library.service.MemberService;
import com.library.util.UserSession;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class RenewMemberDialogController {

    @FXML private TextField txtMemberCode;
    @FXML private TextField txtFullName;
    @FXML private TextField txtStatus;
    @FXML private TextField txtOldEndDate;
    @FXML private TextField txtMonths;
    @FXML private TextField txtAmount;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

    private Member currentMember;
    private final MemberService memberService;
    private static final long NORMAL_RATE = 10000;

    public RenewMemberDialogController() {
        this.memberService = new MemberService();
    }

    public void setMember(Member member) {
        this.currentMember = member;
        txtMemberCode.setText(member.getMemberCode());
        txtFullName.setText(member.getFullName());
        txtStatus.setText(member.getStatus());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (member.getMembershipEndDate() != null) {
            txtOldEndDate.setText(member.getMembershipEndDate().format(formatter));
        } else {
            txtOldEndDate.setText("N/A");
        }

        // Add listener to month input to auto-calculate price
        txtMonths.textProperty().addListener((observable, oldValue, newValue) -> {
            calculateAmount(newValue);
        });
    }

    private void calculateAmount(String monthsStr) {
        if (monthsStr == null || monthsStr.trim().isEmpty()) {
            txtAmount.setText("0");
            return;
        }

        try {
            int months = Integer.parseInt(monthsStr.trim());
            if (months <= 0) {
                txtAmount.setText("0");
                return;
            }

            if ("STUDENT".equalsIgnoreCase(currentMember.getMemberType())) {
                txtAmount.setText("0");
            } else {
                long totalAmount = NORMAL_RATE * months;
                txtAmount.setText(String.valueOf(totalAmount));
            }
        } catch (NumberFormatException e) {
            txtAmount.setText("0");
        }
    }

    @FXML
    private void handleConfirm() {
        String monthsStr = txtMonths.getText();
        
        // 1. Validate number of months
        int months = 0;
        try {
            months = Integer.parseInt(monthsStr.trim());
            if (months <= 0) {
                showAlert(Alert.AlertType.WARNING, "Canh bao", "So thang nhap vao khong hop le");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Canh bao", "So thang nhap vao khong hop le");
            return;
        }

        // 2. Validate amount
        BigDecimal amount = BigDecimal.ZERO;
        try {
            amount = new BigDecimal(txtAmount.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                showAlert(Alert.AlertType.WARNING, "Canh bao", "So tien khong hop le");
                return;
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.WARNING, "Canh bao", "So tien khong hop le");
            return;
        }

        // 3. Check suspension status
        if ("SUSPENDED".equalsIgnoreCase(currentMember.getStatus())) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Xac nhan");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Thanh vien nay dang bi DINH CHI. Ban co chac chan muon tiep tuc gia han va mo khoa the khong?");
            
            ButtonType btnYes = new ButtonType("Co", ButtonBar.ButtonData.YES);
            ButtonType btnNo = new ButtonType("Khong", ButtonBar.ButtonData.NO);
            confirmAlert.getButtonTypes().setAll(btnYes, btnNo);

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == btnNo) {
                // Librarian cancels renewal process
                return;
            }
        }

        final int finalMonths = months;
        final BigDecimal finalAmount = amount;

        // Get current logged-in librarian info from UserSession
        com.library.model.User loggedInUser = UserSession.getInstance().getLoggedInUser();
        if (loggedInUser == null || loggedInUser.getUserId() == null) {
            showAlert(Alert.AlertType.ERROR, "Loi", "Khong tim thay thong tin thu thu dang nhap. Vui long dang nhap lai!");
            return;
        }
        final Long processedBy = loggedInUser.getUserId();

        // Disable confirm button to prevent double-click
        btnConfirm.setDisable(true);

        Task<Boolean> renewTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                memberService.renewMember(currentMember, finalMonths, finalAmount, processedBy);
                return true;
            }
        };

        renewTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Thanh cong", "Da gia han the thanh vien thanh cong.");
                closeDialog();
            });
        });

        renewTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                btnConfirm.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Loi", "Loi khi gia han: " + renewTask.getException().getMessage());
            });
        });

        new Thread(renewTask).start();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

