package com.library.controller;

import com.library.model.Member;
import com.library.service.MemberService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditMemberDialogController {

    @FXML
    private TextField txtMemberCode;
    @FXML
    private TextField txtFullName;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtPhone;
    @FXML
    private TextField txtAddress;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSave;

    private Member currentMember;
    private final MemberService memberService;

    public EditMemberDialogController() {
        this.memberService = new MemberService();
    }

    public void setMember(Member member) {
        this.currentMember = member;
        txtMemberCode.setText(member.getMemberCode());
        txtMemberCode.setDisable(true);
        txtFullName.setText(member.getFullName());
        txtEmail.setText(member.getEmail());
        txtPhone.setText(member.getPhone());
        txtAddress.setText(member.getAddress());
    }

    @FXML
    private void handleSave() {
        // Validate
        if (txtFullName.getText() == null || txtFullName.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Họ tên không được để trống!");
            return;
        }

        currentMember.setFullName(txtFullName.getText().trim());
        currentMember.setEmail(txtEmail.getText().trim());
        currentMember.setPhone(txtPhone.getText().trim());
        currentMember.setAddress(txtAddress.getText().trim());

        // Disable save button to prevent double submit
        btnSave.setDisable(true);

        Task<Boolean> updateTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                memberService.updateMember(currentMember);
                return true;
            }
        };

        updateTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin thành công.");
                closeDialog();
            });
        });

        updateTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                btnSave.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Lỗi",
                        "Không thể cập nhật: " + updateTask.getException().getMessage());
            });
        });

        new Thread(updateTask).start();
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
