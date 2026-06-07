package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.QuestionBank;
import com.akatsuki.model.User;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.function.Consumer;

public class LibraryView extends VBox {
    private final User user;
    private final Consumer<Integer> onStartTest;
    private FlowPane bankGrid;
    private String currentFilter = "All";

    public LibraryView(User user, Consumer<Integer> onStartTest) {
        this.user = user;
        this.onStartTest = onStartTest;
        setSpacing(0);
        buildUI();
        loadBanks();
    }

    private void buildUI() {
        HBox header = new HBox();
        header.setSpacing(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 32, 0));

        VBox titleBox = new VBox(8);
        Label title = new Label("Test Library");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 700;");
        Label subtitle = new Label("Choose a test to start your practice session.");
        subtitle.setStyle("-fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);

        HBox filterBox = new HBox(4);
        filterBox.getStyleClass().add("filter-tabs");
        String[] filters = {"All", "IELTS", "TOEIC", "VSTEP", "GENERAL"};
        for (String f : filters) {
            Button btn = new Button(f);
            btn.getStyleClass().add("filter-tab");
            if (f.equals(currentFilter)) btn.getStyleClass().add("active");
            btn.setOnAction(e -> {
                currentFilter = f;
                filterBox.getChildren().forEach(c -> c.getStyleClass().remove("active"));
                btn.getStyleClass().add("active");
                loadBanks();
            });
            filterBox.getChildren().add(btn);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(titleBox, spacer, filterBox);

        bankGrid = new FlowPane();
        bankGrid.setHgap(24);
        bankGrid.setVgap(24);
        bankGrid.setPrefWrapLength(1200);

        getChildren().addAll(header, bankGrid);
    }

    private void loadBanks() {
        bankGrid.getChildren().clear();
        List<QuestionBank> banks = DatabaseManager.getInstance().getBanks(user.getId());
        List<QuestionBank> filtered = currentFilter.equals("All") ? banks :
                banks.stream().filter(b -> currentFilter.equals(b.getExamType())).toList();

        if (filtered.isEmpty()) {
            Label empty = new Label("No tests available yet.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-padding: 80;");
            bankGrid.getChildren().add(empty);
            return;
        }

        for (QuestionBank b : filtered) {
            VBox card = createBankCard(b);
            bankGrid.getChildren().add(card);
        }
    }

    private VBox createBankCard(QuestionBank bank) {
        VBox card = new VBox(0);
        card.getStyleClass().add("bank-card");
        card.setPrefWidth(280);
        card.setMaxWidth(280);

        VBox imgBox = new VBox();
        imgBox.setPrefHeight(120);
        imgBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #1e3fae, #6366f1); -fx-background-radius: 16 16 0 0;");
        imgBox.setAlignment(Pos.TOP_LEFT);
        imgBox.setPadding(new Insets(12));
        Label badge = new Label(bank.getExamType());
        badge.getStyleClass().addAll("badge", "badge-primary");
        badge.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-text-fill: #1e3fae;");
        imgBox.getChildren().add(badge);

        VBox body = new VBox(8);
        body.setPadding(new Insets(16));

        Label name = new Label(bank.getBankName());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");
        name.setWrapText(true);

        HBox meta = new HBox(16);
        meta.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        Label qCount = new Label("📝 " + bank.getQuestionCount() + " Qs");
        Label creator = new Label("👤 " + (bank.getCreatorName() != null ? bank.getCreatorName() : "Unknown"));
        qCount.setStyle("-fx-text-fill: #94a3b8;");
        creator.setStyle("-fx-text-fill: #94a3b8;");
        meta.getChildren().addAll(qCount, creator);

        Button startBtn = new Button("Start Test ▶");
        startBtn.getStyleClass().addAll("btn", "btn-dark");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(e -> onStartTest.accept(bank.getId()));

        body.getChildren().addAll(name, meta, startBtn);
        card.getChildren().addAll(imgBox, body);
        card.setOnMouseClicked(e -> {
            if (e.getTarget() != startBtn) onStartTest.accept(bank.getId());
        });
        return card;
    }

    public void refresh() {
        loadBanks();
    }
}
