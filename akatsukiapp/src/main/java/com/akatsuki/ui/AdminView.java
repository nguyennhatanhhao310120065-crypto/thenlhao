package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Admin Panel — User management & resource management.
 * Only accessible to users with role = 0 (Admin).
 */
public class AdminView extends VBox {
    private final User currentUser;
    private VBox contentBox;
    private String activeTab = "users"; // "users" or "resources"

    public AdminView(User currentUser) {
        this.currentUser = currentUser;
        setSpacing(0);
        contentBox = new VBox();
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        getChildren().add(contentBox);
        buildUI();
    }

    private void buildUI() {
        contentBox.getChildren().clear();

        // Header
        VBox header = new VBox(8);
        header.setPadding(new Insets(0, 0, 24, 0));
        Label title = new Label("⚙ Admin Panel");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 700;");
        Label subtitle = new Label("Quản lý người dùng, phân quyền và tài nguyên hệ thống.");
        subtitle.setStyle("-fx-text-fill: #64748b;");
        header.getChildren().addAll(title, subtitle);

        // Stats cards
        int[] stats = DatabaseManager.getInstance().getSystemStats();
        HBox statsRow = new HBox(16);
        statsRow.setPadding(new Insets(0, 0, 24, 0));
        statsRow.getChildren().addAll(
                buildStatCard("👥", "Người dùng", String.valueOf(stats[0]), "#1e3fae"),
                buildStatCard("📚", "Bài test", String.valueOf(stats[1]), "#059669"),
                buildStatCard("❓", "Câu hỏi", String.valueOf(stats[2]), "#7c3aed"),
                buildStatCard("📊", "Lượt thi", String.valueOf(stats[3]), "#d97706")
        );

        // Tab buttons
        HBox tabs = new HBox(8);
        tabs.setPadding(new Insets(0, 0, 20, 0));

        Button usersTab = new Button("👥 Quản lý người dùng");
        usersTab.getStyleClass().add("filter-tab");
        if ("users".equals(activeTab)) usersTab.getStyleClass().add("active");
        usersTab.setOnAction(e -> { activeTab = "users"; buildUI(); });

        Button resourcesTab = new Button("📚 Quản lý tài nguyên");
        resourcesTab.getStyleClass().add("filter-tab");
        if ("resources".equals(activeTab)) resourcesTab.getStyleClass().add("active");
        resourcesTab.setOnAction(e -> { activeTab = "resources"; buildUI(); });

        tabs.getChildren().addAll(usersTab, resourcesTab);

        VBox body = new VBox();
        VBox.setVgrow(body, Priority.ALWAYS);

        if ("users".equals(activeTab)) {
            body.getChildren().add(buildUsersPanel());
        } else {
            body.getChildren().add(buildResourcesPanel());
        }

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        contentBox.getChildren().addAll(header, statsRow, tabs, scroll);
    }

    // ==================== STAT CARD ====================

    private VBox buildStatCard(String icon, String label, String value, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e2e8f0; -fx-border-radius: 16; -fx-border-width: 1;");
        card.setPrefWidth(200);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: " + color + ";");

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #64748b;");

        card.getChildren().addAll(iconLabel, valueLabel, nameLabel);
        return card;
    }

    // ==================== USERS PANEL ====================

    private VBox buildUsersPanel() {
        VBox panel = new VBox(12);

        List<User> users = DatabaseManager.getInstance().getAllUsers();

        Label panelTitle = new Label("👥 Danh sách người dùng (" + users.size() + ")");
        panelTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");
        panel.getChildren().add(panelTitle);

        // Table header
        HBox tableHeader = new HBox();
        tableHeader.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12 12 0 0; -fx-padding: 14 20;");
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.getChildren().addAll(
                makeHeaderCell("ID", 60),
                makeHeaderCell("Username", 180),
                makeHeaderCell("Vai trò", 160),
                makeHeaderCell("Ngày tạo", 160),
                makeHeaderCell("Hành động", 140)
        );
        panel.getChildren().add(tableHeader);

        // Table rows
        for (User u : users) {
            HBox row = new HBox();
            row.setStyle("-fx-background-color: white; -fx-border-color: #f1f5f9 transparent transparent transparent; -fx-border-width: 1 0 0 0; -fx-padding: 12 20;");
            row.setAlignment(Pos.CENTER_LEFT);

            // ID
            Label idLabel = new Label(String.valueOf(u.getId()));
            idLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #94a3b8;");
            idLabel.setPrefWidth(60);

            // Username
            Label nameLabel = new Label(u.getUsername());
            nameLabel.setStyle("-fx-font-weight: 600;");
            nameLabel.setPrefWidth(180);

            // Role with badge + ComboBox
            HBox roleBox = new HBox(8);
            roleBox.setAlignment(Pos.CENTER_LEFT);
            roleBox.setPrefWidth(160);

            Label roleBadge = new Label(u.getRoleLabel());
            String badgeColor = switch (u.getRole()) {
                case 0 -> "#ef4444"; // Admin = red
                case 1 -> "#1e3fae"; // Lecturer = blue
                default -> "#64748b"; // Student = gray
            };
            roleBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: white; -fx-padding: 3 10; " +
                    "-fx-background-color: " + badgeColor + "; -fx-background-radius: 6;");

            ComboBox<String> roleCombo = new ComboBox<>();
            roleCombo.getItems().addAll("Admin", "Lecturer", "Student");
            roleCombo.setValue(u.getRoleLabel());
            roleCombo.setStyle("-fx-font-size: 11px; -fx-pref-width: 90;");
            roleCombo.setOnAction(e -> {
                int newRole = switch (roleCombo.getValue()) {
                    case "Admin" -> 0;
                    case "Lecturer" -> 1;
                    default -> 2;
                };
                if (newRole != u.getRole()) {
                    if (u.getId() == currentUser.getId() && newRole != 0) {
                        showAlert("⚠ Bạn không thể tự hạ quyền Admin của chính mình!");
                        roleCombo.setValue(u.getRoleLabel());
                        return;
                    }
                    boolean ok = DatabaseManager.getInstance().updateUserRole(u.getId(), newRole);
                    if (ok) {
                        showAlert("✅ Đã đổi vai trò của " + u.getUsername() + " thành " + roleCombo.getValue());
                        buildUI();
                    } else {
                        showAlert("❌ Đổi vai trò thất bại.");
                        roleCombo.setValue(u.getRoleLabel());
                    }
                }
            });

            roleBox.getChildren().addAll(roleBadge, roleCombo);

            // Created at
            Label dateLabel = new Label(u.getCreatedAt() != null ? u.getCreatedAt().substring(0, Math.min(10, u.getCreatedAt().length())) : "—");
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
            dateLabel.setPrefWidth(160);

            // Actions
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.setPrefWidth(140);

            if (u.getId() != currentUser.getId()) {
                Button deleteBtn = new Button("🗑 Xóa");
                deleteBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #ef4444; -fx-padding: 4 12; " +
                        "-fx-background-color: rgba(239,68,68,0.08); -fx-background-radius: 8; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Xóa user \"" + u.getUsername() + "\" và TẤT CẢ dữ liệu liên quan?\n(Bài test, kết quả, câu hỏi đã lưu)",
                            ButtonType.YES, ButtonType.NO);
                    confirm.setHeaderText("⚠ Xác nhận xóa user");
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        boolean ok = DatabaseManager.getInstance().deleteUser(u.getId());
                        if (ok) {
                            showAlert("✅ Đã xóa user " + u.getUsername());
                            buildUI();
                        } else {
                            showAlert("❌ Xóa thất bại.");
                        }
                    }
                });
                actions.getChildren().add(deleteBtn);
            } else {
                Label meLabel = new Label("(Bạn)");
                meLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #059669;");
                actions.getChildren().add(meLabel);
            }

            row.getChildren().addAll(idLabel, nameLabel, roleBox, dateLabel, actions);
            panel.getChildren().add(row);
        }

        // Bottom border
        Region bottom = new Region();
        bottom.setStyle("-fx-background-color: #f1f5f9; -fx-pref-height: 2; -fx-background-radius: 0 0 12 12;");
        panel.getChildren().add(bottom);

        return panel;
    }

    // ==================== RESOURCES PANEL ====================

    private VBox buildResourcesPanel() {
        VBox panel = new VBox(12);

        List<QuestionBank> banks = DatabaseManager.getInstance().getAllBanksAdmin();

        Label panelTitle = new Label("📚 Tất cả bài test (" + banks.size() + ")");
        panelTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");
        panel.getChildren().add(panelTitle);

        if (banks.isEmpty()) {
            Label empty = new Label("Chưa có bài test nào trong hệ thống.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-padding: 40;");
            panel.getChildren().add(empty);
            return panel;
        }

        // Table header
        HBox tableHeader = new HBox();
        tableHeader.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12 12 0 0; -fx-padding: 14 20;");
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        tableHeader.getChildren().addAll(
                makeHeaderCell("ID", 50),
                makeHeaderCell("Tên bài test", 220),
                makeHeaderCell("Người tạo", 120),
                makeHeaderCell("Loại đề", 80),
                makeHeaderCell("Câu hỏi", 70),
                makeHeaderCell("Trạng thái", 90),
                makeHeaderCell("Ngày tạo", 110),
                makeHeaderCell("Hành động", 140)
        );
        panel.getChildren().add(tableHeader);

        for (QuestionBank b : banks) {
            HBox row = new HBox();
            row.setStyle("-fx-background-color: white; -fx-border-color: #f1f5f9 transparent transparent transparent; -fx-border-width: 1 0 0 0; -fx-padding: 12 20;");
            row.setAlignment(Pos.CENTER_LEFT);

            Label idLabel = new Label(String.valueOf(b.getId()));
            idLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #94a3b8;");
            idLabel.setPrefWidth(50);

            Label nameLabel = new Label(b.getBankName());
            nameLabel.setStyle("-fx-font-weight: 600;");
            nameLabel.setPrefWidth(220);
            nameLabel.setWrapText(false);

            Label creatorLabel = new Label(b.getCreatorName() != null ? b.getCreatorName() : "—");
            creatorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            creatorLabel.setPrefWidth(120);

            Label examLabel = new Label(b.getExamType());
            examLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #1e3fae; -fx-padding: 3 8; " +
                    "-fx-background-color: rgba(30,63,174,0.08); -fx-background-radius: 6;");
            HBox examBox = new HBox(examLabel);
            examBox.setPrefWidth(80);

            Label countLabel = new Label(String.valueOf(b.getQuestionCount()));
            countLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #7c3aed;");
            countLabel.setPrefWidth(70);

            // Visibility toggle
            Button visBtn = new Button(b.isPublic() ? "🌐 Public" : "🔒 Private");
            visBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-cursor: hand; " +
                    "-fx-background-radius: 6; -fx-text-fill: " + (b.isPublic() ? "#059669" : "#94a3b8") + "; " +
                    "-fx-background-color: " + (b.isPublic() ? "rgba(5,150,105,0.08)" : "#f1f5f9") + ";");
            visBtn.setPrefWidth(90);
            visBtn.setOnAction(e -> {
                DatabaseManager.getInstance().toggleVisibility(b.getId(), b.getCreatedBy(), !b.isPublic());
                buildUI();
            });

            Label dateLabel = new Label(b.getCreatedAt() != null ? b.getCreatedAt().substring(0, Math.min(10, b.getCreatedAt().length())) : "—");
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
            dateLabel.setPrefWidth(110);

            // Actions
            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.setPrefWidth(140);

            Button deleteBtn = new Button("🗑 Xóa");
            deleteBtn.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #ef4444; -fx-padding: 4 12; " +
                    "-fx-background-color: rgba(239,68,68,0.08); -fx-background-radius: 8; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Xóa bài test \"" + b.getBankName() + "\"?\n(Tất cả câu hỏi và kết quả thi liên quan sẽ bị xóa)",
                        ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText("⚠ Xác nhận xóa bài test");
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.YES) {
                    boolean ok = DatabaseManager.getInstance().deleteBankAdmin(b.getId());
                    if (ok) {
                        showAlert("✅ Đã xóa bài test: " + b.getBankName());
                        buildUI();
                    } else {
                        showAlert("❌ Xóa thất bại.");
                    }
                }
            });
            actions.getChildren().add(deleteBtn);

            row.getChildren().addAll(idLabel, nameLabel, creatorLabel, examBox, countLabel, visBtn, dateLabel, actions);
            panel.getChildren().add(row);
        }

        Region bottom = new Region();
        bottom.setStyle("-fx-background-color: #f1f5f9; -fx-pref-height: 2; -fx-background-radius: 0 0 12 12;");
        panel.getChildren().add(bottom);

        return panel;
    }

    // ==================== HELPERS ====================

    private Label makeHeaderCell(String text, double width) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-text-transform: uppercase;");
        label.setPrefWidth(width);
        return label;
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public void dispose() {
        // Nothing to dispose for now
    }
}
