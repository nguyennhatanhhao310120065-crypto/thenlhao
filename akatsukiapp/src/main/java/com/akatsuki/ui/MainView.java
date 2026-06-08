package com.akatsuki.ui;

import com.akatsuki.model.User;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MainView extends BorderPane {
    private final User user;
    private final Runnable onLogout;
    private String activeTab = "library";
    private StackPane contentArea;
    private HBox navLinks;

    private LibraryView libraryView;
    private CreateTestView createTestView;
    private QuestionBankView questionBankView;
    private TestEngineView testEngineView;
    private PracticeView practiceView;
    private ResultsView resultsView;
    private SavedQuestionsView savedQuestionsView;
    private Integer currentTestBankId = null;

    public MainView(User user, Runnable onLogout) {
        this.user = user;
        this.onLogout = onLogout;
        buildNavbar();
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(32, 40, 32, 40));

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #f6f6f8;");

        setCenter(scrollPane);
        switchTab("library");
    }

    private void buildNavbar() {
        HBox navbar = new HBox();
        navbar.getStyleClass().add("navbar");
        navbar.setPadding(new Insets(12, 40, 12, 40));
        navbar.setAlignment(Pos.CENTER_LEFT);

        HBox logo = new HBox(8);
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setCursor(javafx.scene.Cursor.HAND);
        Label logoIcon = new Label("◆");
        logoIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-min-width: 36; -fx-min-height: 36; -fx-alignment: center; -fx-background-color: #1e3fae; -fx-background-radius: 8;");
        Label logoText = new Label("AkatsukiApp");
        logoText.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");
        logo.getChildren().addAll(logoIcon, logoText);
        logo.setOnMouseClicked(e -> requestSwitchTab("library"));

        Region spacer1 = new Region();
        spacer1.setPrefWidth(48);

        navLinks = new HBox(4);
        navLinks.setAlignment(Pos.CENTER_LEFT);

        String[][] tabs;
        if (user.getRole() == 2) {
            tabs = new String[][]{
                    {"library", "Library"}, {"practice", "Practice"}, {"saved", "Saved"}, {"results", "My Progress"}
            };
        } else {
            tabs = new String[][]{
                    {"library", "Library"}, {"create", "Create Test"}, {"banks", "Question Bank"}, {"saved", "Saved"}, {"results", "My Progress"}
            };
        }

        for (String[] tab : tabs) {
            Button btn = new Button(tab[1]);
            btn.getStyleClass().add("nav-btn");
            if (tab[0].equals(activeTab)) btn.getStyleClass().add("active");
            btn.setOnAction(e -> requestSwitchTab(tab[0]));
            navLinks.getChildren().add(btn);
        }

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox userSection = new HBox(12);
        userSection.setAlignment(Pos.CENTER_RIGHT);
        userSection.setStyle("-fx-padding: 0 0 0 24; -fx-border-color: #e2e8f0 transparent transparent transparent; -fx-border-width: 0 0 0 1;");

        VBox userInfo = new VBox(2);
        userInfo.setAlignment(Pos.CENTER_RIGHT);
        Label userName = new Label(user.getUsername());
        userName.setStyle("-fx-font-size: 12px; -fx-font-weight: 700;");
        Label userRole = new Label(user.getRoleLabel().toUpperCase());
        userRole.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        userInfo.getChildren().addAll(userName, userRole);

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 20px; -fx-min-width: 36; -fx-min-height: 36; -fx-alignment: center; -fx-background-color: #f1f5f9; -fx-background-radius: 8;");

        Button logoutBtn = new Button("⏻");
        logoutBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8; -fx-background-color: transparent; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            if (confirmLeaveCreateTest()) {
                disposeCurrentView();
                onLogout.run();
            }
        });

        userSection.getChildren().addAll(userInfo, avatar, logoutBtn);

        navbar.getChildren().addAll(logo, spacer1, navLinks, spacer2, userSection);
        navbar.setStyle("-fx-background-color: rgba(255,255,255,0.92); -fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");

        setTop(navbar);
    }

    /**
     * Check if CreateTestView has unsaved data and show confirmation dialog.
     * Returns true if it's safe to proceed (no unsaved data, or user confirmed).
     */
    private boolean confirmLeaveCreateTest() {
        if (createTestView != null && createTestView.isGenerating()) {
            showInfoAlert("⏳ AI đang tạo câu hỏi. Vui lòng đợi quá trình hoàn tất trước khi chuyển trang.");
            return false;
        }
        if (createTestView == null || !createTestView.hasUnsavedData()) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Test Data");
        alert.setHeaderText("⚠ Đề thi chưa được lưu!");
        alert.setContentText("Bạn có câu hỏi đã tạo nhưng chưa lưu vào Question Bank. Nếu chuyển sang mục khác, dữ liệu sẽ bị mất.\n\nBạn muốn làm gì?");

        ButtonType saveBtn = new ButtonType("💾 Lưu và chuyển", ButtonBar.ButtonData.YES);
        ButtonType discardBtn = new ButtonType("Không lưu, chuyển luôn", ButtonBar.ButtonData.NO);
        ButtonType cancelBtn = new ButtonType("Ở lại", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveBtn, discardBtn, cancelBtn);

        var result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == saveBtn) {
                boolean saved = createTestView.trySave();
                if (saved) {
                    showInfoAlert("✅ Đã lưu đề thi thành công!");
                    return true;
                } else {
                    showInfoAlert("❌ Lưu thất bại. Vui lòng thử lại.");
                    return false;
                }
            } else if (result.get() == discardBtn) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private void showInfoAlert(String msg) {
        Alert info = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        info.setHeaderText(null);
        info.showAndWait();
    }

    /**
     * Called by nav buttons — checks for unsaved data before switching.
     */
    private void requestSwitchTab(String tabId) {
        if (tabId.equals(activeTab)) return;
        if (confirmLeaveCreateTest()) {
            switchTab(tabId);
        }
    }

    private void switchTab(String tabId) {
        disposeCurrentView();
        currentTestBankId = null;
        activeTab = tabId;

        updateNavButtons();
        contentArea.getChildren().clear();

        switch (tabId) {
            case "library" -> {
                libraryView = new LibraryView(user, this::startTest);
                contentArea.getChildren().add(libraryView);
            }
            case "practice" -> {
                practiceView = new PracticeView(user);
                contentArea.getChildren().add(practiceView);
            }
            case "create" -> {
                createTestView = new CreateTestView(user);
                contentArea.getChildren().add(createTestView);
            }
            case "banks" -> {
                questionBankView = new QuestionBankView(user);
                contentArea.getChildren().add(questionBankView);
            }
            case "saved" -> {
                savedQuestionsView = new SavedQuestionsView(user);
                contentArea.getChildren().add(savedQuestionsView);
            }
            case "results" -> {
                resultsView = new ResultsView(user);
                contentArea.getChildren().add(resultsView);
            }
        }
    }

    private void startTest(int bankId) {
        if (!confirmLeaveCreateTest()) return;
        disposeCurrentView();
        currentTestBankId = bankId;
        contentArea.getChildren().clear();
        testEngineView = new TestEngineView(user, bankId, () -> switchTab("library"));
        contentArea.getChildren().add(testEngineView);
    }

    private void updateNavButtons() {
        for (var node : navLinks.getChildren()) {
            if (node instanceof Button btn) {
                btn.getStyleClass().remove("active");
            }
        }
        for (var node : navLinks.getChildren()) {
            if (node instanceof Button btn) {
                String text = btn.getText();
                boolean match = switch (activeTab) {
                    case "library" -> "Library".equals(text);
                    case "practice" -> "Practice".equals(text);
                    case "create" -> "Create Test".equals(text);
                    case "banks" -> "Question Bank".equals(text);
                    case "saved" -> "Saved".equals(text);
                    case "results" -> "My Progress".equals(text);
                    default -> false;
                };
                if (match) btn.getStyleClass().add("active");
            }
        }
    }

    private void disposeCurrentView() {
        if (testEngineView != null) { testEngineView.dispose(); testEngineView = null; }
        if (practiceView != null) { practiceView.dispose(); practiceView = null; }
        if (createTestView != null) { createTestView.dispose(); createTestView = null; }
        if (questionBankView != null) { questionBankView.dispose(); questionBankView = null; }
        if (savedQuestionsView != null) { savedQuestionsView.dispose(); savedQuestionsView = null; }
    }
}
