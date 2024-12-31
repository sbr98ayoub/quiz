package com.example.quiz.repository;

import com.example.quiz.domain.User;
import com.example.quiz.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByUserId(Long userId);
    List<Quiz> findByProgrammingLanguage(String programmingLanguage);
    Quiz findByIdAndUserId(Long id, Long userId);

}
