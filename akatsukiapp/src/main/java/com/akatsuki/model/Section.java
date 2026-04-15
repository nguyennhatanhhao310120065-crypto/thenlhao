package com.akatsuki.model;

import java.util.List;
import java.util.ArrayList;

public class Section {
    private int id;
    private int bankId;
    private int sectionNumber;
    private String instruction;
    private List<Question> questions;

    public Section() {
        this.questions = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBankId() { return bankId; }
    public void setBankId(int bankId) { this.bankId = bankId; }
    public int getSectionNumber() { return sectionNumber; }
    public void setSectionNumber(int sectionNumber) { this.sectionNumber = sectionNumber; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions != null ? questions : new ArrayList<>(); }
}
