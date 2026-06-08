package com.akatsuki.model;

/** A single row in a test's leaderboard: a student's best attempt. */
public class RankingEntry {
    private final int rank;
    private final String username;
    private final double bestScore;
    private final int correctCount;
    private final int totalQuestions;
    private final int attempts;
    private final String completedAt;

    public RankingEntry(int rank, String username, double bestScore, int correctCount,
                        int totalQuestions, int attempts, String completedAt) {
        this.rank = rank;
        this.username = username;
        this.bestScore = bestScore;
        this.correctCount = correctCount;
        this.totalQuestions = totalQuestions;
        this.attempts = attempts;
        this.completedAt = completedAt;
    }

    public int getRank() { return rank; }
    public String getUsername() { return username; }
    public double getBestScore() { return bestScore; }
    public int getCorrectCount() { return correctCount; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getAttempts() { return attempts; }
    public String getCompletedAt() { return completedAt; }
}
