package com.akatsuki;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.User;
import com.akatsuki.ui.AuthView;
import com.akatsuki.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.nio.file.Path;

public class App extends Application {
    private Stage primaryStage;
    private Scene scene;
    private StackPane root;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        String dbPath = Path.of(System.getProperty("user.dir"), "akatsuki.db").toString();
        DatabaseManager.getInstance(dbPath);

        root = new StackPane();
        scene = new Scene(root, 1280, 800);

        try {
            var cssUrl = getClass().getResource("/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }

        showAuth();

        stage.setTitle("AkatsukiApp — AI Listening Test Platform");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

    private void showAuth() {
        root.getChildren().clear();
        AuthView authView = new AuthView(this::onLogin);
        root.getChildren().add(authView);
    }

    private void onLogin(User user) {
        root.getChildren().clear();
        MainView mainView = new MainView(user, this::showAuth);
        root.getChildren().add(mainView);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
