package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.*;
import com.akatsuki.service.GeminiService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.*;

public class PracticeView extends VBox {
    private final User user;
    private int practiceStep = 1;
    private File audioFile;
    private String transcript = "";
    private String examType = "IELTS";
    private List<String> selectedTypes = new ArrayList<>(List.of("mcq", "true_false"));
    private int questionsPerType = 5;
    private int timerMinutes = 10;
    private List<Question> questions = new ArrayList<>();
    private Map<Integer, String> answers = new HashMap<>();
    private int timeLeft = -1;
    private Timeline timerTimeline;
    private MediaPlayer mediaPlayer;
    private VBox contentBox;
    private FlowPane navigatorGrid;
    private TranscriptPane transcriptPane;
    private String activeTypeFilter = null;

    private static final Map<String, String[][]> EXAM_CONFIG = new LinkedHashMap<>();
    static {
        EXAM_CONFIG.put("IELTS", new String[][]{{"mcq","Multiple Choice"},{"table_completion","Table Completion"},{"note_completion","Note Completion"},{"sentence_completion","Sentence Completion"},{"matching","Matching"},{"true_false_ng","T/F/Not Given"},{"short_answer","Short Answer"}});
        EXAM_CONFIG.put("TOEIC", new String[][]{{"mcq","Multiple Choice"},{"true_false","True/False"},{"fill_blank","Fill in Blank"},{"table_completion","Table Completion"},{"short_answer","Short Answer"}});
        EXAM_CONFIG.put("VSTEP", new String[][]{{"mcq","Multiple Choice"},{"true_false","True/False"},{"fill_blank","Fill in Blank"},{"table_completion","Table Completion"},{"note_completion","Note Completion"},{"short_answer","Short Answer"}});
        EXAM_CONFIG.put("GENERAL", new String[][]{{"mcq","Multiple Choice"},{"true_false","True/False"},{"fill_blank","Fill in Blank"},{"table_completion","Table Completion"},{"note_completion","Note Completion"},{"sentence_completion","Sentence Completion"},{"short_answer","Short Answer"},{"matching","Matching"}});
    }

    public PracticeView(User user) {
        this.user = user;
        setSpacing(0);
        contentBox = new VBox();
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        getChildren().add(contentBox);
        buildUI();
    }

    private void buildUI() {
        contentBox.getChildren().clear();
        switch (practiceStep) {
            case 1 -> buildStep1();
            case 2 -> buildStep2();
            case 3 -> buildTestUI();
            case 4 -> buildResultsUI();
        }
    }

    private void buildStep1() {
        VBox header = new VBox(8);
        header.setPadding(new Insets(0, 0, 32, 0));
        Label title = new Label("🏋 Self-Study Practice");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 700;");
        Label subtitle = new Label("Upload an audio file and the system will generate a practice test for you.");
        subtitle.setStyle("-fx-text-fill: #64748b;");
        header.getChildren().addAll(title, subtitle);

        VBox uploadZone = new VBox(16);
        uploadZone.getStyleClass().add("upload-zone");
        uploadZone.setAlignment(Pos.CENTER);
        uploadZone.setPadding(new Insets(48));

        Label iconLabel = new Label("☁");
        iconLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: #1e3fae;");
        Label h3 = new Label("Upload Audio File");
        h3.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");

        Button selectBtn = new Button(audioFile != null ? audioFile.getName() : "Select File");
        selectBtn.getStyleClass().add("btn-file");
        selectBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Audio File");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a", "*.ogg", "*.flac"));
            File file = fc.showOpenDialog(getScene().getWindow());
            if (file != null) { audioFile = file; buildUI(); }
        });

        uploadZone.getChildren().addAll(iconLabel, h3, selectBtn);
        if (audioFile != null) {
            Button nextBtn = new Button("Continue ▶");
            nextBtn.getStyleClass().addAll("btn", "btn-primary");
            nextBtn.setOnAction(e -> { practiceStep = 2; buildUI(); });
            uploadZone.getChildren().add(nextBtn);
        }

        contentBox.getChildren().addAll(header, uploadZone);
    }

    private void buildStep2() {
        VBox content = new VBox(24);

        VBox audioCard = new VBox(12);
        audioCard.getStyleClass().addAll("card", "card-sm");
        audioCard.setPadding(new Insets(24));
        Label audioTitle = new Label("🎵 Audio Preview: " + audioFile.getName());
        audioTitle.setStyle("-fx-font-weight: 700;");
        HBox playerControls = new HBox(12);
        Button playBtn = new Button("▶ Play");
        playBtn.getStyleClass().addAll("btn", "btn-primary");
        playBtn.setOnAction(e -> {
            try {
                if (mediaPlayer != null) mediaPlayer.dispose();
                mediaPlayer = new MediaPlayer(new Media(audioFile.toURI().toString()));
                mediaPlayer.play();
            } catch (Exception ex) { showAlert("Cannot play: " + ex.getMessage()); }
        });
        Button stopBtn = new Button("⏹ Stop");
        stopBtn.getStyleClass().addAll("btn", "btn-ghost");
        stopBtn.setOnAction(e -> { if (mediaPlayer != null) mediaPlayer.stop(); });
        playerControls.getChildren().addAll(playBtn, stopBtn);
        audioCard.getChildren().addAll(audioTitle, playerControls);

        VBox tCard = new VBox(12);
        tCard.getStyleClass().addAll("card", "card-sm");
        tCard.setPadding(new Insets(24));
        HBox tHeader = new HBox();
        tHeader.setAlignment(Pos.CENTER_LEFT);
        Label tTitle = new Label("📝 Transcript");
        tTitle.setStyle("-fx-font-weight: 700;");
        Region tSpacer = new Region();
        HBox.setHgrow(tSpacer, Priority.ALWAYS);
        Button transcribeBtn = new Button("✨ Auto-Transcribe");
        transcribeBtn.getStyleClass().add("btn-auto");
        transcribeBtn.setOnAction(e -> {
            transcribeBtn.setText("Transcribing...");
            transcribeBtn.setDisable(true);
            new Thread(() -> {
                try {
                    String result = GeminiService.getInstance().transcribeAudio(audioFile);
                    Platform.runLater(() -> { transcript = result; transcribeBtn.setText("✨ Auto-Transcribe"); transcribeBtn.setDisable(false); buildUI(); });
                } catch (Exception ex) {
                    Platform.runLater(() -> { showAlert("Failed: " + ex.getMessage()); transcribeBtn.setText("✨ Auto-Transcribe"); transcribeBtn.setDisable(false); });
                }
            }).start();
        });
        tHeader.getChildren().addAll(tTitle, tSpacer, transcribeBtn);
        TextArea tArea = new TextArea(transcript);
        tArea.setPromptText("Paste or auto-generate...");
        tArea.setPrefRowCount(6);
        tArea.textProperty().addListener((obs, old, val) -> transcript = val);
        tCard.getChildren().addAll(tHeader, tArea);

        VBox cfgCard = new VBox(16);
        cfgCard.getStyleClass().addAll("card", "card-sm");
        cfgCard.setPadding(new Insets(24));
        Label cfgTitle = new Label("⚙ Practice Configuration");
        cfgTitle.setStyle("-fx-font-weight: 700;");

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

        FlowPane typeGrid = new FlowPane(10, 10);
        for (String[] t : EXAM_CONFIG.get(examType)) {
            CheckBox cb = new CheckBox(t[1]);
            cb.setSelected(selectedTypes.contains(t[0]));
            cb.setOnAction(e -> { if (cb.isSelected()) { if (!selectedTypes.contains(t[0])) selectedTypes.add(t[0]); } else selectedTypes.remove(t[0]); });
            typeGrid.getChildren().add(cb);
        }

        HBox countAndTimer = new HBox(24);
        VBox countBox = new VBox(8);
        Label countLabel = new Label("QUESTIONS PER TYPE");
        countLabel.getStyleClass().add("form-label");
        Spinner<Integer> countSpinner = new Spinner<>(3, 50, questionsPerType);
        countSpinner.valueProperty().addListener((obs, old, val) -> questionsPerType = val);
        countBox.getChildren().addAll(countLabel, countSpinner);

        VBox timerBox = new VBox(8);
        Label timerLabel = new Label("TIMER (MINUTES)");
        timerLabel.getStyleClass().add("form-label");
        Spinner<Integer> timerSpinner = new Spinner<>(1, 120, timerMinutes);
        timerSpinner.valueProperty().addListener((obs, old, val) -> timerMinutes = val);
        timerBox.getChildren().addAll(timerLabel, timerSpinner);
        countAndTimer.getChildren().addAll(countBox, timerBox);

        cfgCard.getChildren().addAll(cfgTitle, new Label("EXAM MODULE"), examTabs, new Label("QUESTION TYPES"), typeGrid, countAndTimer);

        HBox buttons = new HBox();
        Button backBtn = new Button("◀ Back");
        backBtn.getStyleClass().addAll("btn", "btn-ghost");
        backBtn.setOnAction(e -> { practiceStep = 1; buildUI(); });
        Region bSpacer = new Region();
        HBox.setHgrow(bSpacer, Priority.ALWAYS);
        int total = selectedTypes.size() * questionsPerType;
        Button genBtn = new Button("Start Practice (" + total + " Qs) ⚡");
        genBtn.getStyleClass().addAll("btn", "btn-primary", "btn-lg");
        genBtn.setDisable(transcript.isEmpty() || selectedTypes.isEmpty());
        genBtn.setOnAction(e -> handleGenerate(genBtn));
        buttons.getChildren().addAll(backBtn, bSpacer, genBtn);

        content.getChildren().addAll(audioCard, tCard, cfgCard, buttons);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        contentBox.getChildren().add(sp);
    }

    private void buildTestUI() {
        VBox leftCol = new VBox(16);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 16, 0));
        VBox tBox = new VBox(4);
        Label tag = new Label("🏋 Practice Session");
        tag.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #059669;");
        Label title = new Label("Self-Study Practice");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 700;");
        tBox.getChildren().addAll(tag, title);
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label timerDisplay = new Label(timeLeft >= 0 ? formatTime(timeLeft) : "--:--");
        timerDisplay.setId("timer-display");
        timerDisplay.setStyle("-fx-font-size: 24px; -fx-font-weight: 700;");
        Button startTimerBtn = new Button("Start Timer");
        startTimerBtn.getStyleClass().addAll("btn", "btn-ghost");
        startTimerBtn.setVisible(timeLeft < 0);
        startTimerBtn.setOnAction(e -> { timeLeft = timerMinutes * 60; startTimer(); startTimerBtn.setVisible(false); });
        HBox timerBox = new HBox(12, timerDisplay, startTimerBtn);
        timerBox.setAlignment(Pos.CENTER_RIGHT);
        header.getChildren().addAll(tBox, hSpacer, timerBox);

        // Audio player
        if (audioFile != null) {
            HBox audioPlayer = new HBox(12);
            audioPlayer.setStyle("-fx-padding: 14; -fx-background-color: white; -fx-background-radius: 12;");
            audioPlayer.setAlignment(Pos.CENTER_LEFT);
            Button playBtn = new Button("▶");
            playBtn.setStyle("-fx-font-size: 20px; -fx-min-width: 44; -fx-min-height: 44; -fx-background-color: #1e3fae; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
            playBtn.setOnAction(e -> {
                try {
                    if (mediaPlayer == null) mediaPlayer = new MediaPlayer(new Media(audioFile.toURI().toString()));
                    if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) { mediaPlayer.pause(); playBtn.setText("▶"); }
                    else { mediaPlayer.play(); playBtn.setText("⏸"); }
                } catch (Exception ex) { showAlert("Cannot play: " + ex.getMessage()); }
            });

            Slider progressSlider = new Slider(0, 100, 0);
            HBox.setHgrow(progressSlider, Priority.ALWAYS);
            Label audioTimeLabel = new Label("0:00");
            audioTimeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748b; -fx-min-width: 80;");

            if (mediaPlayer != null) {
                mediaPlayer.currentTimeProperty().addListener((obs, ov, nv) -> {
                    if (nv != null) Platform.runLater(() -> {
                        double sec = nv.toSeconds();
                        int m = (int)(sec/60), s = (int)(sec%60);
                        String tot = "";
                        if (mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()) {
                            double ts = mediaPlayer.getTotalDuration().toSeconds();
                            tot = String.format(" / %d:%02d", (int)(ts/60), (int)(ts%60));
                        }
                        audioTimeLabel.setText(String.format("%d:%02d%s", m, s, tot));
                        if (mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown())
                            progressSlider.setValue((sec / mediaPlayer.getTotalDuration().toSeconds()) * 100);
                    });
                });
                progressSlider.setOnMouseReleased(ev -> {
                    if (mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown())
                        mediaPlayer.seek(Duration.millis((progressSlider.getValue()/100)*mediaPlayer.getTotalDuration().toMillis()));
                });
            }

            Label aLabel = new Label(audioFile.getName());
            aLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px;");
            audioPlayer.getChildren().addAll(playBtn, aLabel, progressSlider, audioTimeLabel);
            leftCol.getChildren().add(audioPlayer);
        }

        // Type filter
        Map<String, List<Question>> grouped = new LinkedHashMap<>();
        for (Question q : questions) grouped.computeIfAbsent(q.getNormalizedType(), k -> new ArrayList<>()).add(q);

        if (grouped.size() > 1) {
            HBox filterTabs = new HBox(6);
            Button allTab = new Button("All (" + questions.size() + ")");
            allTab.getStyleClass().add("filter-tab");
            if (activeTypeFilter == null) allTab.getStyleClass().add("active");
            allTab.setOnAction(e -> { activeTypeFilter = null; buildUI(); });
            filterTabs.getChildren().add(allTab);

            for (var entry : grouped.entrySet()) {
                Button ftab = new Button(Question.getTypeLabel(entry.getKey()) + " (" + entry.getValue().size() + ")");
                ftab.getStyleClass().add("filter-tab");
                if (entry.getKey().equals(activeTypeFilter)) ftab.getStyleClass().add("active");
                String type = entry.getKey();
                ftab.setOnAction(e -> { activeTypeFilter = type; buildUI(); });
                filterTabs.getChildren().add(ftab);
            }
            leftCol.getChildren().add(filterTabs);
        }

        // Questions
        Map<String, List<Question>> displayGroups = new LinkedHashMap<>();
        if (activeTypeFilter != null && grouped.containsKey(activeTypeFilter)) {
            displayGroups.put(activeTypeFilter, grouped.get(activeTypeFilter));
        } else {
            displayGroups = grouped;
        }

        for (var entry : displayGroups.entrySet()) {
            VBox section = new VBox(14);
            section.setPadding(new Insets(24));
            section.setStyle("-fx-background-color: white; -fx-background-radius: 16;");
            Label sTitle = new Label(Question.getTypeLabel(entry.getKey()) + " — " + entry.getValue().size() + " Questions");
            sTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");
            section.getChildren().add(sTitle);

            for (Question q : entry.getValue()) {
                int idx = questions.indexOf(q);
                VBox qBlock = buildQuestionBlock(q, idx);
                section.getChildren().add(qBlock);
            }
            leftCol.getChildren().add(section);
        }

        ScrollPane leftScroll = new ScrollPane(leftCol);
        leftScroll.setFitToWidth(true);
        leftScroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(leftScroll, Priority.ALWAYS);

        // Navigator (sticky, outside scroll)
        VBox navCard = new VBox(14);
        navCard.setPrefWidth(220);
        navCard.setMinWidth(220);
        navCard.setMaxWidth(220);
        navCard.setPadding(new Insets(20));
        navCard.setStyle("-fx-background-color: white; -fx-background-radius: 16;");

        Label navTitle = new Label("📋 Navigator");
        navTitle.setStyle("-fx-font-weight: 700;");
        navigatorGrid = new FlowPane(6, 6);
        rebuildNavigatorDots();

        Button submitBtn = new Button("Submit Practice");
        submitBtn.getStyleClass().addAll("btn", "btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleSubmit());

        navCard.getChildren().addAll(navTitle, navigatorGrid, submitBtn);

        HBox mainLayout = new HBox(24, leftScroll, navCard);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        VBox wrapper = new VBox(0, header, mainLayout);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        contentBox.getChildren().add(wrapper);
    }

    private void rebuildNavigatorDots() {
        navigatorGrid.getChildren().clear();
        for (int i = 0; i < questions.size(); i++) {
            Label dot = new Label(String.valueOf(i + 1));
            dot.getStyleClass().add("q-dot");
            if (answers.containsKey(i)) dot.getStyleClass().add("answered");
            dot.setPrefWidth(32);
            dot.setPrefHeight(32);
            dot.setAlignment(Pos.CENTER);
            navigatorGrid.getChildren().add(dot);
        }
    }

    private void updateNavigator() {
        if (navigatorGrid == null) return;
        for (int i = 0; i < questions.size() && i < navigatorGrid.getChildren().size(); i++) {
            Label dot = (Label) navigatorGrid.getChildren().get(i);
            boolean wasAnswered = dot.getStyleClass().contains("answered");
            boolean isAnswered = answers.containsKey(i);
            if (isAnswered && !wasAnswered) dot.getStyleClass().add("answered");
            else if (!isAnswered && wasAnswered) dot.getStyleClass().remove("answered");
        }
    }

    private VBox buildQuestionBlock(Question q, int idx) {
        VBox block = new VBox(10);
        block.setPadding(new Insets(0, 0, 16, 0));

        HBox label = new HBox(10);
        Label num = new Label(String.valueOf(q.getQuestionNumber()));
        num.setStyle("-fx-min-width: 28; -fx-min-height: 28; -fx-alignment: center; -fx-background-color: #f1f5f9; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        Label qText = new Label(q.getQuestionText());
        qText.setWrapText(true);
        qText.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #334155;");
        HBox.setHgrow(qText, Priority.ALWAYS);
        label.getChildren().addAll(num, qText);
        block.getChildren().add(label);

        String t = q.getNormalizedType();
        String sel = answers.getOrDefault(idx, "");

        if (("mcq".equals(t) || "matching".equals(t)) && !q.getOptions().isEmpty()) {
            VBox optsBox = new VBox(6);
            optsBox.setPadding(new Insets(0, 0, 0, 38));
            ToggleGroup group = new ToggleGroup();
            for (String opt : q.getOptions()) {
                String letter = opt.isEmpty() ? "" : String.valueOf(opt.charAt(0));
                RadioButton rb = new RadioButton(opt);
                rb.setToggleGroup(group);
                rb.setStyle("-fx-padding: 8;");
                if (letter.equals(sel)) rb.setSelected(true);
                rb.setOnAction(e -> { answers.put(idx, letter); updateNavigator(); });
                optsBox.getChildren().add(rb);
            }
            block.getChildren().add(optsBox);
        } else if ("true_false".equals(t) || "true_false_ng".equals(t)) {
            HBox tfBox = new HBox(8);
            tfBox.setPadding(new Insets(0, 0, 0, 38));
            ToggleGroup group = new ToggleGroup();
            String[] opts = "true_false_ng".equals(t) ? new String[]{"True", "False", "Not Given"} : new String[]{"True", "False"};
            for (String opt : opts) {
                RadioButton rb = new RadioButton(opt);
                rb.setToggleGroup(group);
                rb.setStyle("-fx-font-weight: 700; -fx-padding: 8 16;");
                if (opt.equals(sel)) rb.setSelected(true);
                rb.setOnAction(e -> { answers.put(idx, opt); updateNavigator(); });
                tfBox.getChildren().add(rb);
            }
            block.getChildren().add(tfBox);
        } else {
            TextField input = new TextField(sel);
            input.setPromptText("Type your answer...");
            input.setPrefWidth(350);
            input.getStyleClass().add("q-input");
            input.textProperty().addListener((obs, old, val) -> {
                if (val != null && !val.isEmpty()) answers.put(idx, val);
                else answers.remove(idx);
                updateNavigator();
            });
            HBox inputBox = new HBox(input);
            inputBox.setPadding(new Insets(0, 0, 0, 38));
            block.getChildren().add(inputBox);
        }

        return block;
    }

    private void buildResultsUI() {
        int correct = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (Question.isAnswerCorrect(answers.get(i), questions.get(i).getCorrectAnswer(), questions.get(i).getQuestionType()))
                correct++;
        }
        double score = questions.isEmpty() ? 0 : (double) correct / questions.size() * 10;
        int pct = questions.isEmpty() ? 0 : Math.round((float) correct / questions.size() * 100);
        final int correctCount = correct;

        // Re-init audio for replay
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.dispose(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (audioFile != null) {
            try {
                mediaPlayer = new MediaPlayer(new Media(audioFile.toURI().toString()));
            } catch (Exception ignored) {}
        }

        // Header with score
        HBox header = new HBox();
        header.setPadding(new Insets(0, 0, 16, 0));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(6);
        Label tagLbl = new Label("✅ Practice Complete");
        tagLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #059669;");
        Label titleLbl = new Label("Review Results");
        titleLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: 700;");
        titleBox.getChildren().addAll(tagLbl, titleLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(Pos.CENTER);
        Label pctLabel = new Label(pct + "%");
        pctLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: " + (pct >= 70 ? "#047857" : pct >= 40 ? "#b45309" : "#b91c1c") + ";");
        Label scoreText = new Label(String.format("%.1f/10.0 • %d/%d correct", score, correctCount, questions.size()));
        scoreText.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        scoreBox.getChildren().addAll(pctLabel, scoreText);

        Button restartBtn = new Button("🔄 New Practice");
        restartBtn.getStyleClass().addAll("btn", "btn-ghost");
        restartBtn.setOnAction(e -> {
            practiceStep = 1; audioFile = null; transcript = ""; questions.clear(); answers.clear(); timeLeft = -1; activeTypeFilter = null;
            if (timerTimeline != null) timerTimeline.stop();
            if (transcriptPane != null) transcriptPane.dispose();
            if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); mediaPlayer = null; }
            buildUI();
        });

        HBox rightBox = new HBox(20, scoreBox, restartBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        header.getChildren().addAll(titleBox, spacer, rightBox);

        // LEFT: Transcript pane (30%)
        if (transcriptPane != null) transcriptPane.dispose();
        transcriptPane = new TranscriptPane(transcript, mediaPlayer);
        transcriptPane.setPrefWidth(380);
        transcriptPane.setMinWidth(320);

        // RIGHT: Question analysis with type filter
        VBox analysisPanel = new VBox(14);

        Map<String, List<Question>> grouped = new LinkedHashMap<>();
        for (Question q : questions) grouped.computeIfAbsent(q.getNormalizedType(), k -> new ArrayList<>()).add(q);

        if (grouped.size() > 1) {
            HBox filterTabs = new HBox(6);
            Button allTab = new Button("All (" + questions.size() + ")");
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
            analysisPanel.getChildren().add(filterTabs);
        }

        List<Question> displayQuestions;
        if (activeTypeFilter != null && grouped.containsKey(activeTypeFilter)) {
            displayQuestions = grouped.get(activeTypeFilter);
        } else {
            displayQuestions = questions;
        }

        for (Question q : displayQuestions) {
            int i = questions.indexOf(q);
            boolean isCorrect = Question.isAnswerCorrect(answers.get(i), q.getCorrectAnswer(), q.getQuestionType());
            String userAns = answers.getOrDefault(i, "");
            String t = q.getNormalizedType();

            VBox card = new VBox(10);
            card.setPadding(new Insets(16));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-width: 2; -fx-border-radius: 12; -fx-border-color: " + (isCorrect ? "#d1fae5" : "#fee2e2") + ";");

            HBox cHeader = new HBox(8);
            cHeader.setAlignment(Pos.CENTER_LEFT);
            Label numL = new Label(String.valueOf(q.getQuestionNumber()));
            numL.setStyle("-fx-min-width: 28; -fx-min-height: 28; -fx-alignment: center; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: " + (isCorrect ? "#059669" : "#ef4444") + ";");
            Label statusL = new Label(isCorrect ? "Correct" : "Incorrect");
            statusL.setStyle("-fx-font-weight: 700; -fx-text-fill: " + (isCorrect ? "#047857" : "#b91c1c") + ";");
            Label tBadge = new Label(Question.getTypeLabel(t));
            tBadge.getStyleClass().addAll("badge", "badge-slate");
            cHeader.getChildren().addAll(numL, statusL, tBadge);

            Label qTextL = new Label(q.getQuestionText());
            qTextL.setWrapText(true);
            qTextL.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");
            card.getChildren().addAll(cHeader, qTextL);

            // Show all options with highlighting
            if (("mcq".equals(t) || "matching".equals(t)) && q.getOptions() != null && !q.getOptions().isEmpty()) {
                VBox optsBox = new VBox(6);
                for (String opt : q.getOptions()) {
                    String letter = opt.isEmpty() ? "" : String.valueOf(opt.charAt(0));
                    boolean isUserChoice = letter.equalsIgnoreCase(userAns);
                    boolean isCorrectChoice = letter.equalsIgnoreCase(q.getCorrectAnswer());

                    String bgColor = "transparent";
                    String textColor = "#475569";
                    String borderColor = "#f1f5f9";
                    if (isCorrectChoice) { bgColor = "#ecfdf5"; textColor = "#047857"; borderColor = "#86efac"; }
                    if (isUserChoice && !isCorrect) { bgColor = "#fef2f2"; textColor = "#b91c1c"; borderColor = "#fca5a5"; }

                    HBox optRow = new HBox(8);
                    optRow.setPadding(new Insets(8, 12, 8, 12));
                    optRow.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-border-width: 1;");
                    Label optLabel = new Label(opt + (isCorrectChoice ? " ✓" : "") + (isUserChoice && !isCorrect ? " ✗" : ""));
                    optLabel.setWrapText(true);
                    optLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: " + ((isUserChoice || isCorrectChoice) ? "700" : "400") + "; -fx-text-fill: " + textColor + ";");
                    optRow.getChildren().add(optLabel);
                    optsBox.getChildren().add(optRow);
                }
                card.getChildren().add(optsBox);
            } else if ("true_false".equals(t) || "true_false_ng".equals(t)) {
                String[] tfOpts = "true_false_ng".equals(t) ? new String[]{"True", "False", "Not Given"} : new String[]{"True", "False"};
                HBox tfBox = new HBox(8);
                for (String opt : tfOpts) {
                    boolean isUserChoice = opt.equalsIgnoreCase(userAns);
                    boolean isCorrectChoice = opt.equalsIgnoreCase(q.getCorrectAnswer());
                    String bgColor = "#f8fafc";
                    String textColor = "#64748b";
                    if (isCorrectChoice) { bgColor = "#ecfdf5"; textColor = "#047857"; }
                    if (isUserChoice && !isCorrect) { bgColor = "#fef2f2"; textColor = "#b91c1c"; }
                    Label tfLabel = new Label(opt + (isCorrectChoice ? " ✓" : "") + (isUserChoice && !isCorrect ? " ✗" : ""));
                    tfLabel.setStyle("-fx-padding: 8 16; -fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-font-weight: 700; -fx-text-fill: " + textColor + ";");
                    tfBox.getChildren().add(tfLabel);
                }
                card.getChildren().add(tfBox);
            } else {
                String displayUser = userAns.isEmpty() ? "(No answer)" : userAns;
                Label yourAns = new Label("Your answer: " + displayUser);
                yourAns.setStyle("-fx-font-weight: 700; -fx-text-fill: " + (isCorrect ? "#047857" : "#b91c1c") + "; -fx-padding: 8; -fx-background-color: " + (isCorrect ? "#ecfdf5" : "#fef2f2") + "; -fx-background-radius: 8;");
                card.getChildren().add(yourAns);
                if (!isCorrect) {
                    Label correctAns = new Label("Correct: " + q.getCorrectAnswer());
                    correctAns.setStyle("-fx-font-weight: 700; -fx-text-fill: #047857; -fx-padding: 8; -fx-background-color: #ecfdf5; -fx-background-radius: 8;");
                    card.getChildren().add(correctAns);
                }
            }

            // Transcript quote
            String quote = q.getTranscriptQuote();
            if (quote != null && !quote.isEmpty()) {
                VBox quoteBox = new VBox(4);
                quoteBox.setStyle("-fx-padding: 10 14; -fx-background-color: linear-gradient(to bottom right, #fefce8, #fff7ed); -fx-background-radius: 8; -fx-border-color: #fde68a; -fx-border-radius: 8;");
                quoteBox.setCursor(javafx.scene.Cursor.HAND);
                Label quoteLbl = new Label("📎 \"" + quote + "\"");
                quoteLbl.setWrapText(true);
                quoteLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #78350f; -fx-font-style: italic;");
                quoteBox.getChildren().add(quoteLbl);
                quoteBox.setOnMouseClicked(e -> { if (transcriptPane != null) transcriptPane.highlightQuoteAndPlay(quote); });
                card.getChildren().add(quoteBox);
            }

            // Explanation
            if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
                Label expl = new Label("💡 " + q.getExplanation());
                expl.setWrapText(true);
                expl.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-padding: 8 0 0 0;");
                card.getChildren().add(expl);
            }

            analysisPanel.getChildren().add(card);
        }

        ScrollPane analysisScroll = new ScrollPane(analysisPanel);
        analysisScroll.setFitToWidth(true);
        analysisScroll.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(analysisScroll, Priority.ALWAYS);

        HBox grid = new HBox(20);
        grid.getChildren().addAll(transcriptPane, analysisScroll);
        VBox.setVgrow(grid, Priority.ALWAYS);

        VBox wrapper = new VBox(0, header, grid);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        contentBox.getChildren().add(wrapper);
    }

    private void handleGenerate(Button btn) {
        btn.setText("Generating...");
        btn.setDisable(true);

        new Thread(() -> {
            try {
                List<Question> result = GeminiService.getInstance().generateAllQuestions(
                        transcript, examType, selectedTypes, questionsPerType, null);
                for (int i = 0; i < result.size(); i++) result.get(i).setQuestionNumber(i + 1);
                Platform.runLater(() -> {
                    questions = result;
                    answers.clear();
                    timeLeft = -1;
                    activeTypeFilter = null;
                    practiceStep = 3;
                    buildUI();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Generate failed: " + e.getMessage());
                    btn.setText("Start Practice ⚡");
                    btn.setDisable(false);
                });
            }
        }).start();
    }

    private void handleSubmit() {
        int unanswered = questions.size() - answers.size();
        if (unanswered > 0) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "You have " + unanswered + " unanswered. Submit?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> r = confirm.showAndWait();
            if (r.isEmpty() || r.get() != ButtonType.YES) return;
        }

        if (timerTimeline != null) timerTimeline.stop();
        if (mediaPlayer != null) mediaPlayer.stop();

        int correct = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (Question.isAnswerCorrect(answers.get(i), questions.get(i).getCorrectAnswer(), questions.get(i).getQuestionType()))
                correct++;
        }
        double score = questions.isEmpty() ? 0 : (double) correct / questions.size() * 10;
        DatabaseManager.getInstance().saveResult(user.getId(), 0, score, questions.size(), correct);

        activeTypeFilter = null;
        practiceStep = 4;
        buildUI();
    }

    private void startTimer() {
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            timeLeft--;
            Label display = (Label) contentBox.lookup("#timer-display");
            if (display != null) {
                display.setText(formatTime(timeLeft));
                if (timeLeft <= 60) display.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #ef4444;");
            }
            if (timeLeft <= 0) {
                timerTimeline.stop();
                showAlert("Time is up! Submitting automatically.");
                handleSubmit();
            }
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private String formatTime(int seconds) {
        if (seconds < 0) return "--:--";
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public void dispose() {
        if (timerTimeline != null) timerTimeline.stop();
        if (transcriptPane != null) transcriptPane.dispose();
        if (mediaPlayer != null) { mediaPlayer.stop(); mediaPlayer.dispose(); }
    }
}
