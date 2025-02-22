package com.example.quiz.dtos;

public class LeaderboardDTO {
    private Long userId;
    private String fullName;
    private Double averageScore;

    // Constructors
    public LeaderboardDTO(Long userId, String fullName, Double averageScore) {
        this.userId = userId;
        this.fullName = fullName;
        this.averageScore = averageScore;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public Double getAverageScore() {
        return averageScore;
    }
}
