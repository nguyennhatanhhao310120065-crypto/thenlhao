package com.akatsuki.model;

public class StudentResult {
    private int id;
    private int userId;
    private int bankId;
    private double score;
    private int totalQuestions;
    private int correctCount;
    private String completedAt;
    private String bankName;
    private String examType;

    public StudentResult() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getBankId() { return bankId; }
    public void setBankId(int bankId) { this.bankId = bankId; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
}
