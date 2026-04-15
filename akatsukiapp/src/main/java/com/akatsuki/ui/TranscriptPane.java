package com.akatsuki.ui;

import com.akatsuki.model.TranscriptLine;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.List;

public class TranscriptPane extends VBox {
    private final String rawTranscript;
    private final List<TranscriptLine> lines;
    private MediaPlayer mediaPlayer;
    private VBox linesContainer;
    private ScrollPane scrollPane;
    private ChangeListener<Duration> audioSyncListener;
    private ChangeListener<Duration> snippetStopListener;
    private int currentHighlightIdx = -1;
    private int quoteHighlightIdx = -1;
    private Label timeLabel;
    private Slider progressSlider;
    private boolean sliderDragging = false;
    private Button playPauseBtn;

    public TranscriptPane(String transcript, MediaPlayer mediaPlayer) {
        this.rawTranscript = transcript != null ? transcript : "";
        this.lines = TranscriptLine.parse(this.rawTranscript);
        this.mediaPlayer = mediaPlayer;

        setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: #e2e8f0; -fx-border-radius: 20;");
        setMaxHeight(Double.MAX_VALUE);
        buildUI();
        if (mediaPlayer != null) setupAudioSync();
    }

    private void buildUI() {
        getChildren().clear();

        HBox header = new HBox(8);
        header.setPadding(new Insets(16, 20, 12, 20));
        header.setStyle("-fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");

        Label title = new Label("📝 Audio Transcript");
        title.setStyle("-fx-font-weight: 700; -fx-font-size: 15px;");
        header.getChildren().add(title);

        linesContainer = new VBox(0);
        linesContainer.setPadding(new Insets(8, 0, 8, 0));
        buildTranscriptLines();

        scrollPane = new ScrollPane(linesContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(header, scrollPane);

        if (mediaPlayer != null) {
            HBox audioControls = buildAudioControls();
            audioControls.setPadding(new Insets(12, 20, 16, 20));
            audioControls.setStyle("-fx-border-color: #f1f5f9 transparent transparent transparent; -fx-border-width: 1 0 0 0;");
            getChildren().add(audioControls);
        }
    }

    private HBox buildAudioControls() {
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(8, 0, 4, 0));

        playPauseBtn = new Button("▶");
        playPauseBtn.setStyle("-fx-font-size: 16px; -fx-min-width: 36; -fx-min-height: 36; -fx-background-color: #1e3fae; -fx-text-fill: white; -fx-background-radius: 18; -fx-cursor: hand;");
        playPauseBtn.setOnAction(e -> togglePlayPause());

        Button stopBtn = new Button("⏹");
        stopBtn.setStyle("-fx-font-size: 14px; -fx-min-width: 32; -fx-min-height: 32; -fx-background-color: #f1f5f9; -fx-background-radius: 16; -fx-cursor: hand;");
        stopBtn.setOnAction(e -> { if (mediaPlayer != null) mediaPlayer.stop(); });

        progressSlider = new Slider(0, 100, 0);
        progressSlider.setPrefWidth(140);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        progressSlider.setOnMousePressed(e -> sliderDragging = true);
        progressSlider.setOnMouseReleased(e -> {
            sliderDragging = false;
            if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()) {
                double seekMs = (progressSlider.getValue() / 100.0) * mediaPlayer.getTotalDuration().toMillis();
                mediaPlayer.seek(Duration.millis(seekMs));
            }
        });

        timeLabel = new Label("0:00");
        timeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #64748b; -fx-min-width: 60;");

        if (mediaPlayer != null) {
            mediaPlayer.setOnPlaying(() -> Platform.runLater(() -> { if (playPauseBtn != null) playPauseBtn.setText("⏸"); }));
            mediaPlayer.setOnPaused(() -> Platform.runLater(() -> { if (playPauseBtn != null) playPauseBtn.setText("▶"); }));
            mediaPlayer.setOnStopped(() -> Platform.runLater(() -> { if (playPauseBtn != null) playPauseBtn.setText("▶"); }));
            mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                if (mediaPlayer != null) mediaPlayer.stop();
                if (playPauseBtn != null) playPauseBtn.setText("▶");
            }));
        }

        controls.getChildren().addAll(playPauseBtn, stopBtn, progressSlider, timeLabel);
        return controls;
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        removeSnippetStop();
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.play();
        }
    }

    private void removeSnippetStop() {
        if (snippetStopListener != null && mediaPlayer != null) {
            mediaPlayer.currentTimeProperty().removeListener(snippetStopListener);
            snippetStopListener = null;
        }
    }

    private void buildTranscriptLines() {
        linesContainer.getChildren().clear();

        if (lines.isEmpty()) {
            Label noTranscript = new Label(rawTranscript.isEmpty() ? "(No transcript available)" : rawTranscript);
            noTranscript.setWrapText(true);
            noTranscript.setPadding(new Insets(12, 20, 12, 20));
            noTranscript.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");
            linesContainer.getChildren().add(noTranscript);
            return;
        }

        for (int i = 0; i < lines.size(); i++) {
            TranscriptLine line = lines.get(i);

            if (line.isPartHeader()) {
                HBox divider = buildPartHeaderBox(line, i);
                linesContainer.getChildren().add(divider);
            } else if (line.isNarrator()) {
                HBox narratorBox = buildNarratorBox(line, i);
                linesContainer.getChildren().add(narratorBox);
            } else {
                HBox lineBox = buildLineBox(line, i);
                linesContainer.getChildren().add(lineBox);
            }
        }
    }

    /** Part header: styled as a section divider */
    private HBox buildPartHeaderBox(TranscriptLine line, int index) {
        HBox box = new HBox();
        box.setPadding(new Insets(14, 16, 6, 16));
        box.setAlignment(Pos.CENTER);
        box.setUserData(index);

        Label partLabel = new Label(line.getText());
        partLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #1e3fae; " +
                "-fx-padding: 6 20; -fx-background-color: rgba(30,63,174,0.06); -fx-background-radius: 20;");
        box.getChildren().add(partLabel);
        return box;
    }

    /** Narrator/instructions: italic, distinct background, no speaker badge */
    private HBox buildNarratorBox(TranscriptLine line, int index) {
        HBox box = new HBox(8);
        box.setPadding(new Insets(6, 16, 6, 16));
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle("-fx-cursor: hand; -fx-background-color: #f8fafc;");
        box.setUserData(index);

        if (line.hasTimestamp()) {
            Label timeTag = new Label(line.getFormattedTime());
            timeTag.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-min-width: 42; " +
                    "-fx-padding: 2 6; -fx-background-color: #f1f5f9; -fx-background-radius: 4;");
            timeTag.setAlignment(Pos.CENTER);
            box.getChildren().add(timeTag);
        }

        VBox textContent = new VBox(2);
        HBox.setHgrow(textContent, Priority.ALWAYS);

        Label tag = new Label("📋 Instructions");
        tag.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        textContent.getChildren().add(tag);

        Label textLabel = new Label(line.getText());
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-style: italic; -fx-line-spacing: 2;");
        textContent.getChildren().add(textLabel);

        box.getChildren().add(textContent);

        box.setOnMouseClicked(e -> {
            if (mediaPlayer != null && line.hasTimestamp()) seekToTime(line.getStartTimeSec());
        });

        return box;
    }

    /** Regular speaker line */
    private HBox buildLineBox(TranscriptLine line, int index) {
        HBox box = new HBox(8);
        box.setPadding(new Insets(6, 16, 6, 16));
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle("-fx-cursor: hand;");
        box.setUserData(index);

        if (line.hasTimestamp()) {
            Label timeTag = new Label(line.getFormattedTime());
            timeTag.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #1e3fae; -fx-min-width: 42; " +
                    "-fx-padding: 2 6; -fx-background-color: rgba(30,63,174,0.08); -fx-background-radius: 4;");
            timeTag.setAlignment(Pos.CENTER);
            box.getChildren().add(timeTag);
        }

        VBox textContent = new VBox(2);
        HBox.setHgrow(textContent, Priority.ALWAYS);

        if (line.getSpeaker() != null) {
            Label speakerLabel = new Label(line.getSpeaker());
            speakerLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #7c3aed;");
            textContent.getChildren().add(speakerLabel);
        }

        Label textLabel = new Label(line.getText());
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #334155; -fx-line-spacing: 2;");
        textContent.getChildren().add(textLabel);

        box.getChildren().add(textContent);

        box.setOnMouseClicked(e -> {
            if (mediaPlayer != null && line.hasTimestamp()) seekToTime(line.getStartTimeSec());
        });

        box.setOnMouseEntered(e -> {
            int idx = (int) box.getUserData();
            if (idx != currentHighlightIdx && idx != quoteHighlightIdx) {
                box.setStyle("-fx-cursor: hand; -fx-background-color: #f8fafc;");
            }
        });
        box.setOnMouseExited(e -> {
            int idx = (int) box.getUserData();
            if (idx != currentHighlightIdx && idx != quoteHighlightIdx) {
                box.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
            }
        });

        return box;
    }

    private void setupAudioSync() {
        if (mediaPlayer == null) return;

        audioSyncListener = (obs, oldTime, newTime) -> {
            if (newTime == null) return;
            double currentSec = newTime.toSeconds();

            Platform.runLater(() -> {
                if (timeLabel != null) {
                    int mins = (int) (currentSec / 60);
                    int secs = (int) (currentSec % 60);
                    String totalStr = "";
                    if (mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()) {
                        double totalSec = mediaPlayer.getTotalDuration().toSeconds();
                        int tMins = (int) (totalSec / 60);
                        int tSecs = (int) (totalSec % 60);
                        totalStr = String.format(" / %d:%02d", tMins, tSecs);
                    }
                    timeLabel.setText(String.format("%d:%02d%s", mins, secs, totalStr));
                }

                if (!sliderDragging && progressSlider != null && mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()) {
                    double progress = (currentSec / mediaPlayer.getTotalDuration().toSeconds()) * 100;
                    progressSlider.setValue(progress);
                }

                highlightLineAtTime(currentSec);
            });
        };
        mediaPlayer.currentTimeProperty().addListener(audioSyncListener);
    }

    private void highlightLineAtTime(double timeSec) {
        TranscriptLine match = TranscriptLine.findLineAtTime(lines, timeSec);
        if (match == null) return;

        int newIdx = lines.indexOf(match);
        if (newIdx == currentHighlightIdx) return;

        // Clear previous audio highlight
        if (currentHighlightIdx >= 0 && currentHighlightIdx < linesContainer.getChildren().size()) {
            HBox prevBox = (HBox) linesContainer.getChildren().get(currentHighlightIdx);
            TranscriptLine prevLine = lines.get(currentHighlightIdx);
            if (currentHighlightIdx != quoteHighlightIdx) {
                prevBox.setStyle(getDefaultStyle(prevLine));
                resetLineTextStyle(prevBox, prevLine, false);
            }
        }

        currentHighlightIdx = newIdx;
        if (newIdx >= 0 && newIdx < linesContainer.getChildren().size()) {
            HBox box = (HBox) linesContainer.getChildren().get(newIdx);
            if (newIdx != quoteHighlightIdx) {
                box.setStyle("-fx-cursor: hand; -fx-background-color: rgba(30,63,174,0.08); -fx-background-radius: 8;");
            }
            setLineTextBold(box, true);
            scrollToLine(newIdx);
        }
    }

    private String getDefaultStyle(TranscriptLine line) {
        if (line.isNarrator()) return "-fx-cursor: hand; -fx-background-color: #f8fafc;";
        return "-fx-cursor: hand; -fx-background-color: transparent;";
    }

    private void setLineTextBold(HBox box, boolean bold) {
        for (var node : box.getChildren()) {
            if (node instanceof VBox vbox) {
                for (var child : vbox.getChildren()) {
                    if (child instanceof Label label && !label.getStyle().contains("#7c3aed") && !label.getStyle().contains("#94a3b8")) {
                        if (bold) {
                            label.setStyle("-fx-font-size: 13.5px; -fx-line-spacing: 2; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
                        } else {
                            label.setStyle("-fx-font-size: 13.5px; -fx-line-spacing: 2; -fx-text-fill: #334155;");
                        }
                    }
                }
            }
        }
    }

    private void resetLineTextStyle(HBox box, TranscriptLine line, boolean isQuoteHighlight) {
        for (var node : box.getChildren()) {
            if (node instanceof VBox vbox) {
                for (var child : vbox.getChildren()) {
                    if (child instanceof Label label && !label.getStyle().contains("#7c3aed") && !label.getStyle().contains("#94a3b8")) {
                        if (isQuoteHighlight) {
                            label.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #0f172a; -fx-line-spacing: 2;");
                        } else if (line.isNarrator()) {
                            label.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-style: italic; -fx-line-spacing: 2;");
                        } else {
                            label.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #334155; -fx-line-spacing: 2;");
                        }
                    }
                }
            }
        }
    }

    public void highlightQuote(String quote) {
        if (quote == null || quote.isEmpty()) return;

        // Clear previous quote highlight
        if (quoteHighlightIdx >= 0 && quoteHighlightIdx < linesContainer.getChildren().size()) {
            HBox prevBox = (HBox) linesContainer.getChildren().get(quoteHighlightIdx);
            TranscriptLine prevLine = lines.get(quoteHighlightIdx);
            if (quoteHighlightIdx != currentHighlightIdx) {
                prevBox.setStyle(getDefaultStyle(prevLine));
                resetLineTextStyle(prevBox, prevLine, false);
            }
            quoteHighlightIdx = -1;
        }

        TranscriptLine match = TranscriptLine.findLineContainingQuote(lines, quote);
        if (match == null) return;

        int idx = lines.indexOf(match);
        quoteHighlightIdx = idx;

        if (idx >= 0 && idx < linesContainer.getChildren().size()) {
            HBox box = (HBox) linesContainer.getChildren().get(idx);
            box.setStyle("-fx-cursor: hand; -fx-background-color: #fef9c3; -fx-background-radius: 8; -fx-border-color: #fde68a; -fx-border-radius: 8; -fx-border-width: 1;");
            setLineTextBold(box, true);
            scrollToLine(idx);
        }
    }

    public void highlightQuoteAndPlay(String quote) {
        highlightQuote(quote);
        playQuoteSnippet(quote);
    }

    public void playQuoteSnippet(String quote) {
        if (mediaPlayer == null || quote == null || quote.isEmpty()) return;

        Runnable doPlay = () -> {
            if (mediaPlayer.getTotalDuration() == null || mediaPlayer.getTotalDuration().isUnknown()) return;
            double totalSec = mediaPlayer.getTotalDuration().toSeconds();

            double startSec = TranscriptLine.estimateTimeForQuote(lines, quote, totalSec);
            double endSec = TranscriptLine.estimateEndTimeForQuote(lines, quote, totalSec);

            startSec = Math.max(0, Math.min(startSec, totalSec));
            endSec = Math.max(startSec + 1, Math.min(endSec, totalSec));

            double startMs = startSec * 1000.0;
            double stopAtMs = endSec * 1000.0;

            removeSnippetStop();

            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
            }

            mediaPlayer.seek(Duration.millis(startMs));

            snippetStopListener = (obs, oldTime, newTime) -> {
                if (newTime != null && newTime.toMillis() >= stopAtMs) {
                    Platform.runLater(() -> {
                        mediaPlayer.pause();
                        removeSnippetStop();
                    });
                }
            };
            mediaPlayer.currentTimeProperty().addListener(snippetStopListener);
            mediaPlayer.play();
        };

        if (mediaPlayer.getStatus() == MediaPlayer.Status.UNKNOWN) {
            mediaPlayer.setOnReady(doPlay::run);
        } else {
            doPlay.run();
        }
    }

    public void seekToTime(double timeSec) {
        if (mediaPlayer == null) return;

        Runnable doSeek = () -> {
            if (mediaPlayer.getTotalDuration() == null || mediaPlayer.getTotalDuration().isUnknown()) return;
            removeSnippetStop();
            if (mediaPlayer.getStatus() != MediaPlayer.Status.PLAYING) {
                mediaPlayer.seek(Duration.seconds(timeSec));
                mediaPlayer.play();
            } else {
                mediaPlayer.seek(Duration.seconds(timeSec));
            }
        };

        if (mediaPlayer.getStatus() == MediaPlayer.Status.UNKNOWN) {
            mediaPlayer.setOnReady(doSeek::run);
        } else {
            doSeek.run();
        }
    }

    private void scrollToLine(int lineIdx) {
        if (scrollPane == null || linesContainer.getChildren().isEmpty()) return;
        Platform.runLater(() -> {
            if (lineIdx >= 0 && lineIdx < linesContainer.getChildren().size()) {
                var node = linesContainer.getChildren().get(lineIdx);
                double totalHeight = linesContainer.getBoundsInLocal().getHeight();
                double nodeY = node.getBoundsInParent().getMinY();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();
                if (totalHeight > viewportHeight) {
                    double target = (nodeY - viewportHeight / 3.0) / (totalHeight - viewportHeight);
                    scrollPane.setVvalue(Math.max(0, Math.min(1, target)));
                }
            }
        });
    }

    public void dispose() {
        if (mediaPlayer != null && audioSyncListener != null) {
            mediaPlayer.currentTimeProperty().removeListener(audioSyncListener);
        }
        removeSnippetStop();
    }

    public List<TranscriptLine> getLines() { return lines; }
    public ScrollPane getScrollPane() { return scrollPane; }
    public MediaPlayer getMediaPlayer() { return mediaPlayer; }

    public void setMediaPlayer(MediaPlayer mp) {
        dispose();
        this.mediaPlayer = mp;
        if (mp != null) {
            setupAudioSync();
            buildUI();
        }
    }
}
