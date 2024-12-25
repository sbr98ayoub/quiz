package com.example.quiz.model;

import java.util.List;
import java.util.Map;

public class QuizResult {
    private int score;
    private Map<Integer, String> corrections; // Key: Question index, Value: Correct answer
    private List<String> detailedFeedback;

    public QuizResult(int score, Map<Integer, String> corrections, List<String> detailedFeedback) {
        this.score = score;
        this.corrections = corrections;
        this.detailedFeedback = detailedFeedback;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Map<Integer, String> getCorrections() {
        return corrections;
    }

    public void setCorrections(Map<Integer, String> corrections) {
        this.corrections = corrections;
    }

    public List<String> getDetailedFeedback() {
        return detailedFeedback;
    }

    public void setDetailedFeedback(List<String> detailedFeedback) {
        this.detailedFeedback = detailedFeedback;
    }
}
