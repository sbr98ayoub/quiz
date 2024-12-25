package com.example.quiz.controller;

import com.example.quiz.model.Quiz;
import com.example.quiz.model.UserResponse;
import com.example.quiz.model.QuizResult;
import com.example.quiz.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/quiz")
public class ResultController {

    @Autowired
    private QuizService quizService;

    @PostMapping("/result")
    public String submitQuiz(
            @ModelAttribute Quiz quiz,
            @RequestParam Map<String, String> userResponses,
            Model model) {

        // Convert userResponses to UserResponse object
        UserResponse userResponse = new UserResponse(userResponses);

        // Evaluate the quiz
        QuizResult quizResult = quizService.evaluateQuiz(quiz, userResponse);

        // Add result data to the model
        model.addAttribute("result", quizResult);
        return "result"; // Renders the result.html page
    }
}
