package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.User;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;

import java.util.function.Consumer;

public class AuthView extends StackPane {
    private final Consumer<User> onLogin;
    private boolean isLoginMode = true;
    private TextField usernameField;
    private PasswordField passwordField;
    private ComboBox<String> roleCombo;
    private VBox roleGroup;
    private Label titleLabel;
    private Button submitBtn;
    private Hyperlink toggleLink;
    private Label switchText;

    public AuthView(Consumer<User> onLogin) {
        this.onLogin = onLogin;
        getStyleClass().add("auth-page");
        buildUI();
    }

    private void buildUI() {
        VBox card = new VBox(0);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(440);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));

        HBox logoBox = new HBox(8);
        logoBox.setAlignment(Pos.CENTER);
        Label logoIcon = new Label("◆");
        logoIcon.setStyle("-fx-font-size: 28px; -fx-text-fill: #1e3fae;");
        Label logoText = new Label("AkatsukiApp");
        logoText.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #1e3fae;");
        logoBox.getChildren().addAll(logoIcon, logoText);

        titleLabel = new Label("Welcome Back");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-padding: 24 0 24 0;");

        Label userLabel = new Label("USERNAME");
        userLabel.getStyleClass().add("form-label");
        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("form-input");

        Label passLabel = new Label("PASSWORD");
        passLabel.getStyleClass().add("form-label");
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.getStyleClass().add("form-input");

        Label roleLabel = new Label("ROLE");
        roleLabel.getStyleClass().add("form-label");
        roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Student", "Lecturer", "Admin");
        roleCombo.setValue("Student");
        roleCombo.getStyleClass().add("form-input");
        roleCombo.setMaxWidth(Double.MAX_VALUE);

        roleGroup = new VBox(8, roleLabel, roleCombo);
        roleGroup.setVisible(false);
        roleGroup.setManaged(false);

        submitBtn = new Button("Sign In");
        submitBtn.getStyleClass().addAll("btn", "btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleSubmit());

        passwordField.setOnAction(e -> handleSubmit());

        switchText = new Label("Don't have an account? ");
        switchText.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        toggleLink = new Hyperlink("Sign Up");
        toggleLink.setStyle("-fx-text-fill: #1e3fae; -fx-font-weight: 700; -fx-font-size: 14px;");
        toggleLink.setOnAction(e -> toggleMode());

        HBox footer = new HBox(switchText, toggleLink);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(24, 0, 0, 0));

        Label msgLabel = new Label();
        msgLabel.setId("auth-msg");
        msgLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
        msgLabel.setVisible(false);

        VBox formBox = new VBox(16,
                logoBox, titleLabel,
                new VBox(8, userLabel, usernameField),
                new VBox(8, passLabel, passwordField),
                roleGroup, msgLabel, submitBtn, footer
        );
        formBox.setAlignment(Pos.CENTER);

        card.getChildren().add(formBox);
        setAlignment(Pos.CENTER);
        getChildren().add(card);
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            titleLabel.setText("Welcome Back");
            submitBtn.setText("Sign In");
            toggleLink.setText("Sign Up");
            switchText.setText("Don't have an account? ");
            roleGroup.setVisible(false);
            roleGroup.setManaged(false);
        } else {
            titleLabel.setText("Create Account");
            submitBtn.setText("Sign Up");
            toggleLink.setText("Sign In");
            switchText.setText("Already have an account? ");
            roleGroup.setVisible(true);
            roleGroup.setManaged(true);
        }
    }

    private void handleSubmit() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            showMessage("Username and password are required");
            return;
        }

        submitBtn.setDisable(true);
        submitBtn.setText("Please wait...");

        try {
            if (isLoginMode) {
                User user = DatabaseManager.getInstance().login(username, password);
                if (user != null) {
                    onLogin.accept(user);
                } else {
                    showMessage("Invalid username or password");
                }
            } else {
                int role = switch (roleCombo.getValue()) {
                    case "Admin" -> 0;
                    case "Lecturer" -> 1;
                    default -> 2;
                };
                User user = DatabaseManager.getInstance().register(username, password, role);
                if (user != null) {
                    showMessage("Account created! Please sign in.");
                    toggleMode();
                } else {
                    showMessage("Registration failed");
                }
            }
        } catch (Exception e) {
            showMessage(e.getMessage().contains("UNIQUE") ? "Username already exists" : e.getMessage());
        }

        submitBtn.setDisable(false);
        submitBtn.setText(isLoginMode ? "Sign In" : "Sign Up");
    }

    private void showMessage(String msg) {
        Label msgLabel = (Label) lookup("#auth-msg");
        if (msgLabel != null) {
            msgLabel.setText(msg);
            msgLabel.setVisible(true);
        }
    }
}
