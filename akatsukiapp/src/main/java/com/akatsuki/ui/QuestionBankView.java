package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;

import java.util.*;

/**
 * Question Bank = lịch sử tất cả câu hỏi mà người dùng đã tạo (qua các đề).
 * Hiển thị dạng danh sách phẳng, không phải danh sách đề.
 */
public class QuestionBankView extends VBox {
    private final User user;
    private VBox contentBox;
    private String activeTypeFilter = null;
    private Set<Integer> savedIds = new HashSet<>();
    private MediaPlayer mediaPlayer;
    private TranscriptPane transcriptPane;

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
        savedIds = new HashSet<>(DatabaseManager.getInstance().getSavedQuestionIds(user.getId()));

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

        Button saveBtn = new Button();
        applySaveBtnStyle(saveBtn, savedIds.contains(q.getId()));
        saveBtn.setOnAction(e -> toggleSave(q, saveBtn));
        // Prevent the click from bubbling up and opening the detail panel.
        saveBtn.setOnMouseClicked(javafx.event.Event::consume);
        qHeader.getChildren().add(saveBtn);

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

        Label clickHint = new Label("🎧 Nhấn để nghe lại audio & kiểm tra trước khi lưu");
        clickHint.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #1e3fae; -fx-padding: 4 0 0 0;");
        qCard.getChildren().add(clickHint);

        qCard.setCursor(javafx.scene.Cursor.HAND);
        qCard.setOnMouseClicked(e -> showDetail(q));

        return qCard;
    }

    // ======================== DETAIL PANEL (listen & review before saving) ========================

    /** Opens a large panel with audio + transcript + the question, for reviewing before saving. */
    private void showDetail(Question q) {
        disposeMedia();
        contentBox.getChildren().clear();

        // Audio player from the bank's stored audio URL
        if (q.getAudioUrl() != null && !q.getAudioUrl().isEmpty()) {
            try {
                Media media = new Media(q.getAudioUrl());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setOnError(() -> System.err.println("MediaPlayer error: " + mediaPlayer.getError()));
            } catch (Exception ex) { mediaPlayer = null; }
        }

        // Top bar: back + title + save toggle
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 16, 0));
        Button backBtn = new Button("◀ Back to Question Bank");
        backBtn.getStyleClass().addAll("btn", "btn-ghost");
        backBtn.setOnAction(e -> { disposeMedia(); buildList(); });

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button saveBtn = new Button();
        applySaveBtnStyle(saveBtn, savedIds.contains(q.getId()));
        saveBtn.setOnAction(e -> toggleSave(q, saveBtn));
        topBar.getChildren().addAll(backBtn, topSpacer, saveBtn);

        Label heading = new Label("🎧 Nghe & kiểm tra câu hỏi");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: 800;");
        Label sub = new Label("Nghe lại đoạn audio và transcript để xác nhận câu hỏi trước khi lưu vào Saved.");
        sub.setStyle("-fx-text-fill: #64748b; -fx-padding: 0 0 12 0;");

        // Left: transcript + audio. Right: question detail
        transcriptPane = new TranscriptPane(q.getTranscript() != null ? q.getTranscript() : "", mediaPlayer);
        transcriptPane.setPrefWidth(560);
        transcriptPane.setMinWidth(460);
        transcriptPane.setMinHeight(520);

        VBox questionCol = new VBox(14);
        questionCol.getChildren().add(buildDetailQuestionCard(q));
        ScrollPane qScroll = new ScrollPane(questionCol);
        qScroll.setFitToWidth(true);
        qScroll.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(qScroll, Priority.ALWAYS);

        HBox grid = new HBox(24, transcriptPane, qScroll);
        VBox.setVgrow(grid, Priority.ALWAYS);

        contentBox.getChildren().addAll(topBar, heading, sub, grid);

        // Highlight the relevant transcript passage right away
        String quote = q.getTranscriptQuote();
        if (quote != null && !quote.isEmpty() && transcriptPane != null) {
            transcriptPane.highlightQuote(quote);
        }
    }

    private VBox buildDetailQuestionCard(Question q) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-color: #e2e8f0; -fx-border-radius: 16; -fx-border-width: 2;");
        card.setPadding(new Insets(20));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label typeBadge = new Label(Question.getTypeLabel(q.getNormalizedType()));
        typeBadge.getStyleClass().addAll("badge", "badge-primary");
        header.getChildren().add(typeBadge);
        if (q.getPartLabel() != null) {
            Label fromLabel = new Label("From: " + q.getPartLabel());
            fromLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
            header.getChildren().add(fromLabel);
        }

        Label qText = new Label(q.getQuestionText());
        qText.setWrapText(true);
        qText.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");
        card.getChildren().addAll(header, qText);

        String t = q.getNormalizedType();
        if (("mcq".equals(t) || "matching".equals(t)) && q.getOptions() != null && !q.getOptions().isEmpty()) {
            VBox optsBox = new VBox(4);
            optsBox.setPadding(new Insets(4, 0, 4, 8));
            for (String opt : q.getOptions()) {
                Label optLabel = new Label(opt);
                optLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
                optsBox.getChildren().add(optLabel);
            }
            card.getChildren().add(optsBox);
        }

        HBox ansBox = new HBox();
        ansBox.setMaxWidth(Double.MAX_VALUE);
        ansBox.setStyle("-fx-padding: 16; -fx-background-color: #ecfdf5; -fx-background-radius: 8;");
        VBox ansContent = new VBox(4);
        HBox.setHgrow(ansContent, Priority.ALWAYS);
        Label ansLabel = new Label("CORRECT ANSWER");
        ansLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: rgba(5,150,105,0.5);");
        Label ansVal = new Label(q.getCorrectAnswer());
        ansVal.setWrapText(true);
        ansVal.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #047857;");
        ansContent.getChildren().addAll(ansLabel, ansVal);
        ansBox.getChildren().add(ansContent);
        card.getChildren().add(ansBox);

        String quote = q.getTranscriptQuote();
        if (quote != null && !quote.isEmpty()) {
            VBox quoteBox = new VBox(4);
            quoteBox.setStyle("-fx-padding: 12 14; -fx-background-color: linear-gradient(to bottom right, #fefce8, #fff7ed); -fx-background-radius: 8; -fx-border-color: #fde68a; -fx-border-radius: 8;");
            quoteBox.setCursor(javafx.scene.Cursor.HAND);
            HBox quoteHeader = new HBox(6);
            quoteHeader.setAlignment(Pos.CENTER_LEFT);
            Label quoteLbl = new Label("📎 Transcript passage");
            quoteLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #92400e;");
            Region qs = new Region();
            HBox.setHgrow(qs, Priority.ALWAYS);
            quoteHeader.getChildren().addAll(quoteLbl, qs);
            if (mediaPlayer != null) {
                Label playLbl = new Label("▶ Nghe đoạn này");
                playLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1e3fae; -fx-padding: 2 10; -fx-background-color: rgba(30,63,174,0.1); -fx-background-radius: 99;");
                quoteHeader.getChildren().add(playLbl);
            }
            Label quoteText = new Label("\"" + quote + "\"");
            quoteText.setWrapText(true);
            quoteText.setStyle("-fx-font-size: 14px; -fx-text-fill: #78350f; -fx-font-style: italic;");
            quoteBox.getChildren().addAll(quoteHeader, quoteText);
            quoteBox.setOnMouseClicked(e -> { if (transcriptPane != null) transcriptPane.highlightQuoteAndPlay(quote); });
            card.getChildren().add(quoteBox);
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
            card.getChildren().add(explBox);
        }

        return card;
    }

    private void toggleSave(Question q, Button btn) {
        if (savedIds.contains(q.getId())) {
            DatabaseManager.getInstance().unsaveQuestion(user.getId(), q.getId());
            savedIds.remove(q.getId());
        } else {
            DatabaseManager.getInstance().saveQuestion(user.getId(), q.getId());
            savedIds.add(q.getId());
        }
        applySaveBtnStyle(btn, savedIds.contains(q.getId()));
    }

    private void disposeMedia() {
        if (transcriptPane != null) { transcriptPane.dispose(); transcriptPane = null; }
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.dispose(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    public void dispose() {
        disposeMedia();
    }

    /** Toggles the look of the Save button between "save" and "saved" states. */
    private void applySaveBtnStyle(Button btn, boolean saved) {
        if (saved) {
            btn.setText("✅ Saved");
            btn.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: white; -fx-padding: 4 12; " +
                    "-fx-background-color: #059669; -fx-background-radius: 8; -fx-cursor: hand;");
        } else {
            btn.setText("🔖 Save");
            btn.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #1e3fae; -fx-padding: 4 12; " +
                    "-fx-background-color: rgba(30,63,174,0.08); -fx-background-radius: 8; -fx-cursor: hand;");
        }
    }

    public void refresh() {
        buildList();
    }
}
