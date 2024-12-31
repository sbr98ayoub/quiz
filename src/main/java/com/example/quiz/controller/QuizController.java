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

import com.example.quiz.domain.User;
import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.model.QuizResult;
import com.example.quiz.model.UserResponse;
import com.example.quiz.service.QuizService;
import com.example.quiz.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/quiz")
@CrossOrigin(origins = "http://localhost:3000")
public class QuizController {

    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);
    @Autowired
    private final QuizService quizService;
    @Autowired
    private  final UserService userService ;


    public QuizController(QuizService quizService,UserService userService) {
        this.userService=userService;
        this.quizService = quizService;
    }

    // Endpoint to get the quiz form
    @GetMapping
    public ResponseEntity<String> getQuizForm() {
        return ResponseEntity.ok("Welcome to the Quiz API. Use POST /quiz/generate to generate a quiz.");
    }

    // Endpoint to generate a quiz
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(@RequestBody Map<String, String> requestBody) {
        String programmingLanguage = requestBody.get("programmingLanguage");
        String difficulty = requestBody.get("difficulty");
        logger.info("Generating quiz for language={} and difficulty={}", programmingLanguage, difficulty);

        String prompt = String.format(
               "Generate a multiple-choice programming quiz as a valid JSON array. The format should be strictly as follows, with no extra commas or malformed syntax: " +
                      "[{\"question\":\"<question_text>\",\"options\":{\"A\":\"<option_A>\",\"B\":\"<option_B>\",\"C\":\"<option_C>\",\"D\":\"<option_D>\"},\"correctAnswer\":\"<correct_option>\"}, " +
                        "{\"question\":\"<next_question_text>\",\"options\":{\"A\":\"<next_option_A>\",\"B\":\"<next_option_B>\",\"C\":\"<next_option_C>\",\"D\":\"<next_option_D>\"},\"correctAnswer\":\"<next_correct_option>\"}, ...]. " +
                        "Ensure that all keys and string values are enclosed in double quotes, there are no extra commas, and the JSON array is valid and well-formed. " +
                        "The quiz should cover the %s language at %s difficulty level and contain exactly 10 questions. Do not include any additional explanation or text outside of the JSON format.",
                programmingLanguage, difficulty
        );

        try {
            // Fetch and parse the quiz using QuizService
            List<Question> questions = quizService.fetchAndParseQuiz(prompt);

            if (questions == null || questions.isEmpty()) {
                logger.warn("Failed to parse questions or received an empty list.");
                return ResponseEntity.status(500).body("Error generating quiz.");
            }

            // Assign temporary IDs to questions
            long tempId = 1L;
            for (Question question : questions) {
                question.setId(tempId++);
            }

            // Create a Quiz model and set it to the service
            Quiz quiz = new Quiz();
            quiz.setQuestions(questions);
            quiz.setProgrammingLanguage(programmingLanguage);
            quizService.setCurrentQuiz(quiz);

            logger.info("Successfully generated and parsed quiz.");
            return ResponseEntity.ok(quiz); // Returning the quiz object in the response

        } catch (Exception e) {
            logger.error("Error generating or parsing quiz: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error generating quiz.");
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestParam Map<String, String> responses, @RequestParam Long id) {
        logger.info("Processing quiz submission for user ID: {}", id);
        responses.remove("id");
        logger.info("Received responses: {}", responses);

        // Fetch the quiz object
        Quiz quiz = quizService.getCurrentQuiz();
        if (quiz == null) {
            logger.error("Quiz object is null.");
            return ResponseEntity.status(400).body("Quiz not found.");
        }

        // Validate user
        User user = userService.getUserById(id);
        if (user == null) {
            logger.error("User with ID {} not found.", id);
            return ResponseEntity.status(404).body("User not found.");
        }
        quiz.setUser(user);

        // Map responses to question IDs
        Map<String, String> mappedResponses = new HashMap<>();
        for (Question question : quiz.getQuestions()) {
            String questionKey = String.valueOf(question.getId());
            if (responses.containsKey(questionKey)) {
                mappedResponses.put(questionKey, responses.get(questionKey));
            }
        }

        logger.info("Mapped responses to question IDs: {}", mappedResponses);
        // Map user responses to questions
        for (Question question : quiz.getQuestions()) {
            String userAnswer = responses.get(String.valueOf(question.getId()));
            if (userAnswer != null) {
                question.setUserResponse(userAnswer); // Set user response
                logger.info("Setting user response for Question ID {}: {}", question.getId(), userAnswer);
            }
        }

        // Calculate the score and corrections without saving data
        UserResponse userResponse = new UserResponse(quiz, mappedResponses);
        QuizResult result = quizService.evaluateQuiz(quiz, userResponse);

        int totalQuestions = quiz.getQuestions().size();
        double scorePercentage = (result.getScore() / (double) totalQuestions) * 100;
        quiz.setScore(scorePercentage);

        // Build correction details
        List<Map<String, String>> corrections = quiz.getQuestions().stream().map(question -> {
            String questionId = String.valueOf(question.getId());
            String userAnswer = mappedResponses.get(questionId);
            Map<String, String> correction = new HashMap<>();
            System.out.println("------- question and Answer and details : " + question.getQuestion() +
                    "  correct answer : " + question.getCorrectAnswer() +
                    "  user response  " + question.getUserResponse());
            correction.put("question", question.getQuestion());
            correction.put("yourAnswer", userAnswer != null ? userAnswer : "N/A");
            correction.put("correctAnswer", question.getCorrectAnswer());
            return correction;
        }).collect(Collectors.toList());

        // Prepare the response
        Map<String, Object> response = Map.of(
                "scorePercentage", Math.round(scorePercentage),
                "corrections", corrections
        );

        logger.info("Quiz submitted successfully for user ID: {}", id);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/store")
    public ResponseEntity<?> storeQuiz(
            @RequestParam Long userId,
            @RequestParam String programmingLanguage,
            @RequestBody Map<String, String> userAnswers) {

        if (userAnswers == null || userAnswers.isEmpty()) {
            logger.error("User answers are missing or empty.");
            return ResponseEntity.status(400).body("User answers are missing or empty.");
        }

        Quiz currentQuiz = quizService.getCurrentQuiz();
        if (currentQuiz == null) {
            logger.error("No current quiz found in session.");
            return ResponseEntity.status(400).body("No quiz to store.");
        }

        logger.info("Storing quiz for user ID: {}, programmingLanguage: {}", userId, programmingLanguage);

        // Retrieve the user
        User user = userService.getUserById(userId);
        if (user == null) {
            logger.error("User with ID {} not found.", userId);
            return ResponseEntity.status(404).body("User not found.");
        }

        // Attach user and programming language to the quiz
        currentQuiz.setUser(user);
        currentQuiz.setProgrammingLanguage(programmingLanguage);

        // Ensure questions are attached to the persistence context
        List<Question> questions = currentQuiz.getQuestions();
        List<Question> managedQuestions = new ArrayList<>();

        for (Question question : questions) {
            String userAnswer = userAnswers.get(String.valueOf(question.getId()));
            if (userAnswer != null) {
                question.setUserResponse(userAnswer); // Set the user's response in the Question entity
            }
            // Merge question to attach to the persistence context
            managedQuestions.add(quizService.mergeQuestion(question));
        }

        currentQuiz.setQuestions(managedQuestions);

        try {
            // Save the quiz
            quizService.saveQuiz(currentQuiz);

            return ResponseEntity.ok(Map.of(
                    "message", "Quiz and responses stored successfully.",
                    "quizId", currentQuiz.getId()
            ));
        } catch (Exception e) {
            logger.error("Error storing quiz or responses: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error storing quiz. Please try again.");
        }
    }












    @GetMapping("/history")
    public ResponseEntity<?> getQuizHistory(@RequestParam Long userId) {
        List<Quiz> quizzes = quizService.getQuizzesByUser(userId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Map<String, Object>> quizHistory = quizzes.stream().map(quiz -> {
            Map<String, Object> quizData = new HashMap<>();
            quizData.put("id", quiz.getId());
            String formattedDate = quiz.getCreatedAt().toLocalDate().format(formatter);
            quizData.put("date",formattedDate);
            quizData.put("score", quiz.getScore() != null ? quiz.getScore() : "Not Submitted");
            quizData.put("programmingLanguage", quiz.getProgrammingLanguage());
            return quizData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(quizHistory);
    }

    @GetMapping("/details")
    public ResponseEntity<?> getQuizDetails(@RequestParam Long quizId, @RequestParam Long userId) {
        // Fetch the quiz by ID and user ID
        Quiz quiz = quizService.getQuizById(quizId, userId);

        // If the quiz is not found, return a 404 status
        if (quiz == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Quiz not found for the given user."));
        }

        // Prepare question details including user responses
        List<Map<String, Object>> questionDetails = quiz.getQuestions().stream().map(question -> {
            Map<String, Object> questionData = new HashMap<>();
            questionData.put("question", question.getQuestion());

            // Map correctAnswer key to its option value
            String correctAnswerValue = question.getOptions().get(question.getCorrectAnswer());
            questionData.put("correctAnswer", correctAnswerValue);

            // Map userResponse key to its option value (if userResponse is not null)
            String userResponseValue = question.getUserResponse() != null ? question.getOptions().get(question.getUserResponse()) : "N/A";
            questionData.put("userResponse", userResponseValue);

            // Debugging log to ensure data is fetched correctly
            logger.info("Question: {}, Correct Answer: {}, User Response: {}",
                    question.getQuestion(), correctAnswerValue, userResponseValue);

            return questionData;
        }).collect(Collectors.toList());

        // Return the details of the quiz questions
        return ResponseEntity.ok(questionDetails);
    }





}

