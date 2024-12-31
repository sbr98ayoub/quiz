package com.example.quiz.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "quiz_results")
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private int score;

    @ElementCollection
    @CollectionTable(name = "quiz_corrections", joinColumns = @JoinColumn(name = "result_id"))
    @MapKeyColumn(name = "question_index")
    @Column(name = "correct_answer")
    private Map<Integer, String> corrections;

    @ElementCollection
    @CollectionTable(name = "quiz_feedback", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "feedback")
    private List<String> detailedFeedback;

    public QuizResult() {}

    public QuizResult(Quiz quiz, int score, Map<Integer, String> corrections, List<String> detailedFeedback) {
        this.quiz = quiz;
        this.score = score;
        this.corrections = corrections;
        this.detailedFeedback = detailedFeedback;
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
