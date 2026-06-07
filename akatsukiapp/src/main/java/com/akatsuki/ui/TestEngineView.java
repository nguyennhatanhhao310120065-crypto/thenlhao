package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;

import java.util.*;

public class TestEngineView extends VBox {
    private final User user;
    private final int bankId;
    private final Runnable onFinish;
    private QuestionBank bank;
    private Map<Integer, String> answers = new HashMap<>();
    private boolean isSubmitted = false;
    private double testScore = 0;
    private int correctCount = 0;
    private Set<Integer> savedQuestionIds = new HashSet<>();
    private MediaPlayer mediaPlayer;
    private VBox contentBox;
    private FlowPane navigatorGrid;
    private TranscriptPane transcriptPane;
    private String activeTypeFilter = null;

    public TestEngineView(User user, int bankId, Runnable onFinish) {
        this.user = user;
        this.bankId = bankId;
        this.onFinish = onFinish;
        setSpacing(0);
        contentBox = new VBox();
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        getChildren().add(contentBox);
        loadBank();
    }

    private void loadBank() {
        bank = DatabaseManager.getInstance().getBankById(bankId);
        savedQuestionIds = new HashSet<>(DatabaseManager.getInstance().getSavedQuestionIds(user.getId()));
        if (bank == null) {
            contentBox.getChildren().add(new Label("Failed to load test."));
            return;
        }
        initAudio();
        buildTestUI();
    }

    private void initAudio() {
        if (bank.getAudioUrl() != null && !bank.getAudioUrl().isEmpty() && !"placeholder_url".equals(bank.getAudioUrl())) {
            try {
                Media media = new Media(bank.getAudioUrl());
                mediaPlayer = new MediaPlayer(media);
            } catch (Exception e) {
                System.err.println("Cannot load audio: " + e.getMessage());
            }
        }
    }

    // ==================== TEST UI (During Test) ====================

    private void buildTestUI() {
        contentBox.getChildren().clear();
        if (isSubmitted) { buildResultsUI(); return; }

        List<Question> allQ = bank.getAllQuestions();

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(0, 0, 24, 0));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(6);
        Label tag = new Label("⏱ Test in Progress");
        tag.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1e3fae;");
        Label title = new Label(bank.getBankName());
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 700;");
        titleBox.getChildren().addAll(tag, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox stats = new HBox(12);
        stats.setStyle("-fx-padding: 10 20; -fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14;");
        Label qCounter = new Label(answers.size() + " / " + allQ.size());
        qCounter.setStyle("-fx-font-weight: 700;");
        qCounter.setId("q-counter");
        Label typeLabel = new Label(bank.getExamType());
        typeLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #1e3fae;");
        stats.getChildren().addAll(new Label("Questions: "), qCounter, new Separator(Orientation.VERTICAL), new Label("Type: "), typeLabel);

        header.getChildren().addAll(titleBox, spacer, stats);

        // Main grid: left (scrollable questions) + right (sticky navigator)
        // Audio player
        VBox leftCol = new VBox(20);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        if (mediaPlayer != null) {
            HBox audioPlayer = new HBox(14);
            audioPlayer.setStyle("-fx-padding: 14; -fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e2e8f0; -fx-border-radius: 14;");
            audioPlayer.setAlignment(Pos.CENTER_LEFT);
            Button playBtn = new Button("▶");
            playBtn.setStyle("-fx-font-size: 22px; -fx-min-width: 44; -fx-min-height: 44; -fx-background-color: #1e3fae; -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand;");
            playBtn.setOnAction(e -> {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause(); playBtn.setText("▶");
                } else {
                    mediaPlayer.play(); playBtn.setText("⏸");
                }
            });

            Slider progressSlider = new Slider(0, 100, 0);
            HBox.setHgrow(progressSlider, Priority.ALWAYS);
            Label audioTimeLabel = new Label("0:00");
            audioTimeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748b; -fx-min-width: 80;");

            mediaPlayer.currentTimeProperty().addListener((obs, ov, nv) -> {
                if (nv != null) Platform.runLater(() -> {
                    double sec = nv.toSeconds();
                    int m = (int)(sec/60), s = (int)(sec%60);
                    String total = "";
                    if (mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()) {
                        double ts = mediaPlayer.getTotalDuration().toSeconds();
                        total = String.format(" / %d:%02d", (int)(ts/60), (int)(ts%60));
                    }
                    audioTimeLabel.setText(String.format("%d:%02d%s", m, s, total));
                    if (mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown())
                        progressSlider.setValue((sec / mediaPlayer.getTotalDuration().toSeconds()) * 100);
                });
            });
            progressSlider.setOnMouseReleased(e -> {
                if (mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()) {
                    mediaPlayer.seek(javafx.util.Duration.millis((progressSlider.getValue() / 100) * mediaPlayer.getTotalDuration().toMillis()));
                }
            });
            mediaPlayer.setOnEndOfMedia(() -> { mediaPlayer.stop(); playBtn.setText("▶"); });

            Label audioLabel = new Label("🎧 Audio Player");
            audioLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
            audioPlayer.getChildren().addAll(playBtn, audioLabel, progressSlider, audioTimeLabel);
            leftCol.getChildren().add(audioPlayer);
        }

        // Question type filter tabs
        Map<String, List<Question>> grouped = new LinkedHashMap<>();
        for (Question q : allQ) grouped.computeIfAbsent(q.getNormalizedType(), k -> new ArrayList<>()).add(q);

        if (grouped.size() > 1) {
            HBox filterTabs = new HBox(8);
            filterTabs.setPadding(new Insets(0, 0, 4, 0));

            Button allTab = new Button("All (" + allQ.size() + ")");
            allTab.getStyleClass().add("filter-tab");
            if (activeTypeFilter == null) allTab.getStyleClass().add("active");
            allTab.setOnAction(e -> { activeTypeFilter = null; buildTestUI(); });
            filterTabs.getChildren().add(allTab);

            for (var entry : grouped.entrySet()) {
                Button tab = new Button(Question.getTypeLabel(entry.getKey()) + " (" + entry.getValue().size() + ")");
                tab.getStyleClass().add("filter-tab");
                if (entry.getKey().equals(activeTypeFilter)) tab.getStyleClass().add("active");
                String type = entry.getKey();
                tab.setOnAction(e -> { activeTypeFilter = type; buildTestUI(); });
                filterTabs.getChildren().add(tab);
            }

            ScrollPane filterScroll = new ScrollPane(filterTabs);
            filterScroll.setFitToHeight(true);
            filterScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            filterScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            filterScroll.setStyle("-fx-background-color: transparent;");
            filterScroll.setPrefHeight(40);
            leftCol.getChildren().add(filterScroll);
        }

        // Question sections
        Map<String, List<Question>> displayGroups = new LinkedHashMap<>();
        if (activeTypeFilter != null && grouped.containsKey(activeTypeFilter)) {
            displayGroups.put(activeTypeFilter, grouped.get(activeTypeFilter));
        } else {
            displayGroups = grouped;
        }

        for (var entry : displayGroups.entrySet()) {
            VBox section = new VBox(14);
            section.getStyleClass().add("section-card");
            section.setPadding(new Insets(28));

            Label sectionTitle = new Label(Question.getTypeLabel(entry.getKey()) + " — " + entry.getValue().size() + " Questions");
            sectionTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 700;");
            section.getChildren().add(sectionTitle);

            for (Question q : entry.getValue()) {
                VBox qBlock = buildQuestionBlock(q, allQ);
                qBlock.setId("question-" + q.getId());
                section.getChildren().add(qBlock);
            }
            leftCol.getChildren().add(section);
        }

        ScrollPane leftScroll = new ScrollPane(leftCol);
        leftScroll.setFitToWidth(true);
        leftScroll.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(leftScroll, Priority.ALWAYS);

        // Right: sticky navigator
        VBox rightCol = new VBox(16);
        rightCol.setPrefWidth(240);
        rightCol.setMinWidth(240);
        rightCol.setMaxWidth(240);

        VBox navCard = new VBox(14);
        navCard.getStyleClass().add("navigator-card");
        navCard.setPadding(new Insets(20));

        Label navTitle = new Label("📋 Question Navigator");
        navTitle.setStyle("-fx-font-weight: 700;");

        navigatorGrid = new FlowPane(6, 6);
        rebuildNavigatorDots(allQ);

        Button submitBtn = new Button("Submit Test");
        submitBtn.getStyleClass().addAll("btn", "btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);
        submitBtn.setOnAction(e -> handleSubmit(allQ));

        Label submitNote = new Label("Review all answers before submitting");
        submitNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-alignment: center;");
        submitNote.setAlignment(Pos.CENTER);

        navCard.getChildren().addAll(navTitle, navigatorGrid, submitBtn, submitNote);
        rightCol.getChildren().add(navCard);

        // Layout: use BorderPane so right column is outside the scroll
        HBox mainLayout = new HBox(24);
        mainLayout.getChildren().addAll(leftScroll, rightCol);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        VBox wrapper = new VBox(0, header, mainLayout);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        contentBox.getChildren().add(wrapper);
    }

    private VBox buildQuestionBlock(Question q, List<Question> allQ) {
        VBox block = new VBox(10);
        block.setPadding(new Insets(0, 0, 20, 0));

        HBox label = new HBox(10);
        label.setAlignment(Pos.TOP_LEFT);
        Label num = new Label(String.valueOf(q.getQuestionNumber()));
        num.setStyle("-fx-min-width: 28; -fx-min-height: 28; -fx-alignment: center; -fx-background-color: #f1f5f9; -fx-text-fill: #94a3b8; -fx-background-radius: 8; -fx-font-weight: 700; -fx-font-size: 12px;");

        VBox textBox = new VBox(4);
        Label qText = new Label(q.getQuestionText());
        qText.setWrapText(true);
        qText.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #334155;");
        Label badge = new Label(Question.getTypeLabel(q.getNormalizedType()));
        badge.getStyleClass().addAll("badge", "badge-slate");
        badge.setStyle("-fx-font-size: 9px;");
        textBox.getChildren().addAll(qText, badge);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        label.getChildren().addAll(num, textBox);
        block.getChildren().add(label);

        String t = q.getNormalizedType();
        String sel = answers.getOrDefault(q.getId(), "");

        if (("mcq".equals(t) || "matching".equals(t)) && !q.getOptions().isEmpty()) {
            VBox optsBox = new VBox(6);
            optsBox.setPadding(new Insets(0, 0, 0, 38));
            ToggleGroup group = new ToggleGroup();
            for (String opt : q.getOptions()) {
                String letter = opt.isEmpty() ? "" : String.valueOf(opt.charAt(0));
                String text = opt.length() > 2 ? opt.substring(opt.indexOf('.') + 1).trim() : opt;
                RadioButton rb = new RadioButton(letter + ". " + text);
                rb.setToggleGroup(group);
                rb.setStyle("-fx-font-size: 14px; -fx-padding: 8;");
                if (letter.equals(sel)) rb.setSelected(true);
                rb.setOnAction(e -> { answers.put(q.getId(), letter); updateNavigator(allQ); });
                optsBox.getChildren().add(rb);
            }
            block.getChildren().add(optsBox);
        } else if ("true_false".equals(t) || "true_false_ng".equals(t)) {
            HBox tfBox = new HBox(10);
            tfBox.setPadding(new Insets(0, 0, 0, 38));
            ToggleGroup group = new ToggleGroup();
            String[] options = "true_false_ng".equals(t) ? new String[]{"True", "False", "Not Given"} : new String[]{"True", "False"};
            for (String opt : options) {
                RadioButton rb = new RadioButton(opt);
                rb.setToggleGroup(group);
                rb.setStyle("-fx-font-weight: 700; -fx-padding: 8 18;");
                if (opt.equals(sel)) rb.setSelected(true);
                rb.setOnAction(e -> { answers.put(q.getId(), opt); updateNavigator(allQ); });
                tfBox.getChildren().add(rb);
            }
            block.getChildren().add(tfBox);
        } else {
            HBox inputBox = new HBox();
            inputBox.setPadding(new Insets(0, 0, 0, 38));
            TextField inputField = new TextField(sel);
            inputField.setPromptText("Type your answer here...");
            inputField.setPrefWidth(400);
            inputField.getStyleClass().add("q-input");
            inputField.textProperty().addListener((obs, old, val) -> {
                if (val != null && !val.isEmpty()) answers.put(q.getId(), val);
                else answers.remove(q.getId());
                updateNavigator(allQ);
            });
            inputBox.getChildren().add(inputField);
            block.getChildren().add(inputBox);
        }

        return block;
    }

    private void rebuildNavigatorDots(List<Question> allQ) {
        navigatorGrid.getChildren().clear();
        for (int i = 0; i < allQ.size(); i++) {
            Question q = allQ.get(i);
            Label dot = new Label(String.valueOf(i + 1));
            dot.getStyleClass().add("q-dot");
            if (answers.containsKey(q.getId())) dot.getStyleClass().add("answered");
            dot.setPrefWidth(34);
            dot.setPrefHeight(34);
            dot.setAlignment(Pos.CENTER);
            dot.setCursor(javafx.scene.Cursor.HAND);
            final int qId = q.getId();
            dot.setOnMouseClicked(e -> scrollToQuestion(qId));
            navigatorGrid.getChildren().add(dot);
        }
    }

    private void scrollToQuestion(int questionId) {
        var node = contentBox.lookup("#question-" + questionId);
        if (node != null) {
            node.requestFocus();
            Platform.runLater(() -> {
                var scrollPane = (ScrollPane) contentBox.lookup(".scroll-pane");
                if (scrollPane == null) {
                    for (var child : contentBox.getChildren()) {
                        if (child instanceof VBox wrapper) {
                            for (var wChild : wrapper.getChildren()) {
                                if (wChild instanceof HBox hbox) {
                                    for (var hChild : hbox.getChildren()) {
                                        if (hChild instanceof ScrollPane sp) {
                                            scrollToNode(sp, node);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    scrollToNode(scrollPane, node);
                }
            });
        }
    }

    private void scrollToNode(ScrollPane sp, javafx.scene.Node node) {
        var content = sp.getContent();
        double contentHeight = content.getBoundsInLocal().getHeight();
        double viewportHeight = sp.getViewportBounds().getHeight();
        if (contentHeight <= viewportHeight) return;
        var bounds = node.localToScene(node.getBoundsInLocal());
        var contentBounds = content.localToScene(content.getBoundsInLocal());
        double nodeY = bounds.getMinY() - contentBounds.getMinY();
        double target = (nodeY - 100) / (contentHeight - viewportHeight);
        sp.setVvalue(Math.max(0, Math.min(1, target)));
    }

    private void updateNavigator(List<Question> allQ) {
        Label counter = (Label) contentBox.lookup("#q-counter");
        if (counter != null) counter.setText(answers.size() + " / " + allQ.size());

        if (navigatorGrid != null) {
            for (int i = 0; i < allQ.size() && i < navigatorGrid.getChildren().size(); i++) {
                Label dot = (Label) navigatorGrid.getChildren().get(i);
                boolean wasAnswered = dot.getStyleClass().contains("answered");
                boolean isAnswered = answers.containsKey(allQ.get(i).getId());
                if (isAnswered && !wasAnswered) dot.getStyleClass().add("answered");
                else if (!isAnswered && wasAnswered) dot.getStyleClass().remove("answered");
            }
        }
    }

    private void handleSubmit(List<Question> allQ) {
        int unanswered = allQ.size() - answers.size();
        if (unanswered > 0) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "You have " + unanswered + " unanswered question(s). Submit anyway?",
                    ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.YES) return;
        }

        correctCount = 0;
        for (Question q : allQ) {
            if (Question.isAnswerCorrect(answers.get(q.getId()), q.getCorrectAnswer(), q.getQuestionType()))
                correctCount++;
        }
        testScore = allQ.isEmpty() ? 0 : (double) correctCount / allQ.size() * 10;
        isSubmitted = true;

        DatabaseManager.getInstance().saveResult(user.getId(), bankId, testScore, allQ.size(), correctCount);
        if (mediaPlayer != null) mediaPlayer.stop();
        activeTypeFilter = null;
        buildTestUI();
    }

    // ==================== RESULTS UI (After Submit) ====================

    private void buildResultsUI() {
        contentBox.getChildren().clear();

        List<Question> allQ = bank.getAllQuestions();

        // Re-init audio for replay
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.dispose(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        initAudio();

        // Header
        HBox header = new HBox();
        header.setPadding(new Insets(0, 0, 20, 0));
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(6);
        Label tag = new Label("✅ Test Completed");
        tag.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #059669;");
        Label title = new Label("Review Results — " + bank.getBankName());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700;");
        titleBox.getChildren().addAll(tag, title);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);
        Label scoreLabel = new Label("SCORE");
        scoreLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        Label scoreVal = new Label(String.format("%.1f / 10.0", testScore));
        scoreVal.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: #1e3fae;");
        Label correctLabel = new Label(correctCount + " / " + allQ.size() + " correct");
        correctLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        scoreBox.getChildren().addAll(scoreLabel, scoreVal, correctLabel);

        Button finishBtn = new Button("Finish Review ▶");
        finishBtn.getStyleClass().addAll("btn", "btn-dark");
        finishBtn.setOnAction(e -> { disposeTranscript(); onFinish.run(); });

        HBox rightBox = new HBox(20, scoreBox, finishBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(titleBox, spacer, rightBox);

        // Left: Transcript pane (30% width)
        transcriptPane = new TranscriptPane(bank.getTranscript(), mediaPlayer);
        transcriptPane.setPrefWidth(380);
        transcriptPane.setMinWidth(320);

        // Right: Question analysis with type filter
        VBox analysisPanel = new VBox(14);

        // Type filter for results
        Map<String, List<Question>> grouped = new LinkedHashMap<>();
        for (Question q : allQ) grouped.computeIfAbsent(q.getNormalizedType(), k -> new ArrayList<>()).add(q);

        if (grouped.size() > 1) {
            HBox filterTabs = new HBox(6);
            Button allTab = new Button("All (" + allQ.size() + ")");
            allTab.getStyleClass().add("filter-tab");
            if (activeTypeFilter == null) allTab.getStyleClass().add("active");
            allTab.setOnAction(e -> { activeTypeFilter = null; buildResultsUI(); });
            filterTabs.getChildren().add(allTab);

            for (var entry : grouped.entrySet()) {
                Button tab = new Button(Question.getTypeLabel(entry.getKey()) + " (" + entry.getValue().size() + ")");
                tab.getStyleClass().add("filter-tab");
                if (entry.getKey().equals(activeTypeFilter)) tab.getStyleClass().add("active");
                String type = entry.getKey();
                tab.setOnAction(e -> { activeTypeFilter = type; buildResultsUI(); });
                filterTabs.getChildren().add(tab);
            }
            analysisPanel.getChildren().add(filterTabs);
        }

        Label analysisTitle = new Label("📊 Question Analysis");
        analysisTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 16px;");
        analysisPanel.getChildren().add(analysisTitle);

        List<Question> displayQuestions;
        if (activeTypeFilter != null && grouped.containsKey(activeTypeFilter)) {
            displayQuestions = grouped.get(activeTypeFilter);
        } else {
            displayQuestions = allQ;
        }

        for (Question q : displayQuestions) {
            VBox card = buildResultCard(q, allQ);
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

    private VBox buildResultCard(Question q, List<Question> allQ) {
        int idx = allQ.indexOf(q);
        boolean correct = Question.isAnswerCorrect(answers.get(q.getId()), q.getCorrectAnswer(), q.getQuestionType());
        String userAns = answers.getOrDefault(q.getId(), "");
        String t = q.getNormalizedType();

        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-width: 2; -fx-border-color: " +
                (correct ? "#d1fae5" : "#fee2e2") + ";");

        // Card header: number + status + type + save button
        HBox cardHeader = new HBox(8);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label numLabel = new Label(String.valueOf(q.getQuestionNumber()));
        numLabel.setStyle("-fx-min-width: 28; -fx-min-height: 28; -fx-alignment: center; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: " +
                (correct ? "#059669" : "#ef4444") + ";");
        Label statusLabel = new Label(correct ? "Correct" : "Incorrect");
        statusLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: " + (correct ? "#047857" : "#b91c1c") + ";");
        Label typeBadge = new Label(Question.getTypeLabel(t));
        typeBadge.getStyleClass().addAll("badge", "badge-slate");

        Region s2 = new Region();
        HBox.setHgrow(s2, Priority.ALWAYS);

        Button saveBtn = new Button(savedQuestionIds.contains(q.getId()) ? "🔖" : "☆");
        saveBtn.setStyle("-fx-font-size: 18px; -fx-background-color: transparent; -fx-cursor: hand;");
        final int qId = q.getId();
        saveBtn.setOnAction(e -> {
            if (savedQuestionIds.contains(qId)) {
                DatabaseManager.getInstance().unsaveQuestion(user.getId(), qId);
                savedQuestionIds.remove(qId);
                saveBtn.setText("☆");
            } else {
                DatabaseManager.getInstance().saveQuestion(user.getId(), qId);
                savedQuestionIds.add(qId);
                saveBtn.setText("🔖");
            }
        });

        cardHeader.getChildren().addAll(numLabel, statusLabel, typeBadge, s2, saveBtn);

        // Question text
        Label qTextLabel = new Label(q.getQuestionText());
        qTextLabel.setWrapText(true);
        qTextLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px;");

        card.getChildren().addAll(cardHeader, qTextLabel);

        // Show ALL options with correct/incorrect highlighting (for MCQ/matching/TF)
        if (("mcq".equals(t) || "matching".equals(t)) && q.getOptions() != null && !q.getOptions().isEmpty()) {
            VBox optsBox = new VBox(6);
            optsBox.setPadding(new Insets(4, 0, 4, 0));
            for (String opt : q.getOptions()) {
                String letter = opt.isEmpty() ? "" : String.valueOf(opt.charAt(0));
                boolean isUserChoice = letter.equalsIgnoreCase(userAns);
                boolean isCorrectChoice = letter.equalsIgnoreCase(q.getCorrectAnswer());

                HBox optRow = new HBox(8);
                optRow.setPadding(new Insets(8, 12, 8, 12));
                optRow.setAlignment(Pos.CENTER_LEFT);

                String bgColor = "transparent";
                String textColor = "#475569";
                String borderColor = "#f1f5f9";
                String indicator = "";

                if (isCorrectChoice) {
                    bgColor = "#ecfdf5";
                    textColor = "#047857";
                    borderColor = "#86efac";
                    indicator = " ✓";
                }
                if (isUserChoice && !correct) {
                    bgColor = "#fef2f2";
                    textColor = "#b91c1c";
                    borderColor = "#fca5a5";
                    indicator = " ✗";
                }
                if (isUserChoice && correct) {
                    indicator = " ✓";
                }

                optRow.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-border-width: 1;");
                Label optLabel = new Label(opt + indicator);
                optLabel.setWrapText(true);
                optLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: " + ((isUserChoice || isCorrectChoice) ? "700" : "400") + "; -fx-text-fill: " + textColor + ";");
                optRow.getChildren().add(optLabel);
                optsBox.getChildren().add(optRow);
            }
            card.getChildren().add(optsBox);
        } else if ("true_false".equals(t) || "true_false_ng".equals(t)) {
            String[] tfOpts = "true_false_ng".equals(t) ? new String[]{"True", "False", "Not Given"} : new String[]{"True", "False"};
            HBox tfBox = new HBox(8);
            tfBox.setPadding(new Insets(4, 0, 4, 0));
            for (String opt : tfOpts) {
                boolean isUserChoice = opt.equalsIgnoreCase(userAns);
                boolean isCorrectChoice = opt.equalsIgnoreCase(q.getCorrectAnswer());

                String bgColor = "#f8fafc";
                String textColor = "#64748b";
                if (isCorrectChoice) { bgColor = "#ecfdf5"; textColor = "#047857"; }
                if (isUserChoice && !correct) { bgColor = "#fef2f2"; textColor = "#b91c1c"; }

                Label tfLabel = new Label(opt + (isCorrectChoice ? " ✓" : "") + (isUserChoice && !correct ? " ✗" : ""));
                tfLabel.setStyle("-fx-padding: 8 16; -fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-font-weight: 700; -fx-text-fill: " + textColor + ";");
                tfBox.getChildren().add(tfLabel);
            }
            card.getChildren().add(tfBox);
        } else {
            // Fill-in type: show user answer and correct answer
            VBox ansBoxes = new VBox(6);
            String displayUser = userAns.isEmpty() ? "(No answer)" : userAns;
            HBox yourAnsBox = new HBox();
            yourAnsBox.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-background-color: " + (correct ? "#ecfdf5" : "#fef2f2") + ";");
            Label yourLabel = new Label("Your answer: " + displayUser);
            yourLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: " + (correct ? "#047857" : "#b91c1c") + ";");
            yourAnsBox.getChildren().add(yourLabel);
            ansBoxes.getChildren().add(yourAnsBox);

            if (!correct) {
                HBox correctBox = new HBox();
                correctBox.setStyle("-fx-padding: 10; -fx-background-radius: 8; -fx-background-color: #ecfdf5;");
                Label correctLbl = new Label("Correct answer: " + q.getCorrectAnswer());
                correctLbl.setStyle("-fx-font-weight: 700; -fx-text-fill: #047857;");
                correctBox.getChildren().add(correctLbl);
                ansBoxes.getChildren().add(correctBox);
            }
            card.getChildren().add(ansBoxes);
        }

        // Transcript quote (clickable → highlight in transcript + play audio)
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
            Label playLbl = new Label(mediaPlayer != null ? "▶ Play" : "");
            playLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1e3fae; -fx-padding: 2 10; -fx-background-color: rgba(30,63,174,0.1); -fx-background-radius: 99;");
            quoteHeader.getChildren().addAll(quoteLbl, qs);
            if (mediaPlayer != null) quoteHeader.getChildren().add(playLbl);

            Label quoteText = new Label("\"" + quote + "\"");
            quoteText.setWrapText(true);
            quoteText.setStyle("-fx-font-size: 13px; -fx-text-fill: #78350f; -fx-font-style: italic;");

            quoteBox.getChildren().addAll(quoteHeader, quoteText);
            quoteBox.setOnMouseClicked(e -> {
                if (transcriptPane != null) {
                    transcriptPane.highlightQuoteAndPlay(quote);
                }
            });

            card.getChildren().add(quoteBox);
        }

        // Explanation
        if (q.getExplanation() != null && !q.getExplanation().isEmpty()) {
            VBox explBox = new VBox(4);
            explBox.setStyle("-fx-padding: 10 0 0 0; -fx-border-color: #f1f5f9 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
            Label explTitle = new Label("💡 EXPLANATION");
            explTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1e3fae;");
            Label explText = new Label(q.getExplanation());
            explText.setWrapText(true);
            explText.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
            explBox.getChildren().addAll(explTitle, explText);
            card.getChildren().add(explBox);
        }

        return card;
    }

    private void disposeTranscript() {
        if (transcriptPane != null) transcriptPane.dispose();
    }

    public void dispose() {
        disposeTranscript();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
    }
}
