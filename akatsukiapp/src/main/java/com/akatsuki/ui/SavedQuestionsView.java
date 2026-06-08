package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.Question;
import com.akatsuki.model.User;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;

import java.util.*;

/**
 * Saved Questions, organised into folders by the listening test (part) they belong to.
 * Each folder can be opened to listen to its audio + transcript and review its questions.
 */
public class SavedQuestionsView extends VBox {
    private final User user;
    private VBox contentBox;
    private MediaPlayer mediaPlayer;
    private TranscriptPane transcriptPane;

    public SavedQuestionsView(User user) {
        this.user = user;
        setSpacing(0);
        contentBox = new VBox();
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        getChildren().add(contentBox);
        loadFolders();
    }

    // ======================== FOLDER LIST ========================

    private void loadFolders() {
        disposeMedia();
        contentBox.getChildren().clear();

        VBox header = new VBox(8);
        header.setPadding(new Insets(0, 0, 24, 0));
        Label title = new Label("Saved Questions");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 700;");
        Label subtitle = new Label("Các câu hỏi đã lưu được nhóm theo từng đề nghe (part).");
        subtitle.setStyle("-fx-text-fill: #64748b;");
        header.getChildren().addAll(title, subtitle);

        List<Question> questions = DatabaseManager.getInstance().getSavedQuestions(user.getId());

        if (questions.isEmpty()) {
            Label empty = new Label("Chưa có câu hỏi nào được lưu.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-padding: 60;");
            contentBox.getChildren().addAll(header, empty);
            return;
        }

        // Group by the listening test (part). partLabel = "bank_name | exam_type".
        Map<String, List<Question>> grouped = new LinkedHashMap<>();
        for (Question q : questions) {
            String key = q.getPartLabel() != null ? q.getPartLabel() : "Khác";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(q);
        }

        VBox listBox = new VBox(16);
        Label count = new Label("📁 " + grouped.size() + " đề • " + questions.size() + " câu đã lưu");
        count.setStyle("-fx-font-weight: 700; -fx-font-size: 16px; -fx-padding: 0 0 4 0;");
        listBox.getChildren().add(count);

        for (var entry : grouped.entrySet()) {
            listBox.getChildren().add(buildFolderCard(entry.getKey(), entry.getValue()));
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        contentBox.getChildren().addAll(header, scroll);
    }

    private VBox buildFolderCard(String label, List<Question> questions) {
        String bankName = label.contains(" | ") ? label.substring(0, label.lastIndexOf(" | ")) : label;
        String examType = label.contains(" | ") ? label.substring(label.lastIndexOf(" | ") + 3) : "";

        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14; -fx-border-width: 2;");
        card.setPadding(new Insets(20));
        card.setCursor(javafx.scene.Cursor.HAND);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label folderIcon = new Label("📁");
        folderIcon.setStyle("-fx-font-size: 26px;");
        VBox titleBox = new VBox(2);
        Label name = new Label(bankName);
        name.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        name.setWrapText(true);
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        if (!examType.isBlank()) {
            Label typeBadge = new Label(examType);
            typeBadge.getStyleClass().addAll("badge", "badge-slate");
            metaRow.getChildren().add(typeBadge);
        }
        Label qCount = new Label(questions.size() + " câu đã lưu");
        qCount.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        metaRow.getChildren().add(qCount);
        boolean hasAudio = questions.get(0).getAudioUrl() != null && !questions.get(0).getAudioUrl().isBlank();
        Label audioBadge = new Label(hasAudio ? "🎧 Có audio & transcript" : "📝 Chỉ transcript");
        audioBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #1e3fae;");
        metaRow.getChildren().add(audioBadge);
        titleBox.getChildren().addAll(name, metaRow);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label openHint = new Label("Mở ▶");
        openHint.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #1e3fae;");

        top.getChildren().addAll(folderIcon, titleBox, openHint);
        card.getChildren().add(top);

        card.setOnMouseClicked(e -> showFolder(label, bankName, examType));
        return card;
    }

    // ======================== FOLDER DETAIL (audio + transcript + questions) ========================

    private void showFolder(String label, String bankName, String examType) {
        disposeMedia();
        contentBox.getChildren().clear();

        // Reload questions for this folder fresh (so removals/additions reflect immediately)
        List<Question> all = DatabaseManager.getInstance().getSavedQuestions(user.getId());
        List<Question> questions = new ArrayList<>();
        for (Question q : all) {
            String key = q.getPartLabel() != null ? q.getPartLabel() : "Khác";
            if (key.equals(label)) questions.add(q);
        }
        if (questions.isEmpty()) { loadFolders(); return; }

        String audioUrl = questions.get(0).getAudioUrl();
        String transcript = questions.get(0).getTranscript();

        if (audioUrl != null && !audioUrl.isBlank()) {
            try {
                Media media = new Media(audioUrl);
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setOnError(() -> System.err.println("MediaPlayer error: " + mediaPlayer.getError()));
            } catch (Exception ex) { mediaPlayer = null; }
        }

        // Top bar
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 16, 0));
        Button backBtn = new Button("◀ Tất cả đề đã lưu");
        backBtn.getStyleClass().addAll("btn", "btn-ghost");
        backBtn.setOnAction(e -> loadFolders());
        topBar.getChildren().add(backBtn);

        VBox headerBox = new VBox(4);
        Label title = new Label("📁 " + bankName);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 800;");
        Label sub = new Label((examType.isBlank() ? "" : examType + " • ") + questions.size()
                + " câu đã lưu — nghe lại audio & transcript để ôn tập.");
        sub.setStyle("-fx-text-fill: #64748b; -fx-padding: 0 0 12 0;");
        headerBox.getChildren().addAll(title, sub);

        // Left: transcript + audio
        transcriptPane = new TranscriptPane(transcript != null ? transcript : "", mediaPlayer);
        transcriptPane.setPrefWidth(560);
        transcriptPane.setMinWidth(460);
        transcriptPane.setMinHeight(520);

        // Right: saved question cards
        VBox questionsCol = new VBox(14);
        for (int i = 0; i < questions.size(); i++) {
            questionsCol.getChildren().add(createSavedCard(questions.get(i), i + 1, label, bankName, examType));
        }
        ScrollPane qScroll = new ScrollPane(questionsCol);
        qScroll.setFitToWidth(true);
        qScroll.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(qScroll, Priority.ALWAYS);

        HBox grid = new HBox(24, transcriptPane, qScroll);
        VBox.setVgrow(grid, Priority.ALWAYS);

        contentBox.getChildren().addAll(topBar, headerBox, grid);
    }

    private VBox createSavedCard(Question q, int idx, String label, String bankName, String examType) {
        VBox card = new VBox(12);
        card.getStyleClass().add("saved-card");
        card.setPadding(new Insets(20));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label numLabel = new Label(String.valueOf(idx));
        numLabel.setStyle("-fx-min-width: 32; -fx-min-height: 32; -fx-alignment: center; -fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #f1f5f9; -fx-border-radius: 8; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        Label typeBadge = new Label(Question.getTypeLabel(q.getNormalizedType()));
        typeBadge.getStyleClass().addAll("badge", "badge-primary");
        headerRow.getChildren().addAll(numLabel, typeBadge);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button removeBtn = new Button("🗑 Remove");
        removeBtn.getStyleClass().addAll("btn", "btn-danger");
        removeBtn.setOnAction(e -> {
            DatabaseManager.getInstance().unsaveQuestion(user.getId(), q.getId());
            // Rebuild this folder (or go back if it becomes empty)
            showFolder(label, bankName, examType);
        });

        HBox headerWithBtn = new HBox(headerRow, spacer, removeBtn);
        headerWithBtn.setAlignment(Pos.CENTER_LEFT);

        Label qText = new Label(q.getQuestionText());
        qText.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
        qText.setWrapText(true);

        String t = q.getNormalizedType();
        VBox bodyBox = new VBox(12);
        bodyBox.getChildren().add(qText);

        if (("mcq".equals(t) || "matching".equals(t)) && q.getOptions() != null && !q.getOptions().isEmpty()) {
            VBox optsBox = new VBox(4);
            optsBox.setPadding(new Insets(0, 0, 0, 8));
            for (String opt : q.getOptions()) {
                Label optLabel = new Label(opt);
                optLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
                optsBox.getChildren().add(optLabel);
            }
            bodyBox.getChildren().add(optsBox);
        }

        HBox answerBox = new HBox();
        answerBox.setMaxWidth(Double.MAX_VALUE);
        answerBox.setStyle("-fx-padding: 14; -fx-background-color: #ecfdf5; -fx-background-radius: 12; -fx-border-color: #d1fae5; -fx-border-radius: 12;");
        VBox answerContent = new VBox(4);
        HBox.setHgrow(answerContent, Priority.ALWAYS);
        Label ansLabel = new Label("CORRECT ANSWER");
        ansLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: rgba(5,150,105,0.5);");
        Label ansVal = new Label(q.getCorrectAnswer());
        ansVal.setWrapText(true);
        ansVal.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #047857;");
        answerContent.getChildren().addAll(ansLabel, ansVal);
        answerBox.getChildren().add(answerContent);
        bodyBox.getChildren().add(answerBox);

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
            bodyBox.getChildren().add(quoteBox);
        }

        if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
            VBox explBox = new VBox(4);
            explBox.setStyle("-fx-padding: 12 0 0 0; -fx-border-color: #f1f5f9 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
            Label explLabel = new Label("💡 EXPLANATION");
            explLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1e3fae;");
            Label explText = new Label(q.getExplanation());
            explText.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-line-spacing: 4;");
            explText.setWrapText(true);
            explBox.getChildren().addAll(explLabel, explText);
            bodyBox.getChildren().add(explBox);
        }

        card.getChildren().addAll(headerWithBtn, bodyBox);
        return card;
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

    public void refresh() {
        loadFolders();
    }
}
