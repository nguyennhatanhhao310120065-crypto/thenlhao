package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.*;
import com.akatsuki.service.GeminiService;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;

public class CreateTestView extends VBox {
    private final User user;
    private int step = 1;
    private File audioFile;
    private String audioPath;
    private String transcript = "";
    private String examType = "IELTS";
    private List<String> selectedTypes = new ArrayList<>(List.of("mcq", "table_completion", "note_completion", "sentence_completion", "matching", "true_false_ng", "short_answer"));
    private int questionCount = 10;
    private List<Question> generatedQuestions;
    private String bankName = "";
    private boolean isPublic = false;
    private Map<String, String> customInstructions = new HashMap<>();
    private MediaPlayer mediaPlayer;
    private TranscriptPane transcriptPane;
    private String activeTypeFilter = null;

    private static final Map<String, String[][]> EXAM_CONFIG = new LinkedHashMap<>();
    static {
        EXAM_CONFIG.put("IELTS", new String[][]{{"mcq","Multiple Choice"},{"table_completion","Table Completion"},{"note_completion","Note Completion"},{"sentence_completion","Sentence Completion"},{"matching","Matching"},{"true_false_ng","T/F/Not Given"},{"short_answer","Short Answer"}});
        EXAM_CONFIG.put("TOEIC", new String[][]{{"mcq","Multiple Choice"},{"true_false","True/False"},{"fill_blank","Fill in Blank"},{"table_completion","Table Completion"},{"short_answer","Short Answer"}});
        EXAM_CONFIG.put("VSTEP", new String[][]{{"mcq","Multiple Choice"},{"true_false","True/False"},{"fill_blank","Fill in Blank"},{"table_completion","Table Completion"},{"note_completion","Note Completion"},{"short_answer","Short Answer"}});
        EXAM_CONFIG.put("GENERAL", new String[][]{{"mcq","Multiple Choice"},{"true_false","True/False"},{"fill_blank","Fill in Blank"},{"table_completion","Table Completion"},{"note_completion","Note Completion"},{"sentence_completion","Sentence Completion"},{"short_answer","Short Answer"},{"matching","Matching"}});
    }

    public CreateTestView(User user) {
        this.user = user;
        setSpacing(0);
        buildUI();
    }

    private void buildUI() {
        getChildren().clear();

        VBox header = new VBox(8);
        header.setPadding(new Insets(0, 0, 16, 0));
        Label title = new Label("Create New Test");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 700;");
        Label subtitle = new Label("Upload audio, configure question types, and generate AI-powered questions.");
        subtitle.setStyle("-fx-text-fill: #64748b;");

        HBox steps = new HBox(8);
        steps.setPadding(new Insets(24, 0, 0, 0));
        String[] stepLabels = {"Upload", "Configure", "Review"};
        for (int i = 0; i < 3; i++) {
            Label circle = new Label(String.valueOf(i + 1));
            circle.setStyle("-fx-min-width: 32; -fx-min-height: 32; -fx-alignment: center; -fx-background-radius: 16; -fx-font-weight: 700; -fx-font-size: 14px; " +
                    (step > i ? "-fx-background-color: #1e3fae; -fx-text-fill: white;" : "-fx-background-color: #e2e8f0; -fx-text-fill: #94a3b8;"));
            Label label = new Label(stepLabels[i]);
            label.setStyle("-fx-font-weight: 500; " + (step > i ? "-fx-text-fill: #0f172a;" : "-fx-text-fill: #94a3b8;"));
            steps.getChildren().addAll(circle, label);
            if (i < 2) {
                Region line = new Region();
                line.setPrefWidth(48);
                line.setPrefHeight(1);
                line.setStyle("-fx-background-color: #e2e8f0;");
                line.setTranslateY(16);
                steps.getChildren().add(line);
            }
        }

        header.getChildren().addAll(title, subtitle, steps);
        getChildren().add(header);

        switch (step) {
            case 1 -> buildStep1();
            case 2 -> buildStep2();
            case 3 -> buildStep3();
        }
    }

    private void buildStep1() {
        VBox uploadZone = new VBox(16);
        uploadZone.getStyleClass().add("upload-zone");
        uploadZone.setAlignment(Pos.CENTER);
        uploadZone.setPadding(new Insets(48));

        Label iconLabel = new Label("☁");
        iconLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: #1e3fae;");
        Label h3 = new Label("Upload Audio File");
        h3.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");
        Label desc = new Label("Supported formats: MP3, WAV, M4A, OGG");
        desc.setStyle("-fx-text-fill: #64748b;");

        Button selectBtn = new Button(audioFile != null ? audioFile.getName() : "Select File");
        selectBtn.getStyleClass().add("btn-file");
        selectBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Audio File");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a", "*.ogg", "*.flac", "*.webm"));
            File file = fc.showOpenDialog(getScene().getWindow());
            if (file != null) {
                audioFile = file;
                audioPath = file.getAbsolutePath();
                buildUI();
            }
        });

        uploadZone.getChildren().addAll(iconLabel, h3, desc, selectBtn);
        if (audioFile != null) {
            Button nextBtn = new Button("Continue ▶");
            nextBtn.getStyleClass().addAll("btn", "btn-primary");
            nextBtn.setOnAction(e -> { step = 2; buildUI(); });
            uploadZone.getChildren().add(nextBtn);
        }
        getChildren().add(uploadZone);
    }

    private void buildStep2() {
        VBox content = new VBox(24);

        // Audio preview
        VBox audioCard = new VBox(16);
        audioCard.getStyleClass().addAll("card", "card-sm");
        audioCard.setPadding(new Insets(24));
        Label audioTitle = new Label("🎵 Audio Preview");
        audioTitle.setStyle("-fx-font-weight: 700;");
        Label audioInfo = new Label("File: " + (audioFile != null ? audioFile.getName() : "None"));
        audioInfo.setStyle("-fx-text-fill: #64748b;");
        audioCard.getChildren().addAll(audioTitle, audioInfo);

        if (audioFile != null) {
            HBox playerControls = new HBox(12);
            playerControls.setAlignment(Pos.CENTER_LEFT);
            Button playBtn = new Button("▶ Play");
            playBtn.getStyleClass().addAll("btn", "btn-primary");
            Button stopBtn = new Button("⏹ Stop");
            stopBtn.getStyleClass().addAll("btn", "btn-ghost");
            playBtn.setOnAction(e -> {
                try {
                    if (mediaPlayer != null) mediaPlayer.dispose();
                    Media media = new Media(audioFile.toURI().toString());
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.play();
                } catch (Exception ex) {
                    showAlert("Cannot play audio: " + ex.getMessage());
                }
            });
            stopBtn.setOnAction(e -> { if (mediaPlayer != null) mediaPlayer.stop(); });
            playerControls.getChildren().addAll(playBtn, stopBtn);
            audioCard.getChildren().add(playerControls);
        }

        // Transcript
        VBox transcriptCard = new VBox(16);
        transcriptCard.getStyleClass().addAll("card", "card-sm");
        transcriptCard.setPadding(new Insets(24));

        HBox transcriptHeader = new HBox();
        transcriptHeader.setAlignment(Pos.CENTER_LEFT);
        Label transcriptTitle = new Label("📝 Transcript");
        transcriptTitle.setStyle("-fx-font-weight: 700;");
        Region tSpacer = new Region();
        HBox.setHgrow(tSpacer, Priority.ALWAYS);
        Button transcribeBtn = new Button("✨ Auto-Transcribe");
        transcribeBtn.getStyleClass().add("btn-auto");
        transcribeBtn.setOnAction(e -> handleTranscribe(transcribeBtn));
        transcriptHeader.getChildren().addAll(transcriptTitle, tSpacer, transcribeBtn);

        TextArea transcriptArea = new TextArea(transcript);
        transcriptArea.setPromptText("Paste or auto-generate transcript...");
        transcriptArea.setPrefRowCount(8);
        transcriptArea.getStyleClass().add("transcript-area");
        transcriptArea.textProperty().addListener((obs, old, val) -> transcript = val);

        transcriptCard.getChildren().addAll(transcriptHeader, transcriptArea);

        // Exam configuration
        VBox examCard = new VBox(16);
        examCard.getStyleClass().addAll("card", "card-sm");
        examCard.setPadding(new Insets(24));

        Label examTitle = new Label("⚙ Exam Configuration");
        examTitle.setStyle("-fx-font-weight: 700;");

        Label moduleLabel = new Label("EXAM MODULE");
        moduleLabel.getStyleClass().add("form-label");

        HBox examTabs = new HBox(8);
        for (String key : EXAM_CONFIG.keySet()) {
            Button tab = new Button(key);
            tab.getStyleClass().add("exam-tab");
            if (key.equals(examType)) tab.getStyleClass().add("active");
            tab.setOnAction(e -> {
                examType = key;
                selectedTypes.clear();
                for (String[] t : EXAM_CONFIG.get(key)) selectedTypes.add(t[0]);
                buildUI();
            });
            examTabs.getChildren().add(tab);
        }

        Label typesLabel = new Label("QUESTION TYPES");
        typesLabel.getStyleClass().add("form-label");

        FlowPane typeGrid = new FlowPane(10, 10);
        String[][] types = EXAM_CONFIG.get(examType);
        for (String[] t : types) {
            CheckBox cb = new CheckBox(t[1]);
            cb.setSelected(selectedTypes.contains(t[0]));
            cb.getStyleClass().add("qtype-checkbox");
            cb.setOnAction(e -> {
                if (cb.isSelected()) { if (!selectedTypes.contains(t[0])) selectedTypes.add(t[0]); }
                else selectedTypes.remove(t[0]);
            });
            typeGrid.getChildren().add(cb);
        }

        Label countLabel = new Label("QUESTIONS PER TYPE");
        countLabel.getStyleClass().add("form-label");

        HBox countControl = new HBox(8);
        countControl.setAlignment(Pos.CENTER_LEFT);
        Button decBtn = new Button("-");
        decBtn.getStyleClass().add("count-btn");
        TextField countField = new TextField(String.valueOf(questionCount));
        countField.setPrefWidth(72);
        countField.setStyle("-fx-alignment: center; -fx-font-weight: 700; -fx-font-size: 16px;");
        countField.textProperty().addListener((obs, old, val) -> {
            try { int v = Integer.parseInt(val); if (v >= 5 && v <= 50) questionCount = v; } catch (NumberFormatException ignored) {}
        });
        Button incBtn = new Button("+");
        incBtn.getStyleClass().add("count-btn");
        Label totalHint = new Label(selectedTypes.size() + " types × " + questionCount + " = " + (selectedTypes.size() * questionCount) + " total");
        totalHint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        decBtn.setOnAction(e -> { questionCount = Math.max(5, questionCount - 5); countField.setText(String.valueOf(questionCount)); totalHint.setText(selectedTypes.size() + " types × " + questionCount + " = " + (selectedTypes.size() * questionCount) + " total"); });
        incBtn.setOnAction(e -> { questionCount = Math.min(50, questionCount + 5); countField.setText(String.valueOf(questionCount)); totalHint.setText(selectedTypes.size() + " types × " + questionCount + " = " + (selectedTypes.size() * questionCount) + " total"); });
        countControl.getChildren().addAll(decBtn, countField, incBtn, totalHint);

        examCard.getChildren().addAll(examTitle, moduleLabel, examTabs, typesLabel, typeGrid, countLabel, countControl);

        // Buttons
        HBox buttons = new HBox();
        buttons.setPadding(new Insets(16, 0, 0, 0));
        Button backBtn = new Button("◀ Back");
        backBtn.getStyleClass().addAll("btn", "btn-ghost");
        backBtn.setOnAction(e -> { step = 1; buildUI(); });
        Region bSpacer = new Region();
        HBox.setHgrow(bSpacer, Priority.ALWAYS);
        Button generateBtn = new Button("Generate " + (selectedTypes.size() * questionCount) + " Questions ⚡");
        generateBtn.getStyleClass().addAll("btn", "btn-primary", "btn-lg");
        generateBtn.setDisable(transcript.isEmpty() || selectedTypes.isEmpty());
        generateBtn.setOnAction(e -> handleGenerate(generateBtn));
        buttons.getChildren().addAll(backBtn, bSpacer, generateBtn);

        content.getChildren().addAll(audioCard, transcriptCard, examCard, buttons);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }

    private void buildStep3() {
        if (generatedQuestions == null || generatedQuestions.isEmpty()) return;

        boolean hasAudio = audioFile != null;

        // Fresh MediaPlayer for review
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.dispose(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (hasAudio) {
            try {
                Media media = new Media(audioFile.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setOnError(() -> System.err.println("MediaPlayer error: " + mediaPlayer.getError()));
            } catch (Exception ex) {
                System.err.println("Cannot load audio for review: " + ex.getMessage());
                mediaPlayer = null;
            }
        }

        // Test name + visibility
        VBox nameCard = new VBox(12);
        nameCard.getStyleClass().addAll("card", "card-sm");
        nameCard.setPadding(new Insets(24));
        Label nameLabel = new Label("TEST NAME");
        nameLabel.getStyleClass().add("form-label");
        TextField nameField = new TextField(bankName);
        nameField.setPromptText("e.g., IELTS Listening Practice 01");
        nameField.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");
        nameField.textProperty().addListener((obs, old, val) -> bankName = val);

        HBox visRow = new HBox(12);
        visRow.setAlignment(Pos.CENTER_LEFT);
        Label visLabel = new Label("VISIBILITY");
        visLabel.getStyleClass().add("form-label");
        Button visBtn = new Button(isPublic ? "🌐 Public" : "🔒 Private");
        visBtn.getStyleClass().addAll("btn", isPublic ? "btn-primary" : "btn-ghost");
        visBtn.setOnAction(e -> { isPublic = !isPublic; visBtn.setText(isPublic ? "🌐 Public" : "🔒 Private"); });
        visRow.getChildren().addAll(visLabel, visBtn);
        nameCard.getChildren().addAll(nameLabel, nameField, visRow);

        // LEFT: Transcript pane using TranscriptPane component
        if (transcriptPane != null) transcriptPane.dispose();
        transcriptPane = new TranscriptPane(transcript, mediaPlayer);
        transcriptPane.setPrefWidth(420);
        transcriptPane.setMinWidth(320);

        // RIGHT: Questions panel with type filter
        VBox questionsBox = new VBox(14);

        Map<String, List<Question>> grouped = new LinkedHashMap<>();
        for (Question q : generatedQuestions) grouped.computeIfAbsent(q.getNormalizedType(), k -> new ArrayList<>()).add(q);

        // Type filter tabs
        if (grouped.size() > 1) {
            HBox filterTabs = new HBox(6);
            Button allTab = new Button("All (" + generatedQuestions.size() + ")");
            allTab.getStyleClass().add("filter-tab");
            if (activeTypeFilter == null) allTab.getStyleClass().add("active");
            allTab.setOnAction(e -> { activeTypeFilter = null; buildUI(); });
            filterTabs.getChildren().add(allTab);

            for (var entry : grouped.entrySet()) {
                Button tab = new Button(Question.getTypeLabel(entry.getKey()) + " (" + entry.getValue().size() + ")");
                tab.getStyleClass().add("filter-tab");
                if (entry.getKey().equals(activeTypeFilter)) tab.getStyleClass().add("active");
                String type = entry.getKey();
                tab.setOnAction(e -> { activeTypeFilter = type; buildUI(); });
                filterTabs.getChildren().add(tab);
            }
            questionsBox.getChildren().add(filterTabs);
        }

        Label reviewTitle = new Label("Review Questions (" + generatedQuestions.size() + " total)");
        reviewTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");
        questionsBox.getChildren().add(reviewTitle);

        Map<String, List<Question>> displayGroups;
        if (activeTypeFilter != null && grouped.containsKey(activeTypeFilter)) {
            displayGroups = new LinkedHashMap<>();
            displayGroups.put(activeTypeFilter, grouped.get(activeTypeFilter));
        } else {
            displayGroups = grouped;
        }

        for (var entry : displayGroups.entrySet()) {
            String type = entry.getKey();
            List<Question> qs = entry.getValue();

            Label typeLabel = new Label(Question.getTypeLabel(type) + " — " + qs.size() + " questions");
            typeLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #64748b; -fx-padding: 12 0;");
            questionsBox.getChildren().add(typeLabel);

            for (Question q : qs) {
                VBox qCard = buildReviewCard(q, hasAudio);
                questionsBox.getChildren().add(qCard);
            }
        }

        ScrollPane qScroll = new ScrollPane(questionsBox);
        qScroll.setFitToWidth(true);
        qScroll.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(qScroll, Priority.ALWAYS);

        HBox grid = new HBox(24);
        grid.setPadding(new Insets(8, 0, 0, 0));
        grid.getChildren().addAll(transcriptPane, qScroll);
        VBox.setVgrow(grid, Priority.ALWAYS);

        // Top action bar (always visible above the grid)
        HBox topActions = new HBox(12);
        topActions.setPadding(new Insets(0, 0, 12, 0));
        topActions.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("◀ Back to Configure");
        backBtn.getStyleClass().addAll("btn", "btn-ghost");
        backBtn.setOnAction(e -> { step = 2; activeTypeFilter = null; buildUI(); });

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Label unsavedWarning = new Label("⚠ Chưa lưu — nhớ nhấn Save trước khi rời trang!");
        unsavedWarning.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #d97706;");

        Button topSaveBtn = new Button("💾 Save to Question Bank");
        topSaveBtn.getStyleClass().addAll("btn", "btn-primary", "btn-lg");
        topSaveBtn.setOnAction(e -> handleSave(topSaveBtn));

        topActions.getChildren().addAll(backBtn, topSpacer, unsavedWarning, topSaveBtn);

        // Bottom save bar
        HBox bottomButtons = new HBox();
        bottomButtons.setPadding(new Insets(16, 0, 0, 0));
        Region bSpacer = new Region();
        HBox.setHgrow(bSpacer, Priority.ALWAYS);
        Button bottomSaveBtn = new Button("💾 Save to Question Bank");
        bottomSaveBtn.getStyleClass().addAll("btn", "btn-primary", "btn-lg");
        bottomSaveBtn.setOnAction(e -> handleSave(bottomSaveBtn));
        bottomButtons.getChildren().addAll(bSpacer, bottomSaveBtn);

        getChildren().addAll(nameCard, topActions, grid, bottomButtons);
    }

    private VBox buildReviewCard(Question q, boolean hasAudio) {
        VBox qCard = new VBox(10);
        qCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 2;");
        qCard.setPadding(new Insets(16));

        HBox qHeader = new HBox(8);
        qHeader.setAlignment(Pos.CENTER_LEFT);
        Label numBadge = new Label(String.valueOf(q.getQuestionNumber()));
        numBadge.setStyle("-fx-min-width: 28; -fx-min-height: 28; -fx-alignment: center; -fx-background-color: #1e3fae; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700;");
        Label typeBadge = new Label(Question.getTypeLabel(q.getNormalizedType()));
        typeBadge.getStyleClass().addAll("badge", "badge-primary");
        qHeader.getChildren().addAll(numBadge, typeBadge);

        Label qTextLabel = new Label(q.getQuestionText());
        qTextLabel.setWrapText(true);
        qTextLabel.setStyle("-fx-font-weight: 600;");

        qCard.getChildren().addAll(qHeader, qTextLabel);

        String t = q.getNormalizedType();
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

        // Answer
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

        // Transcript quote
        String quote = q.getTranscriptQuote();
        if (quote != null && !quote.isEmpty()) {
            VBox quoteBox = new VBox(4);
            quoteBox.setStyle("-fx-padding: 10 14; -fx-background-color: linear-gradient(to bottom right, #fefce8, #fff7ed); -fx-background-radius: 8; -fx-border-color: #fde68a; -fx-border-radius: 8;");
            quoteBox.setCursor(javafx.scene.Cursor.HAND);

            HBox quoteHeader = new HBox(6);
            quoteHeader.setAlignment(Pos.CENTER_LEFT);
            Label quoteLbl = new Label("📎 Transcript passage");
            quoteLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #92400e;");
            Region qs = new Region();
            HBox.setHgrow(qs, Priority.ALWAYS);
            quoteHeader.getChildren().add(quoteLbl);
            quoteHeader.getChildren().add(qs);
            if (hasAudio) {
                Label playLbl = new Label("▶ Play");
                playLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1e3fae; -fx-padding: 2 10; -fx-background-color: rgba(30,63,174,0.1); -fx-background-radius: 99;");
                quoteHeader.getChildren().add(playLbl);
            }

            Label quoteText = new Label("\"" + quote + "\"");
            quoteText.setWrapText(true);
            quoteText.setStyle("-fx-font-size: 13px; -fx-text-fill: #78350f; -fx-font-style: italic;");

            quoteBox.getChildren().addAll(quoteHeader, quoteText);
            quoteBox.setOnMouseClicked(e -> {
                if (transcriptPane != null) transcriptPane.highlightQuoteAndPlay(quote);
            });

            qCard.getChildren().add(quoteBox);
        }

        // Explanation
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

    private void handleTranscribe(Button btn) {
        if (audioFile == null) { showAlert("Please select an audio file first."); return; }
        if (!GeminiService.getInstance().isConfigured()) { showAlert("GEMINI_API_KEY is not configured."); return; }

        btn.setText("Transcribing...");
        btn.setDisable(true);

        new Thread(() -> {
            try {
                String result = GeminiService.getInstance().transcribeAudio(audioFile);
                Platform.runLater(() -> {
                    transcript = result;
                    btn.setText("✨ Auto-Transcribe");
                    btn.setDisable(false);
                    buildUI();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Transcribe failed: " + e.getMessage());
                    btn.setText("✨ Auto-Transcribe");
                    btn.setDisable(false);
                });
            }
        }).start();
    }

    private void handleGenerate(Button btn) {
        if (transcript.isEmpty()) { showAlert("Please add a transcript first."); return; }
        if (selectedTypes.isEmpty()) { showAlert("Please select at least one question type."); return; }

        btn.setText("Generating...");
        btn.setDisable(true);

        new Thread(() -> {
            try {
                List<Question> questions = GeminiService.getInstance().generateAllQuestions(
                        transcript, examType, selectedTypes, questionCount, customInstructions);
                Platform.runLater(() -> {
                    generatedQuestions = questions;
                    step = 3;
                    activeTypeFilter = null;
                    buildUI();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Generate failed: " + e.getMessage());
                    btn.setText("Generate " + (selectedTypes.size() * questionCount) + " Questions ⚡");
                    btn.setDisable(false);
                });
            }
        }).start();
    }

    private void handleSave(Button btn) {
        if (generatedQuestions == null || generatedQuestions.isEmpty()) return;
        if (audioFile == null) { showAlert("Please select an audio file."); return; }

        String name = bankName.isEmpty() ? examType + " Test - " + java.time.LocalDate.now() : bankName;

        Section section = new Section();
        section.setSectionNumber(1);
        section.setInstruction(examType + " Listening Comprehension — " + generatedQuestions.size() + " Questions");
        section.setQuestions(generatedQuestions);

        btn.setText("Saving...");
        btn.setDisable(true);

        int bankId = DatabaseManager.getInstance().saveBank(name, user.getId(), examType,
                audioFile.toURI().toString(), transcript, 0, 0, List.of(section));

        if (bankId > 0) {
            showAlert("Test saved successfully!");
            step = 1;
            audioFile = null;
            audioPath = null;
            transcript = "";
            generatedQuestions = null;
            bankName = "";
            activeTypeFilter = null;
            buildUI();
        } else {
            showAlert("Save failed.");
            btn.setText("Save to Question Bank 💾");
            btn.setDisable(false);
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Returns true if user has generated questions that haven't been saved yet.
     */
    public boolean hasUnsavedData() {
        return step == 3 && generatedQuestions != null && !generatedQuestions.isEmpty();
    }

    /**
     * Attempt to save current data. Returns true if saved successfully.
     */
    public boolean trySave() {
        if (!hasUnsavedData() || audioFile == null) return false;

        String name = bankName.isEmpty() ? examType + " Test - " + java.time.LocalDate.now() : bankName;

        Section section = new Section();
        section.setSectionNumber(1);
        section.setInstruction(examType + " Listening Comprehension — " + generatedQuestions.size() + " Questions");
        section.setQuestions(generatedQuestions);

        int bankId = DatabaseManager.getInstance().saveBank(name, user.getId(), examType,
                audioFile.toURI().toString(), transcript, 0, 0, List.of(section));

        if (bankId > 0) {
            generatedQuestions = null;
            return true;
        }
        return false;
    }

    public void dispose() {
        if (transcriptPane != null) transcriptPane.dispose();
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.dispose(); } catch (Exception ignored) {}
        }
    }
}
