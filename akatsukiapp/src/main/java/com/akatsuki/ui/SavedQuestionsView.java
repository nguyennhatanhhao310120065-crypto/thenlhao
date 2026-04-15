package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.Question;
import com.akatsuki.model.User;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class SavedQuestionsView extends VBox {
    private final User user;
    private VBox listBox;

    public SavedQuestionsView(User user) {
        this.user = user;
        setSpacing(0);
        buildUI();
        loadSaved();
    }

    private void buildUI() {
        VBox header = new VBox(8);
        header.setPadding(new Insets(0, 0, 32, 0));
        Label title = new Label("Saved Questions");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 700;");
        Label subtitle = new Label("Review questions you've saved for further study.");
        subtitle.setStyle("-fx-text-fill: #64748b;");
        header.getChildren().addAll(title, subtitle);

        listBox = new VBox(16);

        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, scrollPane);
    }

    private void loadSaved() {
        listBox.getChildren().clear();

        List<Question> questions = DatabaseManager.getInstance().getSavedQuestions(user.getId());

        if (questions.isEmpty()) {
            Label empty = new Label("No questions saved yet.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-padding: 60;");
            empty.setAlignment(Pos.CENTER);
            listBox.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            VBox card = createSavedCard(q, i + 1);
            listBox.getChildren().add(card);
        }
    }

    private VBox createSavedCard(Question q, int idx) {
        VBox card = new VBox(16);
        card.getStyleClass().add("saved-card");
        card.setPadding(new Insets(24));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(Question.getTypeLabel(q.getNormalizedType()));
        typeBadge.getStyleClass().addAll("badge", "badge-primary");

        if (q.getPartLabel() != null) {
            Label fromLabel = new Label("From: " + q.getPartLabel());
            fromLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-text-transform: uppercase;");
            headerRow.getChildren().addAll(typeBadge, fromLabel);
        } else {
            headerRow.getChildren().add(typeBadge);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button removeBtn = new Button("🗑 Remove");
        removeBtn.getStyleClass().addAll("btn", "btn-danger");
        removeBtn.setOnAction(e -> {
            DatabaseManager.getInstance().unsaveQuestion(user.getId(), q.getId());
            loadSaved();
        });

        HBox headerWithBtn = new HBox(headerRow, spacer, removeBtn);
        headerWithBtn.setAlignment(Pos.CENTER_LEFT);

        HBox bodyRow = new HBox(16);
        Label numLabel = new Label(String.valueOf(idx));
        numLabel.setStyle("-fx-min-width: 40; -fx-min-height: 40; -fx-alignment: center; -fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #f1f5f9; -fx-border-radius: 8; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        VBox contentBox = new VBox(12);
        Label qText = new Label(q.getQuestionText());
        qText.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
        qText.setWrapText(true);

        HBox answerBox = new HBox();
        answerBox.setStyle("-fx-padding: 12; -fx-background-color: #ecfdf5; -fx-background-radius: 12; -fx-border-color: #d1fae5; -fx-border-radius: 12;");
        VBox answerContent = new VBox(4);
        Label ansLabel = new Label("CORRECT ANSWER");
        ansLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: rgba(5,150,105,0.5);");
        Label ansVal = new Label(q.getCorrectAnswer());
        ansVal.setStyle("-fx-font-weight: 700; -fx-text-fill: #047857;");
        answerContent.getChildren().addAll(ansLabel, ansVal);
        answerBox.getChildren().add(answerContent);

        contentBox.getChildren().addAll(qText, answerBox);

        if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
            VBox explBox = new VBox(8);
            explBox.setStyle("-fx-padding: 16 0 0 0; -fx-border-color: #f1f5f9 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
            Label explLabel = new Label("💡 EXPLANATION");
            explLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1e3fae;");
            Label explText = new Label(q.getExplanation());
            explText.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-line-spacing: 4;");
            explText.setWrapText(true);
            explBox.getChildren().addAll(explLabel, explText);
            contentBox.getChildren().add(explBox);
        }

        bodyRow.getChildren().addAll(numLabel, contentBox);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        card.getChildren().addAll(headerWithBtn, bodyRow);
        return card;
    }

    public void refresh() {
        loadSaved();
    }
}
