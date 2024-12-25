package com.example.quiz.model;

import java.util.Map;

public class UserResponse {
    private Map<String, String> userAnswers; // Key: Question index, Value: Selected answer

    public UserResponse(Map<String, String> userAnswers) {
        this.userAnswers = userAnswers;
    }

    public Map<String, String> getUserAnswers() {
        return userAnswers;
    }

    public void setUserAnswers(Map<String, String> userAnswers) {
        this.userAnswers = userAnswers;
    }
}
