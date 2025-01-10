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
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/quiz")
@CrossOrigin(origins = "http://localhost:3000")
public class QuizController {

    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);

    @Autowired
    private final QuizService quizService;

    @Autowired
    private final UserService userService;

    public QuizController(QuizService quizService, UserService userService) {
        this.quizService = quizService;
        this.userService = userService;
    }

    // A simple welcome or test endpoint
    @GetMapping
    public ResponseEntity<String> getQuizForm() {
        return ResponseEntity.ok("Welcome to the Quiz API. Use POST /quiz/generate to generate a quiz.");
    }

    /**
     * 1) Generate quiz from LLaMA or similar AI
     * 2) Parse the returned JSON and create ephemeral IDs for each question
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateQuiz(@RequestBody Map<String, String> requestBody) {
        String programmingLanguage = requestBody.get("programmingLanguage");
        String difficulty = requestBody.get("difficulty");
        logger.info("Generating quiz for language={} and difficulty={}", programmingLanguage, difficulty);

        // This is your original prompt. Keep it as strict as you need.
        String prompt = String.format(
                "Generate a multiple-choice programming quiz as a valid JSON array. The format should be strictly as follows, with no extra commas or malformed syntax: " +
                        "[{\"question\":\"<question_text>\",\"options\":{\"A\":\"<option_A>\",\"B\":\"<option_B>\",\"C\":\"<option_C>\",\"D\":\"<option_D>\"},\"correctAnswer\":\"<correct_option>\"}, " +
                        "{\"question\":\"<next_question_text>\",\"options\":{\"A\":\"<next_option_A>\",\"B\":\"<next_option_B>\",\"C\":\"<next_option_C>\",\"D\":\"<next_option_D>\"},\"correctAnswer\":\"<next_correct_option>\"}, ...]. " +
                        "Ensure that all keys and string values are enclosed in double quotes, there are no extra commas, and the JSON array is valid and well-formed. " +
                        "The quiz should cover the %s language at %s difficulty level and contain exactly 10 questions. Do not include any additional explanation or text outside of the JSON format.",
                programmingLanguage, difficulty
        );

        try {
            // Use the service to call LLaMA (or any other AI endpoint) and parse the JSON
            List<Question> questions = quizService.fetchAndParseQuiz(prompt);
            if (questions == null || questions.isEmpty()) {
                logger.warn("Failed to parse questions or received an empty list.");
                return ResponseEntity.status(500).body("Error generating quiz.");
            }

            // (Approach A) Assign ephemeral (in-memory) IDs so the front end can map answers.
            long tempId = 1L;
            for (Question q : questions) {
                q.setId(tempId++);
            }

            // Create the Quiz object in memory
            Quiz quiz = new Quiz();
            quiz.setProgrammingLanguage(programmingLanguage);
            quiz.setQuestions(questions);

            // Save it in memory only (do NOT persist). If you DO want to persist now, see Approach B
            quizService.setCurrentQuiz(quiz);

            logger.info("Successfully generated and parsed quiz.");
            return ResponseEntity.ok(quiz); // returning the in-memory quiz
        } catch (Exception e) {
            logger.error("Error generating or parsing quiz: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error generating quiz.");
        }
    }

    /**
     * Submit quiz answers
     * The front end calls /quiz/submit with `id = userId` plus all question_id=answer
     * For example: POST /quiz/submit?id=1&1=A&2=B&3=A ...
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestParam Map<String, String> responses, @RequestParam Long id) {
        logger.info("Processing quiz submission for user ID: {}", id);
        // The "id" param is the userId, but we want to remove it from the map so it doesn't mess up mapping
        responses.remove("id");
        logger.info("Received responses: {}", responses);

        // 1) Retrieve the in-memory quiz
        Quiz quiz = quizService.getCurrentQuiz();
        if (quiz == null) {
            logger.error("Quiz object is null.");
            return ResponseEntity.status(400).body("Quiz not found.");
        }

        // 2) Validate user
        User user = userService.getUserById(id);
        if (user == null) {
            logger.error("User with ID {} not found.", id);
            return ResponseEntity.status(404).body("User not found.");
        }
        quiz.setUser(user);

        // 3) Re-map the responses to match ephemeral question IDs
        Map<String, String> mappedResponses = new HashMap<>();
        for (Question question : quiz.getQuestions()) {
            // question.getId() is a Long we assigned in memory
            String key = String.valueOf(question.getId());
            if (responses.containsKey(key)) {
                mappedResponses.put(key, responses.get(key));
            }
        }
        logger.info("Mapped responses to question IDs: {}", mappedResponses);

        // 4) Set userResponse for each question
        for (Question question : quiz.getQuestions()) {
            String userAnswer = mappedResponses.get(String.valueOf(question.getId()));
            if (userAnswer != null) {
                question.setUserResponse(userAnswer);
                logger.info("Setting user response for Question ID {}: {}", question.getId(), userAnswer);
            }
        }

        // 5) Evaluate the quiz
        //    In memory we rely on question.getId() not being null
        UserResponse userResponse = new UserResponse(quiz, mappedResponses);
        QuizResult result = quizService.evaluateQuiz(quiz, userResponse);

        // 6) Build the response
        int totalQuestions = quiz.getQuestions().size();
        double scorePercentage = (result.getScore() / (double) totalQuestions) * 100;
        quiz.setScore(scorePercentage);

        // Build corrections
        List<Map<String, String>> corrections = quiz.getQuestions().stream()
                .map(question -> {
                    Map<String, String> corr = new HashMap<>();
                    corr.put("question", question.getQuestion());
                    corr.put("yourAnswer", question.getUserResponse() != null ? question.getUserResponse() : "N/A");
                    corr.put("correctAnswer", question.getCorrectAnswer());
                    return corr;
                })
                .collect(Collectors.toList());

        // Return JSON
        Map<String, Object> response = Map.of(
                "scorePercentage", Math.round(scorePercentage),
                "corrections", corrections
        );
        logger.info("Quiz submitted successfully for user ID: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Store the quiz in the DB. Approach A: we set question.setId(null) so the DB
     * will treat them as brand-new rows.
     */
    @PostMapping("/store")
    public ResponseEntity<?> storeQuiz(
            @RequestParam Long userId,
            @RequestParam String programmingLanguage,
            @RequestBody List<Question> userAnswers) {

        if (userAnswers == null || userAnswers.isEmpty()) {
            return ResponseEntity.badRequest().body("User answers are missing or empty.");
        }

        Quiz currentQuiz = quizService.getCurrentQuiz();
        if (currentQuiz == null) {
            return ResponseEntity.badRequest().body("No quiz to store.");
        }

        // Validate user
        User user = userService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        // Set quiz metadata
        currentQuiz.setUser(user);
        currentQuiz.setProgrammingLanguage(programmingLanguage);

        // Update questions from the request, then set the ID to null to force new inserts
        for (int i = 0; i < currentQuiz.getQuestions().size(); i++) {
            Question question = currentQuiz.getQuestions().get(i);
            Question userQ = userAnswers.get(i);

            // We forcibly set the question's ID to null so JPA inserts a new row
            question.setId(null);
            question.setUserResponse(userQ.getUserResponse());
            question.setQuiz(currentQuiz);
        }

        logger.info("Storing Quiz: ID = {}", currentQuiz.getId());
        for (Question question : currentQuiz.getQuestions()) {
            logger.info("Storing Question: ID = {}, User Response = {}",
                    question.getId(), question.getUserResponse());
        }

        // Persist the quiz with brand-new question rows
        try {
            quizService.saveQuizWithQuestions(currentQuiz);
            logger.info("Successfully stored Quiz: ID = {}", currentQuiz.getId());

            for (Question question : currentQuiz.getQuestions()) {
                logger.info("Stored Question: ID = {}, User Response = {}",
                        question.getId(), question.getUserResponse());
            }

            return ResponseEntity.ok("Quiz and responses stored successfully.");
        } catch (Exception e) {
            logger.error("Error storing quiz: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error storing quiz.");
        }
    }

    // GET quiz history
    @GetMapping("/history")
    public ResponseEntity<?> getQuizHistory(@RequestParam Long userId) {
        List<Quiz> quizzes = quizService.getQuizzesByUser(userId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Map<String, Object>> quizHistory = quizzes.stream().map(quiz -> {
            Map<String, Object> quizData = new HashMap<>();
            quizData.put("id", quiz.getId());
            String formattedDate = quiz.getCreatedAt().toLocalDate().format(formatter);
            quizData.put("date", formattedDate);
            quizData.put("score", quiz.getScore() != null ? quiz.getScore() : "Not Submitted");
            quizData.put("programmingLanguage", quiz.getProgrammingLanguage());
            return quizData;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(quizHistory);
    }

    // GET quiz details
    @GetMapping("/details")
    public ResponseEntity<?> getQuizDetails(@RequestParam Long quizId, @RequestParam Long userId) {
        // Fetch the quiz by ID and user ID
        Quiz quiz = quizService.getQuizById(quizId, userId);

        if (quiz == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("message", "Quiz not found for the given user."));
        }

        // Prepare question details including user responses
        List<Map<String, Object>> questionDetails = quiz.getQuestions().stream().map(question -> {
            Map<String, Object> questionData = new HashMap<>();
            questionData.put("question", question.getQuestion());

            // Map correctAnswer key to its option value
            String correctAnswerValue = question.getOptions().get(question.getCorrectAnswer());
            questionData.put("correctAnswer", correctAnswerValue);

            // Map userResponse key to its option value
            String userResponseValue = question.getUserResponse() != null
                    ? question.getOptions().get(question.getUserResponse())
                    : "N/A";
            questionData.put("userResponse", userResponseValue);

            logger.info("Question: {}, Correct Answer: {}, User Response: {}",
                    question.getQuestion(), correctAnswerValue, userResponseValue);

            return questionData;
        }).collect(Collectors.toList());

        // Return the details of the quiz questions
        return ResponseEntity.ok(questionDetails);
    }
}
