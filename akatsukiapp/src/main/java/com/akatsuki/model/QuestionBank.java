package com.akatsuki.model;

import java.util.List;
import java.util.ArrayList;

public class QuestionBank {
    private int id;
    private String bankName;
    private int createdBy;
    private boolean isPublic;
    private String examType;
    private String audioUrl;
    private String transcript;
    private double startTime;
    private double endTime;
    private String createdAt;
    private String creatorName;
    private int questionCount;
    private List<Section> sections;

    public QuestionBank() {
        this.sections = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public double getStartTime() { return startTime; }
    public void setStartTime(double startTime) { this.startTime = startTime; }
    public double getEndTime() { return endTime; }
    public void setEndTime(double endTime) { this.endTime = endTime; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public List<Section> getSections() { return sections; }
    public void setSections(List<Section> sections) { this.sections = sections != null ? sections : new ArrayList<>(); }

    public List<Question> getAllQuestions() {
        List<Question> all = new ArrayList<>();
        for (Section s : sections) all.addAll(s.getQuestions());
        return all;
    }
}
