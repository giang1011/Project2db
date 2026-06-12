package com.library.controller;

import com.library.model.Member;
import com.library.model.MemberStudentProfile;
import com.library.service.MemberService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AddMemberController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(AddMemberController.class);

    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private ComboBox<String> cbMemberType;
    @FXML private DatePicker dpDateOfBirth;
    @FXML private TextArea txtAddress;
    @FXML private DatePicker dpMembershipStartDate;
    @FXML private DatePicker dpMembershipEndDate;

    // Student fields
    @FXML private VBox vboxStudentInfo;
    @FXML private TextField txtSchoolName;
    @FXML private TextField txtStudentCode;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final MemberService memberService;

    public AddMemberController() {
        // Use clean constructor injection or manual creation
        this.memberService = new MemberService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize ComboBox values
        cbMemberType.setItems(FXCollections.observableArrayList("NORMAL", "STUDENT"));

        // Default Start Date is current date
        dpMembershipStartDate.setValue(LocalDate.now());
        
        // Listen for Member Type change to toggle Student Info
        cbMemberType.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isStudent = "STUDENT".equals(newVal);
            vboxStudentInfo.setVisible(isStudent);
            vboxStudentInfo.setManaged(isStudent);
        });

        // Student area is hidden by default
        vboxStudentInfo.setVisible(false);
        vboxStudentInfo.setManaged(false);
    }

    @FXML
    void handleSave(ActionEvent event) {
        // Required validation
        if (txtFullName.getText().isEmpty() || 
            cbMemberType.getValue() == null || dpMembershipStartDate.getValue() == null || 
            dpMembershipEndDate.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Canh bao", "Vui long nhap day du cac truong bat buoc!");
            return;
        }

        if (dpMembershipEndDate.getValue().isBefore(dpMembershipStartDate.getValue()) || 
            dpMembershipEndDate.getValue().isEqual(dpMembershipStartDate.getValue())) {
            showAlert(Alert.AlertType.WARNING, "Canh bao", "Ngay ket thuc phai lon hon ngay bat dau!");
            return;
        }

        // Create Member object
        Member member = new Member();
        member.setFullName(txtFullName.getText().trim());
        member.setEmail(txtEmail.getText().trim());
        member.setPhone(txtPhone.getText().trim());
        member.setMemberType(cbMemberType.getValue());
        member.setDateOfBirth(dpDateOfBirth.getValue());
        member.setAddress(txtAddress.getText().trim());
        member.setMembershipStartDate(dpMembershipStartDate.getValue());
        member.setMembershipEndDate(dpMembershipEndDate.getValue());
        member.setStatus("ACTIVE");
        
        // Mock creator info (can be retrieved from session)
        member.setCreatedBy(1L); 
        member.setUpdatedBy(1L);

        // Create StudentProfile object if STUDENT
        MemberStudentProfile profile = null;
        if ("STUDENT".equals(member.getMemberType())) {
            if (txtSchoolName.getText().isEmpty() || txtStudentCode.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Canh bao", "Vui long nhap Ten truong va Ma sinh vien!");
                return;
            }
            profile = new MemberStudentProfile();
            profile.setSchoolName(txtSchoolName.getText().trim());
            profile.setStudentCode(txtStudentCode.getText().trim());
            profile.setStudentStatus("ACTIVE");
            profile.setStudentVerificationStatus("PENDING");
        }

        // Execute save on Background Thread
        disableUI(true);
        
        final MemberStudentProfile finalProfile = profile;
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                memberService.addMember(member, finalProfile);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            disableUI(false);
            showAlert(Alert.AlertType.INFORMATION, "Thanh cong", "Da them thanh vien thanh cong!");
            closeWindow();
        });

        saveTask.setOnFailed(e -> {
            disableUI(false);
            logger.error("Loi luu thanh vien", saveTask.getException());
            showAlert(Alert.AlertType.ERROR, "Loi", "Co loi xay ra khi luu: " + saveTask.getException().getMessage());
        });

        // Run Task on Virtual Thread (Powerful new feature of Java 21)
        Thread.ofVirtual().name("SaveMemberVirtualThread").start(saveTask);
    }

    @FXML
    void handleCancel(ActionEvent event) {
        closeWindow();
    }

    private void disableUI(boolean disable) {
        btnSave.setDisable(disable);
        btnCancel.setDisable(disable);
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

    private void closeWindow() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}

