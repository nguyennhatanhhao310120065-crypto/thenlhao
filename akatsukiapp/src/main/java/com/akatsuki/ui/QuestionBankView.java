package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.*;
import com.akatsuki.service.GeminiService;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.*;

import java.io.File;
import java.net.URI;
import java.util.*;

public class QuestionBankView extends VBox {
    private final User user;
    private QuestionBank selectedBank;
    private VBox contentBox;
    private MediaPlayer mediaPlayer;
    private TranscriptPane transcriptPane;
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
        Label title = new Label("My Question Banks");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 700;");
        Label subtitle = new Label("Manage and review your saved listening tests.");
        subtitle.setStyle("-fx-text-fill: #64748b;");
        header.getChildren().addAll(title, subtitle);

        FlowPane grid = new FlowPane(24, 24);
        grid.setPrefWrapLength(1200);

        List<QuestionBank> banks = DatabaseManager.getInstance().getBanks(user.getId());

        if (banks.isEmpty()) {
            Label empty = new Label("No question banks yet.");
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-padding: 60;");
            grid.getChildren().add(empty);
        } else {
            for (QuestionBank b : banks) {
                VBox card = new VBox(12);
                card.getStyleClass().add("bank-list-card");
                card.setPrefWidth(280);
                card.setPadding(new Insets(24));
                card.setCursor(javafx.scene.Cursor.HAND);

                HBox top = new HBox();
                top.setAlignment(Pos.CENTER_LEFT);
                Label badge = new Label(b.getExamType());
                badge.getStyleClass().addAll("badge", "badge-primary");
                top.getChildren().add(badge);

                Label name = new Label(b.getBankName());
                name.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");
                name.setWrapText(true);

                HBox footer = new HBox();
                footer.setAlignment(Pos.CENTER_LEFT);
                footer.setStyle("-fx-padding: 12 0 0 0; -fx-border-color: #f8fafc transparent transparent transparent; -fx-border-width: 1 0 0 0;");
                Label qc = new Label(b.getQuestionCount() + " questions");
                qc.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label date = new Label(b.getCreatedAt() != null ? b.getCreatedAt().substring(0, Math.min(10, b.getCreatedAt().length())) : "");
                date.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
                footer.getChildren().addAll(qc, spacer, date);

                card.getChildren().addAll(top, name, footer);
                card.setOnMouseClicked(e -> openBankDetail(b.getId()));
                grid.getChildren().add(card);
            }
        }

        contentBox.getChildren().addAll(header, grid);
    }

    private void openBankDetail(int bankId) {
        selectedBank = DatabaseManager.getInstance().getBankById(bankId);
        if (selectedBank == null) return;
        activeTypeFilter = null;
        buildDetail();
    }

    private boolean transcriptHasTimestamps(String transcript) {
        if (transcript == null || transcript.isBlank()) return false;
        return transcript.contains("[0:") || transcript.contains("[1:") || transcript.contains("[2:")
                || transcript.contains("[3:") || transcript.contains("[4:") || transcript.contains("[5:");
    }

    private void buildDetail() {
        contentBox.getChildren().clear();
        disposeAudio();

        String rawTranscript = selectedBank.getTranscript() != null ? selectedBank.getTranscript() : "";
        boolean hasAudio = selectedBank.getAudioUrl() != null && !selectedBank.getAudioUrl().isEmpty()
                && !"placeholder_url".equals(selectedBank.getAudioUrl());

        // Auto re-transcribe old transcripts that don't have timestamps
        if (hasAudio && !transcriptHasTimestamps(rawTranscript) && !rawTranscript.isBlank()) {
            autoReTranscribe();
            return;
        }

        if (hasAudio) {
            try {
                Media media = new Media(selectedBank.getAudioUrl());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setOnError(() -> System.err.println("Media error: " + mediaPlayer.getError()));
            } catch (Exception e) {
                System.err.println("Cannot load audio: " + e.getMessage());
                mediaPlayer = null;
            }
        }

        Button backBtn = new Button("◀ Back to Banks");
        backBtn.getStyleClass().addAll("btn", "btn-ghost");
        backBtn.setOnAction(e -> { disposeAudio(); selectedBank = null; activeTypeFilter = null; buildList(); });

        // Header bar
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(0, 0, 16, 0));

        VBox titleBox = new VBox(4);
        Label bankTitle = new Label(selectedBank.getBankName());
        bankTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 700;");
        Label bankMeta = new Label(selectedBank.getExamType() + " • " + selectedBank.getAllQuestions().size() + " questions");
        bankMeta.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        titleBox.getChildren().addAll(bankTitle, bankMeta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);

        // Manual re-transcribe button (always available)
        Button reTranscribeBtn = new Button("🔄 Re-transcribe");
        reTranscribeBtn.getStyleClass().addAll("btn", "btn-ghost");
        reTranscribeBtn.setOnAction(e -> handleReTranscribe(reTranscribeBtn));
        if (hasAudio) actions.getChildren().add(reTranscribeBtn);

        Button visBtn = new Button(selectedBank.isPublic() ? "🌐 Public" : "🔒 Private");
        visBtn.getStyleClass().addAll("btn", selectedBank.isPublic() ? "btn-primary" : "btn-ghost");
        visBtn.setOnAction(e -> {
            boolean newVis = !selectedBank.isPublic();
            if (DatabaseManager.getInstance().toggleVisibility(selectedBank.getId(), user.getId(), newVis)) {
                selectedBank.setPublic(newVis);
                visBtn.setText(newVis ? "🌐 Public" : "🔒 Private");
            }
        });

        Button deleteBtn = new Button("🗑 Delete");
        deleteBtn.getStyleClass().addAll("btn", "btn-danger");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this question bank permanently?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    disposeAudio();
                    DatabaseManager.getInstance().deleteBank(selectedBank.getId(), user.getId());
                    selectedBank = null;
                    buildList();
                }
            });
        });

        actions.getChildren().addAll(visBtn, deleteBtn);
        headerBar.getChildren().addAll(titleBox, spacer, actions);

        // LEFT: Transcript pane
        transcriptPane = new TranscriptPane(rawTranscript, mediaPlayer);
        transcriptPane.setPrefWidth(550);
        transcriptPane.setMinWidth(450);
        HBox.setHgrow(transcriptPane, Priority.SOMETIMES);

        // RIGHT: Questions panel with type filter
        VBox questionsPanel = new VBox(14);
        HBox.setHgrow(questionsPanel, Priority.ALWAYS);

        List<Question> allQ = selectedBank.getAllQuestions();
        Map<String, List<Question>> grouped = new LinkedHashMap<>();
        for (Question q : allQ) grouped.computeIfAbsent(q.getNormalizedType(), k -> new ArrayList<>()).add(q);

        if (grouped.size() > 1) {
            HBox filterTabs = new HBox(6);
            Button allTab = new Button("All (" + allQ.size() + ")");
            allTab.getStyleClass().add("filter-tab");
            if (activeTypeFilter == null) allTab.getStyleClass().add("active");
            allTab.setOnAction(e -> { activeTypeFilter = null; buildDetail(); });
            filterTabs.getChildren().add(allTab);

            for (var entry : grouped.entrySet()) {
                Button tab = new Button(Question.getTypeLabel(entry.getKey()) + " (" + entry.getValue().size() + ")");
                tab.getStyleClass().add("filter-tab");
                if (entry.getKey().equals(activeTypeFilter)) tab.getStyleClass().add("active");
                String type = entry.getKey();
                tab.setOnAction(e -> { activeTypeFilter = type; buildDetail(); });
                filterTabs.getChildren().add(tab);
            }
            questionsPanel.getChildren().add(filterTabs);
        }

        Label qTitle = new Label("📊 Questions (" + allQ.size() + ")");
        qTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 16px;");
        questionsPanel.getChildren().add(qTitle);

        List<Question> displayQuestions;
        if (activeTypeFilter != null && grouped.containsKey(activeTypeFilter)) {
            displayQuestions = grouped.get(activeTypeFilter);
        } else {
            displayQuestions = allQ;
        }

        for (Question q : displayQuestions) {
            VBox qCard = buildQuestionCard(q, hasAudio);
            questionsPanel.getChildren().add(qCard);
        }

        ScrollPane qScroll = new ScrollPane(questionsPanel);
        qScroll.setFitToWidth(true);
        qScroll.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(qScroll, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(transcriptPane, qScroll);
        splitPane.setDividerPositions(0.40);
        splitPane.setStyle("-fx-background-color: transparent; -fx-padding: 8 0 0 0;");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        contentBox.getChildren().addAll(backBtn, headerBar, splitPane);
    }

    private void autoReTranscribe() {
        contentBox.getChildren().clear();

        VBox loadingBox = new VBox(16);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(80));

        Label icon = new Label("🔄");
        icon.setStyle("-fx-font-size: 48px;");
        Label title = new Label("Đang cập nhật Transcript...");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700;");
        Label subtitle = new Label("Transcript cũ không có timestamps và speaker labels.\nĐang tự động tạo lại với format mới. Vui lòng đợi...");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(48, 48);

        Button cancelBtn = new Button("◀ Quay lại");
        cancelBtn.getStyleClass().addAll("btn", "btn-ghost");
        cancelBtn.setOnAction(e -> { selectedBank = null; buildList(); });

        loadingBox.getChildren().addAll(icon, title, subtitle, progress, cancelBtn);
        contentBox.getChildren().add(loadingBox);

        new Thread(() -> {
            try {
                File audioFile = new File(new URI(selectedBank.getAudioUrl()));
                if (!audioFile.exists()) {
                    Platform.runLater(() -> {
                        showAlert("Không tìm thấy file audio. Hiển thị transcript cũ.");
                        selectedBank.setTranscript(selectedBank.getTranscript() + "\n[0:00] ");
                        buildDetail();
                    });
                    return;
                }

                String newTranscript = GeminiService.getInstance().transcribeAudio(audioFile);

                if (newTranscript != null && !newTranscript.isBlank()) {
                    DatabaseManager.getInstance().updateTranscript(selectedBank.getId(), newTranscript);
                    selectedBank.setTranscript(newTranscript);
                    Platform.runLater(this::buildDetail);
                } else {
                    Platform.runLater(() -> {
                        showAlert("Re-transcribe thất bại. Hiển thị transcript cũ.");
                        buildDetailWithCurrentData();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Re-transcribe thất bại: " + e.getMessage());
                    buildDetailWithCurrentData();
                });
            }
        }).start();
    }

    private void buildDetailWithCurrentData() {
        // Force build detail even with old transcript (skip auto-retranscribe)
        String rawTranscript = selectedBank.getTranscript() != null ? selectedBank.getTranscript() : "";
        if (!rawTranscript.isBlank() && !transcriptHasTimestamps(rawTranscript)) {
            // Temporarily mark as having timestamps to bypass auto-retranscribe
            selectedBank.setTranscript("[0:00] " + rawTranscript);
        }
        buildDetail();
    }

    private void handleReTranscribe(Button btn) {
        if (selectedBank == null || selectedBank.getAudioUrl() == null) return;

        btn.setText("⏳ Đang xử lý...");
        btn.setDisable(true);

        new Thread(() -> {
            try {
                File audioFile = new File(new URI(selectedBank.getAudioUrl()));
                if (!audioFile.exists()) {
                    Platform.runLater(() -> {
                        showAlert("Không tìm thấy file audio. File có thể đã bị di chuyển hoặc xóa.");
                        btn.setText("🔄 Re-transcribe");
                        btn.setDisable(false);
                    });
                    return;
                }

                String newTranscript = GeminiService.getInstance().transcribeAudio(audioFile);

                if (newTranscript != null && !newTranscript.isBlank()) {
                    DatabaseManager.getInstance().updateTranscript(selectedBank.getId(), newTranscript);
                    selectedBank.setTranscript(newTranscript);

                    Platform.runLater(() -> {
                        showAlert("Transcript đã được cập nhật thành công với timestamps và speaker labels!");
                        buildDetail();
                    });
                } else {
                    Platform.runLater(() -> {
                        showAlert("Re-transcribe thất bại. Transcript trống.");
                        btn.setText("🔄 Re-transcribe");
                        btn.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Re-transcribe thất bại: " + e.getMessage());
                    btn.setText("🔄 Re-transcribe");
                    btn.setDisable(false);
                });
            }
        }).start();
    }

    private VBox buildQuestionCard(Question q, boolean hasAudio) {
        VBox qCard = new VBox(10);
        qCard.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 2;");
        qCard.setPadding(new Insets(16));

        HBox qHeader = new HBox(8);
        qHeader.setAlignment(Pos.CENTER_LEFT);
        Label numLabel = new Label(String.valueOf(q.getQuestionNumber()));
        numLabel.setStyle("-fx-min-width: 28; -fx-min-height: 28; -fx-alignment: center; -fx-background-color: #1e3fae; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-size: 12px; -fx-font-weight: 700;");
        Label typeBadge = new Label(Question.getTypeLabel(q.getNormalizedType()));
        typeBadge.getStyleClass().addAll("badge", "badge-slate");
        qHeader.getChildren().addAll(numLabel, typeBadge);

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
            quoteBox.setCursor(javafx.scene.Cursor.HAND);

            HBox quoteHeader = new HBox(6);
            quoteHeader.setAlignment(Pos.CENTER_LEFT);
            Label quoteLbl = new Label("📎 Transcript passage");
            quoteLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #92400e;");
            Region qs = new Region();
            HBox.setHgrow(qs, Priority.ALWAYS);
            quoteHeader.getChildren().addAll(quoteLbl, qs);
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

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void disposeAudio() {
        if (transcriptPane != null) { transcriptPane.dispose(); transcriptPane = null; }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    public void refresh() {
        if (selectedBank != null) {
            openBankDetail(selectedBank.getId());
        } else {
            buildList();
        }
    }
}
