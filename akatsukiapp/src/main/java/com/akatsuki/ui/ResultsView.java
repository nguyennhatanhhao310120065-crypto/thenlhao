package com.akatsuki.ui;

import com.akatsuki.database.DatabaseManager;
import com.akatsuki.model.StudentResult;
import com.akatsuki.model.User;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ResultsView extends VBox {
    private final User user;
    private VBox listBox;

    public ResultsView(User user) {
        this.user = user;
        setSpacing(0);
        buildUI();
        loadResults();
    }

    private void buildUI() {
        VBox header = new VBox(8);
        header.setPadding(new Insets(0, 0, 32, 0));
        Label title = new Label("My Progress");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: 700;");
        Label subtitle = new Label("Track your performance over time.");
        subtitle.setStyle("-fx-text-fill: #64748b;");
        header.getChildren().addAll(title, subtitle);

        listBox = new VBox(0);
        listBox.getStyleClass().add("results-table-wrap");

        HBox tableHeader = new HBox();
        tableHeader.setStyle("-fx-background-color: #f8fafc; -fx-padding: 16 24; -fx-border-color: transparent transparent #e2e8f0 transparent; -fx-border-width: 0 0 1 0;");
        Label h1 = new Label("TEST NAME");
        h1.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-pref-width: 300;");
        Label h2 = new Label("EXAM TYPE");
        h2.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-pref-width: 120;");
        Label h3 = new Label("DATE");
        h3.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-pref-width: 150;");
        Label h4 = new Label("SCORE");
        h4.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-pref-width: 100; -fx-alignment: center-right;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        tableHeader.getChildren().addAll(h1, h2, h3, spacer, h4);
        listBox.getChildren().add(tableHeader);

        ScrollPane scrollPane = new ScrollPane(listBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-border-radius: 16;");

        getChildren().addAll(header, scrollPane);
    }

    private void loadResults() {
        while (listBox.getChildren().size() > 1) listBox.getChildren().remove(1);

        List<StudentResult> results = DatabaseManager.getInstance().getResults(user.getId());

        if (results.isEmpty()) {
            Label empty = new Label("No results yet.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 500; -fx-padding: 60; -fx-font-size: 16px;");
            empty.setAlignment(Pos.CENTER);
            listBox.getChildren().add(empty);
            return;
        }

        for (StudentResult r : results) {
            HBox row = new HBox();
            row.setStyle("-fx-padding: 16 24; -fx-border-color: transparent transparent #f1f5f9 transparent; -fx-border-width: 0 0 1 0;");
            row.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(r.getBankName());
            name.setStyle("-fx-font-weight: 700; -fx-text-fill: #334155; -fx-pref-width: 300;");

            Label type = new Label(r.getExamType());
            type.getStyleClass().addAll("badge", "badge-slate");
            HBox typeBox = new HBox(type);
            typeBox.setPrefWidth(120);

            Label date = new Label(r.getCompletedAt() != null ? r.getCompletedAt().substring(0, Math.min(10, r.getCompletedAt().length())) : "");
            date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-pref-width: 150;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            VBox scoreBox = new VBox(2);
            scoreBox.setAlignment(Pos.CENTER_RIGHT);
            scoreBox.setPrefWidth(100);
            Label score = new Label(String.format("%.1f/10", r.getScore()));
            score.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: " + (r.getScore() >= 7 ? "#059669" : "#0f172a") + ";");
            if (r.getCorrectCount() > 0) {
                Label detail = new Label(r.getCorrectCount() + "/" + r.getTotalQuestions() + " correct");
                detail.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
                scoreBox.getChildren().addAll(score, detail);
            } else {
                scoreBox.getChildren().add(score);
            }

            row.getChildren().addAll(name, typeBox, date, spacer, scoreBox);
            listBox.getChildren().add(row);
        }
    }

    public void refresh() {
        loadResults();
    }
}
