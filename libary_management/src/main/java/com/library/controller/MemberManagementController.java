package com.library.controller;

import com.library.model.Member;
import com.library.service.MemberService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.fxml.FXMLLoader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MemberManagementController {

    @FXML
    private TableView<Member> tableMembers;
    @FXML
    private TableColumn<Member, String> colMemberCode;
    @FXML
    private TableColumn<Member, String> colFullName;
    @FXML
    private TableColumn<Member, String> colMemberType;
    @FXML
    private TableColumn<Member, String> colStatus;
    @FXML
    private TableColumn<Member, String> colStartDate;
    @FXML
    private TableColumn<Member, String> colEndDate;
    @FXML
    private TableColumn<Member, Void> colActions;

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbMemberType;
    @FXML
    private ComboBox<String> cmbStatus;
    @FXML
    private Button btnFilter;
    @FXML
    private Button btnViewPending;

    @FXML
    private Label lblTotalMembers;
    @FXML
    private Label lblExpiredMembers;
    @FXML
    private Label lblPendingProfiles;

    private final MemberService memberService;
    private ObservableList<Member> memberList;
    private FilteredList<Member> filteredList;

    public MemberManagementController() {
        this.memberService = new MemberService();
    }

    @FXML
    public void initialize() {
        colMemberCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMemberCode()));
        colFullName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getFullName()));
        colMemberType.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMemberType()));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colStartDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getMembershipStartDate() != null) {
                return new SimpleStringProperty(cellData.getValue().getMembershipStartDate().format(formatter));
            }
            return new SimpleStringProperty("");
        });

        colEndDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getMembershipEndDate() != null) {
                return new SimpleStringProperty(cellData.getValue().getMembershipEndDate().format(formatter));
            }
            return new SimpleStringProperty("");
        });

        colEndDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getMembershipEndDate() != null) {
                return new SimpleStringProperty(cellData.getValue().getMembershipEndDate().format(formatter));
            }
            return new SimpleStringProperty("");
        });

        setupStatusColumn();
        setupEndDateColumn();
        setupActionColumn();
        setupFilters();
        loadData();
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        loadData();
    }

    private void setupStatusColumn() {
        colStatus.setCellFactory(param -> new TableCell<>() {
            private final Label badge = new Label();
            private final HBox container = new HBox(badge);
            {
                container.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    badge.setText(item);
                    badge.getStyleClass().removeAll("badge-active", "badge-expired", "badge-suspended");
                    badge.getStyleClass().add("badge");

                    if ("ACTIVE".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-active");
                    } else if ("EXPIRED".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-expired");
                    } else if ("SUSPENDED".equalsIgnoreCase(item)) {
                        badge.getStyleClass().add("badge-suspended");
                    }
                    setGraphic(container);
                }
            }
        });
    }

    private void setupEndDateColumn() {
        colEndDate.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Member member = getTableView().getItems().get(getIndex());
                    if (member != null && member.getMembershipEndDate() != null) {
                        LocalDate now = LocalDate.now();
                        LocalDate endDate = member.getMembershipEndDate();
                        if ("SUSPENDED".equals(member.getStatus())) {
                            setStyle("-fx-text-fill: #9e9e9e;");
                        } else if (endDate.isBefore(now) || "EXPIRED".equals(member.getStatus())) {
                            setStyle("-fx-text-fill: #e53935; -fx-font-weight: bold;");
                        } else if (endDate.isBefore(now.plusDays(4))) {
                            setStyle("-fx-text-fill: #f57f17; -fx-font-weight: bold;");
                        } else {
                            setStyle("");
                        }
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void setupActionColumn() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final Button btnRenew = new Button();
            private final Button btnConfirm = new Button();
            private final HBox pane = new HBox(8, btnEdit, btnDelete, btnRenew, btnConfirm);

            {
                btnEdit.getStyleClass().addAll("btn-action", "btn-edit");
                btnDelete.getStyleClass().addAll("btn-action", "btn-delete");
                btnRenew.getStyleClass().addAll("btn-action", "btn-renew");
                btnConfirm.getStyleClass().addAll("btn-action", "btn-confirm");

                btnEdit.setText("Sửa");
                btnEdit.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0284c7; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #bae6fd; -fx-border-radius: 6;");
                btnEdit.setTooltip(new Tooltip("Sửa thông tin"));

                btnDelete.setText("Khóa");
                btnDelete.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #fca5a5; -fx-border-radius: 6;");
                btnDelete.setTooltip(new Tooltip("Khóa/Đình chỉ"));

                btnRenew.setText("Gia hạn");
                btnRenew.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #86efac; -fx-border-radius: 6;");
                btnRenew.setTooltip(new Tooltip("Gia hạn thẻ"));

                btnConfirm.setText("Xác nhận");
                btnConfirm.setStyle("-fx-background-color: #ffedd5; -fx-text-fill: #d97706; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand; -fx-border-color: #fdba74; -fx-border-radius: 6;");
                btnConfirm.setTooltip(new Tooltip("Xác nhận lên NORMAL"));

                pane.setAlignment(javafx.geometry.Pos.CENTER);

                btnEdit.setOnAction(event -> {
                    Member member = getTableView().getItems().get(getIndex());
                    openEditDialog(member);
                });

                btnDelete.setOnAction(event -> {
                    Member member = getTableView().getItems().get(getIndex());
                    suspendMember(member);
                });

                btnRenew.setOnAction(event -> {
                    Member member = getTableView().getItems().get(getIndex());
                    renewMember(member);
                });

                btnConfirm.setOnAction(event -> {
                    Member member = getTableView().getItems().get(getIndex());
                    confirmStudent(member);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Member member = getTableView().getItems().get(getIndex());
                    if ("STUDENT".equals(member.getMemberType()) && "EXPIRED".equals(member.getStatus())) {
                        btnConfirm.setVisible(true);
                        btnConfirm.setManaged(true);
                    } else {
                        btnConfirm.setVisible(false);
                        btnConfirm.setManaged(false);
                    }
                    setGraphic(pane);
                }
            }
        });
    }

    private void openEditDialog(Member member) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/librarian/EditMemberDialog.fxml"));
            Parent root = loader.load();

            EditMemberDialogController controller = loader.getController();
            controller.setMember(member);

            Stage stage = new Stage();
            stage.setTitle("Sửa Thông Tin Độc Giả");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Reload after editing
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở form sửa: " + e.getMessage());
        }
    }

    private void suspendMember(Member member) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận khóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc chắn muốn khóa/đình chỉ độc giả " + member.getMemberCode() + " không?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    memberService.suspendMember(member.getMemberId());
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã khóa độc giả thành công.");
                loadData();
            });
            task.setOnFailed(e -> {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi khi khóa độc giả: " + task.getException().getMessage());
            });
            new Thread(task).start();
        }
    }

    private void confirmStudent(Member member) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xac nhan chuyen doi");
        confirm.setHeaderText(null);
        confirm.setContentText("Ban muon xac nhan chuyen the sinh vien " + member.getMemberCode()
                + " thanh the NORMAL? (Phi thu 100,000 VND)");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            com.library.model.User loggedInUser = com.library.util.UserSession.getInstance().getLoggedInUser();
            final Long processedBy = (loggedInUser != null) ? loggedInUser.getUserId() : null;

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    memberService.confirmStudentToNormal(member.getMemberId(), processedBy);
                    return null;
                }
            };
            task.setOnSucceeded(e -> {
                showAlert(Alert.AlertType.INFORMATION, "Thanh cong", "Da cap nhat va thu phi thanh cong.");
                loadData();
            });
            task.setOnFailed(e -> {
                showAlert(Alert.AlertType.ERROR, "Loi", "Loi khi cap nhat: " + task.getException().getMessage());
            });
            new Thread(task).start();
        }
    }

    private void renewMember(Member member) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/librarian/RenewMemberDialog.fxml"));
            Parent root = loader.load();

            RenewMemberDialogController controller = loader.getController();
            controller.setMember(member);

            Stage stage = new Stage();
            stage.setTitle("Gia Hạn Thẻ Thành Viên");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Reload member list after renewal
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở form gia hạn: " + e.getMessage());
        }
    }

    private void loadData() {
        Task<List<Member>> loadTask = new Task<>() {
            @Override
            protected List<Member> call() throws Exception {
                return memberService.getAllMembers();
            }
        };

        loadTask.setOnSucceeded(e -> {
            memberList = FXCollections.observableArrayList(loadTask.getValue());
            filteredList = new FilteredList<>(memberList);
            tableMembers.setItems(filteredList);
            updateDashboardSummaries();
            applyFilter();
        });

        loadTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.ERROR, "Lỗi",
                        "Không thể tải danh sách độc giả: " + loadTask.getException().getMessage());
            });
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateDashboardSummaries() {
        if (memberList == null)
            return;

        long total = memberList.size();
        long expired = memberList.stream()
                .filter(m -> "EXPIRED".equals(m.getStatus()) ||
                        (m.getMembershipEndDate() != null && m.getMembershipEndDate().isBefore(LocalDate.now())))
                .count();
        long pending = memberList.stream().filter(m -> "SUSPENDED".equals(m.getStatus())).count();

        Platform.runLater(() -> {
            if (lblTotalMembers != null)
                lblTotalMembers.setText(String.valueOf(total));
            if (lblExpiredMembers != null)
                lblExpiredMembers.setText(String.valueOf(expired));
            if (lblPendingProfiles != null)
                lblPendingProfiles.setText(String.valueOf(pending));
        });
    }

    private void setupFilters() {
        cmbMemberType.setItems(FXCollections.observableArrayList("Tất cả", "STUDENT", "NORMAL"));
        cmbMemberType.setValue("Tất cả");

        cmbStatus.setItems(FXCollections.observableArrayList("Tất cả", "ACTIVE", "EXPIRED", "SUSPENDED"));
        cmbStatus.setValue("Tất cả");

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        cmbMemberType.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        cmbStatus.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        btnFilter.setOnAction(e -> applyFilter());
    }

    private void applyFilter() {
        if (filteredList == null)
            return;

        String searchText = txtSearch.getText() != null ? txtSearch.getText().toLowerCase().trim() : "";
        String selectedType = cmbMemberType.getValue();
        String selectedStatus = cmbStatus.getValue();

        filteredList.setPredicate(member -> {
            // 1. Filter by search keyword (Member code or full name)
            if (!searchText.isEmpty()) {
                boolean matchCode = member.getMemberCode() != null
                        && member.getMemberCode().toLowerCase().contains(searchText);
                boolean matchName = member.getFullName() != null
                        && member.getFullName().toLowerCase().contains(searchText);
                if (!matchCode && !matchName) {
                    return false;
                }
            }

            // 2. Filter by member type
            if (selectedType != null && !selectedType.equals("Tất cả")) {
                if (member.getMemberType() == null || !member.getMemberType().equalsIgnoreCase(selectedType)) {
                    return false;
                }
            }

            // 3. Filter by card status
            if (selectedStatus != null && !selectedStatus.equals("Tất cả")) {
                if (member.getStatus() == null || !member.getStatus().equalsIgnoreCase(selectedStatus)) {
                    return false;
                }
            }

            return true;
        });
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

