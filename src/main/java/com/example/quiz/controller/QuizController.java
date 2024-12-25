//package com.example.quiz.controller;
//
//import com.example.quiz.model.Question;
//import com.example.quiz.model.Quiz;
//import com.example.quiz.model.QuizResult;
//import com.example.quiz.model.UserResponse;
//import com.example.quiz.service.QuizService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Controller
//@RequestMapping("/quiz")
//public class QuizController {
//
//    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);
//
//    private final QuizService quizService;
//
//    @Autowired
//    public QuizController(QuizService quizService) {
//        this.quizService = quizService;
//    }
//
//    @GetMapping
//    public String getQuizForm() {
//        return "index"; // Renders the index.html page
//    }
//
//    @PostMapping("/generate")
//    public String generateQuiz(
//            @RequestParam String programmingLanguage,
//            @RequestParam String difficulty,
//            Model model) {
//
//        logger.info("Generating quiz for language={} and difficulty={}", programmingLanguage, difficulty);
//
//        String prompt = String.format(
//                "Generate a multiple-choice programming quiz as a valid JSON array. The format should be strictly as follows, with no extra commas or malformed syntax: " +
//                        "[{\"question\":\"<question_text>\",\"options\":{\"A\":\"<option_A>\",\"B\":\"<option_B>\",\"C\":\"<option_C>\",\"D\":\"<option_D>\"},\"correctAnswer\":\"<correct_option>\"}, " +
//                        "{\"question\":\"<next_question_text>\",\"options\":{\"A\":\"<next_option_A>\",\"B\":\"<next_option_B>\",\"C\":\"<next_option_C>\",\"D\":\"<next_option_D>\"},\"correctAnswer\":\"<next_correct_option>\"}, ...]. " +
//                        "Ensure that all keys and string values are enclosed in double quotes, there are no extra commas, and the JSON array is valid and well-formed. " +
//                        "The quiz should cover the %s language at %s difficulty level and contain exactly 10 questions. Do not include any additional explanation or text outside of the JSON format.",
//                programmingLanguage, difficulty
//        );
//
//        try {
//            // Fetch and parse the quiz using QuizService
//            List<Question> questions = quizService.fetchAndParseQuiz(prompt);
//
//            if (questions == null || questions.isEmpty()) {
//                logger.warn("Failed to parse questions or received an empty list.");
//                return "error"; // Handle error case
//            }
//
//            // Create a Quiz model and add it to the view
//            Quiz quiz = new Quiz(questions);
//            quizService.setCurrentQuiz(quiz);
//
//            model.addAttribute("quiz", quiz);
//
//            logger.info("Successfully generated and parsed quiz.");
//            return "quiz"; // Renders the quiz.html page
//
//        } catch (Exception e) {
//            logger.error("Error generating or parsing quiz: {}", e.getMessage(), e);
//            return "error"; // Handle exception case
//        }
//    }
//
//        @PostMapping("/submit")
//        public String submitQuiz(@RequestParam Map<String, String> responses, Model model) {
//            logger.info("Processing quiz submission: {}", responses);
//
//            // Fetch the quiz object from the session or database
//            Quiz quiz = quizService.getCurrentQuiz(); // Add logic to retrieve the quiz object
//            if (quiz == null) {
//                logger.error("Quiz object is null.");
//                return "error";
//            }
//
//            // Calculate the score and corrections
//            QuizResult result = quizService.evaluateQuiz(quiz, new UserResponse(responses));
//            int totalQuestions = quiz.getQuestions().size();
//            double scorePercentage = (result.getScore() / (double) totalQuestions) * 100;
//
//            // Add the quiz questions, user responses, and correct answers for feedback display
//            model.addAttribute("quiz", quiz);
//            System.out.println("----responses are ----- : "+responses);
//            // Add results to the model
//            model.addAttribute("scorePercentage", Math.round(scorePercentage));
//            model.addAttribute("corrections", result.getDetailedFeedback());
//            Map<Integer, String> normalizedResponses = new HashMap<>();
//            for (Map.Entry<String, String> entry : responses.entrySet()) {
//                // Extract numeric index from "question_0", "question_1", etc.
//                String key = entry.getKey();
//                int index = Integer.parseInt(key.split("_")[1]);
//                normalizedResponses.put(index, entry.getValue());
//            }
//            model.addAttribute("responses", normalizedResponses);
//
//            logger.info("User Responses: {}", responses);
//            logger.info("Quiz Questions: {}", quiz.getQuestions());
//            logger.info("Correct Answers: {}", quiz.getQuestions().stream()
//                    .map(Question::getCorrectAnswer)
//                    .toList());
//            logger.info("Score Percentage: {}", scorePercentage);
//
//
//            return "result"; // Redirect to result.html
//        }
//
//}

package com.example.quiz.controller;

import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.model.QuizResult;
import com.example.quiz.model.UserResponse;
import com.example.quiz.service.QuizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);
    @Autowired
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // Endpoint to get the quiz form
    @GetMapping
    public ResponseEntity<String> getQuizForm() {
        return ResponseEntity.ok("Welcome to the Quiz API. Use POST /quiz/generate to generate a quiz.");
    }

    // Endpoint to generate a quiz
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(
            @RequestParam String programmingLanguage,
            @RequestParam String difficulty) {

        logger.info("Generating quiz for language={} and difficulty={}", programmingLanguage, difficulty);

        String prompt = String.format(
                "Generate a multiple-choice programming quiz as a valid JSON array. The format should be strictly as follows: " +
                        "[{\"question\":\"<question_text>\",\"options\":{\"A\":\"<option_A>\",\"B\":\"<option_B>\",\"C\":\"<option_C>\",\"D\":\"<option_D>\"},\"correctAnswer\":\"<correct_option>\"}, " +
                        "{\"question\":\"<next_question_text>\",\"options\":{\"A\":\"<next_option_A>\",\"B\":\"<next_option_B>\",\"C\":\"<next_option_C>\",\"D\":\"<next_option_D>\"},\"correctAnswer\":\"<next_correct_option>\"}, ...]. " +
                        "Ensure that all keys and string values are enclosed in double quotes, there are no extra commas, and the JSON array is valid and well-formed. " +
                        "Ensure that every 'correctAnswer' key is properly enclosed with double quotes and no key-value pairs are omitted or malformed. " +
                        "The quiz should cover the %s language at %s difficulty level, contain exactly 10 questions, and start directly with a JSON array that begins with [ and contains no additional text before or after the JSON array itself. " +
                        "Do not add any text before or after the JSON array, and ensure it starts immediately with the opening bracket [.",
                programmingLanguage, difficulty
        );



        try {
            // Fetch and parse the quiz using QuizService
            List<Question> questions = quizService.fetchAndParseQuiz(prompt);

            if (questions == null || questions.isEmpty()) {
                logger.warn("Failed to parse questions or received an empty list.");
                return ResponseEntity.status(500).body("Error generating quiz.");
            }

            // Create a Quiz model and set it to the service (optional based on your requirement)
            Quiz quiz = new Quiz(questions);
            quizService.setCurrentQuiz(quiz);

            logger.info("Successfully generated and parsed quiz.");
            return ResponseEntity.ok(quiz); // Returning the quiz object in the response

        } catch (Exception e) {
            logger.error("Error generating or parsing quiz: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error generating quiz.");
        }
    }

    // Endpoint to submit the quiz
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestParam Map<String, String> responses) {
        logger.info("Processing quiz submission: {}", responses);

        // Fetch the quiz object (you may need to implement session-based retrieval or a more permanent solution)
        Quiz quiz = quizService.getCurrentQuiz(); // Add logic to retrieve the quiz object
        if (quiz == null) {
            logger.error("Quiz object is null.");
            return ResponseEntity.status(400).body("Quiz not found.");
        }

        // Calculate the score and corrections
        QuizResult result = quizService.evaluateQuiz(quiz, new UserResponse(responses));
        int totalQuestions = quiz.getQuestions().size();
        double scorePercentage = (result.getScore() / (double) totalQuestions) * 100;

        // Prepare the response data
        Map<String, Object> response = new HashMap<>();
        response.put("scorePercentage", Math.round(scorePercentage));
        response.put("corrections", result.getDetailedFeedback());

        Map<Integer, String> normalizedResponses = new HashMap<>();
        for (Map.Entry<String, String> entry : responses.entrySet()) {
            String key = entry.getKey();
            int index = Integer.parseInt(key.split("_")[1]);
            normalizedResponses.put(index, entry.getValue());
        }
        response.put("responses", normalizedResponses);

        logger.info("User Responses: {}", responses);
        logger.info("Quiz Questions: {}", quiz.getQuestions());
        logger.info("Correct Answers: {}", quiz.getQuestions().stream()
                .map(Question::getCorrectAnswer)
                .toList());
        logger.info("Score Percentage: {}", scorePercentage);

        return ResponseEntity.ok(response); // Return the result data as JSON
    }
}

