package com.akatsuki.model;

import java.util.List;
import java.util.ArrayList;

public class Question {
    private int id;
    private int sectionId;
    private int questionNumber;
    private String questionText;
    private String correctAnswer;
    private String explanation;
    private String questionType;
    private List<String> options;
    private String transcriptQuote;
    private String partLabel;

    public Question() {
        this.options = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getSectionId() { return sectionId; }
    public void setSectionId(int sectionId) { this.sectionId = sectionId; }
    public int getQuestionNumber() { return questionNumber; }
    public void setQuestionNumber(int questionNumber) { this.questionNumber = questionNumber; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options != null ? options : new ArrayList<>(); }
    public String getTranscriptQuote() { return transcriptQuote; }
    public void setTranscriptQuote(String transcriptQuote) { this.transcriptQuote = transcriptQuote; }
    public String getPartLabel() { return partLabel; }
    public void setPartLabel(String partLabel) { this.partLabel = partLabel; }

    public String getNormalizedType() {
        if (questionType == null) return "fill_blank";
        if ("form_completion".equals(questionType)) return "table_completion";
        return questionType;
    }

    public static String getTypeLabel(String type) {
        if (type == null) return "Unknown";
        return switch (type) {
            case "mcq" -> "Multiple Choice";
            case "matching" -> "Matching";
            case "true_false" -> "True/False";
            case "true_false_ng" -> "T/F/Not Given";
            case "fill_blank" -> "Fill in Blank";
            case "sentence_completion" -> "Sentence Completion";
            case "short_answer" -> "Short Answer";
            case "form_completion", "table_completion" -> "Table/Form Completion";
            case "note_completion" -> "Note Completion";
            default -> type;
        };
    }

    public static boolean isAnswerCorrect(String userAnswer, String correctAnswer, String qType) {
        if (userAnswer == null || correctAnswer == null || userAnswer.isBlank() || correctAnswer.isBlank())
            return false;
        String ua = userAnswer.trim(), ca = correctAnswer.trim();
        String t = qType;
        if ("form_completion".equals(t)) t = "table_completion";
        if (t == null) t = "fill_blank";

        if ("mcq".equals(t) || "matching".equals(t))
            return Character.toUpperCase(ua.charAt(0)) == Character.toUpperCase(ca.charAt(0));
        if ("true_false".equals(t) || "true_false_ng".equals(t))
            return ua.equalsIgnoreCase(ca);
        return ua.toLowerCase().replaceAll("\\s+", " ").equals(ca.toLowerCase().replaceAll("\\s+", " "));
    }

    public static String getOptionText(List<String> options, String letter) {
        if (options == null || letter == null || letter.isEmpty()) return letter != null ? letter : "";
        for (String o : options) {
            if (!o.isEmpty() && Character.toUpperCase(o.charAt(0)) == Character.toUpperCase(letter.charAt(0)))
                return o;
        }
        return letter;
    }
}
