package com.example.quiz.repository;

import com.example.quiz.domain.User;
import com.example.quiz.dtos.LeaderboardDTO;
import com.example.quiz.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByUserId(Long userId);
    List<Quiz> findByProgrammingLanguage(String programmingLanguage);
    Quiz findByIdAndUserId(Long id, Long userId);

    @Query("""
        SELECT new com.example.quiz.dtos.LeaderboardDTO(
            q.user.id,
            q.user.fullName,
            AVG(q.score)
        )
        FROM Quiz q
        WHERE q.score IS NOT NULL
        GROUP BY q.user.id, q.user.fullName
        ORDER BY AVG(q.score) DESC
    """)
    List<LeaderboardDTO> findLeaderboardByAverageScore();

}
