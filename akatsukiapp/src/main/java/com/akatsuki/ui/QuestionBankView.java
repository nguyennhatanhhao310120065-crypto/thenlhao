package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.*;

/**
 * Question Bank = lịch sử tất cả câu hỏi mà người dùng đã tạo (qua các đề).
 * Hiển thị dạng danh sách phẳng, không phải danh sách đề.
 */
public class QuestionBankView extends VBox {
    private final User user;
    private VBox contentBox;
    private String activeTypeFilter = null;

    public QuestionBankView(User user) {
        this.user = user;
        setSpacing(0);
        contentBox = new VBox();
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        getChildren().add(contentBox);
        buildList();
    }

    private void buildList() {
        contentBox.getChildren().clear();

        VBox header = new VBox(8);
        header.setPadding(new Insets(0, 0, 32, 0));
        Label title = new Label("Question Bank");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 700;");
        Label subtitle = new Label("Lịch sử tất cả câu hỏi bạn đã tạo.");
        subtitle.setStyle("-fx-text-fill: #64748b;");
        header.getChildren().addAll(title, subtitle);

        List<Question> allQ = DatabaseManager.getInstance().getCreatedQuestions(user.getId());

        VBox listBox = new VBox(16);

        if (allQ.isEmpty()) {
            Label empty = new Label("Bạn chưa tạo câu hỏi nào.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-padding: 60;");
            listBox.getChildren().add(empty);
            contentBox.getChildren().addAll(header, listBox);
            return;
        }

        // Group theo loại câu hỏi để làm bộ lọc
        Map<String, List<Question>> grouped = new LinkedHashMap<>();
        for (Question q : allQ) grouped.computeIfAbsent(q.getNormalizedType(), k -> new ArrayList<>()).add(q);

        if (grouped.size() > 1) {
            FlowPane filterTabs = new FlowPane(6, 6);
            filterTabs.setPadding(new Insets(0, 0, 16, 0));
            Button allTab = new Button("All (" + allQ.size() + ")");
            allTab.getStyleClass().add("filter-tab");
            if (activeTypeFilter == null) allTab.getStyleClass().add("active");
            allTab.setOnAction(e -> { activeTypeFilter = null; buildList(); });
            filterTabs.getChildren().add(allTab);

            for (var entry : grouped.entrySet()) {
                Button tab = new Button(Question.getTypeLabel(entry.getKey()) + " (" + entry.getValue().size() + ")");
                tab.getStyleClass().add("filter-tab");
                if (entry.getKey().equals(activeTypeFilter)) tab.getStyleClass().add("active");
                String type = entry.getKey();
                tab.setOnAction(e -> { activeTypeFilter = type; buildList(); });
                filterTabs.getChildren().add(tab);
            }
            listBox.getChildren().add(filterTabs);
        }

        List<Question> displayQuestions;
        if (activeTypeFilter != null && grouped.containsKey(activeTypeFilter)) {
            displayQuestions = grouped.get(activeTypeFilter);
        } else {
            displayQuestions = allQ;
        }

        Label count = new Label("📊 " + displayQuestions.size() + " câu hỏi");
        count.setStyle("-fx-font-weight: 700; -fx-font-size: 16px; -fx-padding: 0 0 4 0;");
        listBox.getChildren().add(count);

        for (int i = 0; i < displayQuestions.size(); i++) {
            listBox.getChildren().add(buildQuestionCard(displayQuestions.get(i), i + 1));
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        contentBox.getChildren().addAll(header, scroll);
    }

    private VBox buildQuestionCard(Question q, int idx) {
        VBox qCard = new VBox(10);
        qCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 2;");
        qCard.setPadding(new Insets(16));

        HBox qHeader = new HBox(8);
        qHeader.setAlignment(Pos.CENTER_LEFT);
        Label numLabel = new Label(String.valueOf(idx));
        numLabel.setStyle("-fx-min-width: 28; -fx-min-height: 28; -fx-alignment: center; -fx-background-color: #1e3fae; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700;");
        Label typeBadge = new Label(Question.getTypeLabel(q.getNormalizedType()));
        typeBadge.getStyleClass().addAll("badge", "badge-slate");
        qHeader.getChildren().addAll(numLabel, typeBadge);

        if (q.getPartLabel() != null) {
            Label fromLabel = new Label("From: " + q.getPartLabel());
            fromLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
            qHeader.getChildren().add(fromLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        qHeader.getChildren().add(spacer);

        if (q.getCreatedAt() != null && !q.getCreatedAt().isEmpty()) {
            Label date = new Label(q.getCreatedAt().substring(0, Math.min(10, q.getCreatedAt().length())));
            date.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
            qHeader.getChildren().add(date);
        }

        Label qTextLabel = new Label(q.getQuestionText());
        qTextLabel.setWrapText(true);
        qTextLabel.setStyle("-fx-font-weight: 600;");

        String t = q.getNormalizedType();
        qCard.getChildren().addAll(qHeader, qTextLabel);

        if (("mcq".equals(t) || "matching".equals(t)) && q.getOptions() != null && !q.getOptions().isEmpty()) {
            VBox optsBox = new VBox(4);
            optsBox.setPadding(new Insets(4, 0, 4, 8));
            for (String opt : q.getOptions()) {
                Label optLabel = new Label(opt);
                optLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
                optsBox.getChildren().add(optLabel);
            }
            qCard.getChildren().add(optsBox);
        }

        HBox ansBox = new HBox();
        ansBox.setStyle("-fx-padding: 12; -fx-background-color: #ecfdf5; -fx-background-radius: 8;");
        VBox ansContent = new VBox(2);
        Label ansLabel = new Label("CORRECT ANSWER");
        ansLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: rgba(5,150,105,0.5);");
        Label ansVal = new Label(q.getCorrectAnswer());
        ansVal.setStyle("-fx-font-weight: 700; -fx-text-fill: #047857;");
        ansContent.getChildren().addAll(ansLabel, ansVal);
        ansBox.getChildren().add(ansContent);
        qCard.getChildren().add(ansBox);

        String quote = q.getTranscriptQuote();
        if (quote != null && !quote.isEmpty()) {
            VBox quoteBox = new VBox(4);
            quoteBox.setStyle("-fx-padding: 10 14; -fx-background-color: linear-gradient(to bottom right, #fefce8, #fff7ed); -fx-background-radius: 8; -fx-border-color: #fde68a; -fx-border-radius: 8;");

            Label quoteLbl = new Label("📎 Transcript passage");
            quoteLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #92400e;");

            Label quoteText = new Label("\"" + quote + "\"");
            quoteText.setWrapText(true);
            quoteText.setStyle("-fx-font-size: 13px; -fx-text-fill: #78350f; -fx-font-style: italic;");

            quoteBox.getChildren().addAll(quoteLbl, quoteText);
            qCard.getChildren().add(quoteBox);
        }

        if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
            VBox explBox = new VBox(4);
            explBox.setStyle("-fx-padding: 12 0 0 0; -fx-border-color: #f1f5f9 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
            Label explTitle = new Label("💡 EXPLANATION");
            explTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1e3fae;");
            Label explText = new Label(q.getExplanation());
            explText.setWrapText(true);
            explText.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-line-spacing: 3;");
            explBox.getChildren().addAll(explTitle, explText);
            qCard.getChildren().add(explBox);
        }

        return qCard;
    }

    public void refresh() {
        buildList();
    }
}
