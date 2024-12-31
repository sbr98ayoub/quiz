package com.example.quiz.model;

import jakarta.persistence.*;
import java.util.Map;

@Entity
@Table(name = "user_responses")
public class UserResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ElementCollection
    @CollectionTable(name = "responses", joinColumns = @JoinColumn(name = "response_id"))
    @MapKeyColumn(name = "question_id")
    @Column(name = "response")
    private Map<String, String> userAnswers;

    public UserResponse() {}

    public UserResponse(Quiz quiz, Map<String, String> userAnswers) {
        this.quiz = quiz;
        this.userAnswers = userAnswers;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }

    public Map<String, String> getUserAnswers() {
        return userAnswers;
    }

    public void setUserAnswers(Map<String, String> userAnswers) {
        this.userAnswers = userAnswers;
    }
}
