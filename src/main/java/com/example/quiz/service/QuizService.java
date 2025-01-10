package com.example.quiz.service;

import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.model.QuizResult;
import com.example.quiz.model.UserResponse;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.repository.QuizRepository;
import com.example.quiz.repository.QuizResultRepository;
import com.example.quiz.repository.UserResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuizService {

    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);

    private static final String LLAMA_API_URL = "http://localhost:11434/api/generate";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private Quiz currentQuiz;
    private final QuizRepository quizRepository;
    private  final QuizResultRepository quizResultRepository ;
    private  final UserResponseRepository userResponseRepository;
    private final QuestionRepository questionRepository;

    public QuizService(ObjectMapper objectMapper, QuizRepository quizRepository, QuizResultRepository quizResultRepository, UserResponseRepository userResponseRepository, QuestionRepository questionRepository) {
        this.quizResultRepository = quizResultRepository;
        this.userResponseRepository = userResponseRepository;
        this.questionRepository = questionRepository;
        this.webClient = WebClient.builder()
                .baseUrl(LLAMA_API_URL)
                .build();
        this.objectMapper = objectMapper;
        this.currentQuiz = null;
        this.quizRepository=quizRepository;
    }

    public void setCurrentQuiz(Quiz quiz) {
        this.currentQuiz = quiz;
    }

    public Quiz getCurrentQuiz() {
        if (currentQuiz == null) {
            logger.warn("No quiz is currently set.");
        }
        return currentQuiz;
    }

    public List<Question> fetchAndParseQuiz(String prompt) {
        Map<String, String> requestBody = Map.of(
                "prompt", prompt,
                "model", "llama3.2"
        );

        try {
            String aggregatedResponse = webClient.post()
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .map(response -> Optional.ofNullable(response.get("response"))
                            .map(Object::toString).orElse(""))
                    .timeout(Duration.ofMinutes(5))
                    .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                    .reduce("", String::concat)
                    .block();

            if (aggregatedResponse == null || aggregatedResponse.isEmpty()) {
                throw new IllegalStateException("Aggregated response is empty.");
            }

            logger.info("Aggregated response received: {}", aggregatedResponse);

            String cleanedJson = extractQuizJson(aggregatedResponse);
            logger.debug("Cleaned JSON: {}", cleanedJson);

            return parseQuestions(cleanedJson);

        } catch (Exception e) {
            logger.error("Error fetching or parsing quiz: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching or parsing quiz: " + e.getMessage(), e);
        }
    }

    public QuizResult evaluateQuiz(Quiz quiz, UserResponse userResponse) {
        int score = 0;
        Map<Integer, String> corrections = new HashMap<>();
        List<String> detailedFeedback = new ArrayList<>();

        for (Question question : quiz.getQuestions()) {
            String userAnswer = userResponse.getUserAnswers().get(String.valueOf(question.getId()));
            String correctAnswer = question.getCorrectAnswer();

            if (userAnswer != null && userAnswer.equals(correctAnswer)) {
                score++;
                detailedFeedback.add("Question: " + question.getQuestion() + " - Correct!");
            } else {
                corrections.put(Math.toIntExact(question.getId()), correctAnswer);
                detailedFeedback.add("Question: " + question.getQuestion() + " - Incorrect. Correct answer: " + correctAnswer);
            }
        }

        quiz.setScore((double) score); // Set score for the quiz

        return new QuizResult(quiz, score, corrections, detailedFeedback);
    }




    private String extractQuizJson(String jsonResponse) {
        try {
            // Remove any text before the actual JSON array starts
            int startIndex = jsonResponse.indexOf('[');
            int endIndex = jsonResponse.lastIndexOf(']');

            if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
                throw new IllegalStateException("Response does not contain a valid JSON array.");
            }

            String extractedJson = jsonResponse.substring(startIndex, endIndex + 1).trim();

            // Validate and sanitize the extracted JSON
            String sanitizedJson = sanitizeJson(extractedJson);

            JsonNode root = objectMapper.readTree(sanitizedJson);
            if (!root.isArray()) {
                throw new IllegalStateException("Extracted content is not a JSON array.");
            }

            return root.toString();
        } catch (Exception e) {
            logger.error("Error extracting quiz JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error extracting quiz JSON: " + e.getMessage(), e);
        }
    }

//    private String sanitizeJson(String jsonResponse) {
//        return jsonResponse
//                .replaceAll("\\]\\s*,\\s*\\[", ",") // Merge nested arrays
//                .replaceAll("\\\\\"", "\"") // Handle escaped quotes
//                .replaceAll("[^\\x20-\\x7E]", ""); // Remove non-ASCII characters
//    }
private String sanitizeJson(String jsonResponse) {
    try {
        // Ensure the response is trimmed
        String sanitizedResponse = jsonResponse.trim();

        // Remove non-ASCII characters while preserving valid JSON control characters
        sanitizedResponse = sanitizedResponse.replaceAll("[^\\x20-\\x7E\\r\\n\\t]", "");

        // Remove improperly escaped backslashes
        sanitizedResponse = sanitizedResponse.replaceAll("(?<!\\\\)\\\\(?![\"/bfnrt])", "");

        // Remove trailing commas in JSON objects or arrays
        sanitizedResponse = sanitizedResponse.replaceAll(",\\s*(\\}|\\])", "$1");

        // Ensure the JSON is properly enclosed (only if not already valid)
        if (!sanitizedResponse.startsWith("[") || !sanitizedResponse.endsWith("]")) {
            throw new IllegalStateException("Sanitized JSON is not a valid JSON array.");
        }

        // Collapse excessive whitespace (e.g., newlines, tabs) into a single space
        sanitizedResponse = sanitizedResponse.replaceAll("\\s+", " ");

        return sanitizedResponse;
    } catch (Exception e) {
        logger.error("Error sanitizing JSON: {}", e.getMessage(), e);
        throw new RuntimeException("Error sanitizing JSON: " + e.getMessage(), e);
    }
}


    private List<Question> parseQuestions(String cleanedJson) {
        try {
            List<Question> dtos = objectMapper.readValue(cleanedJson, new TypeReference<>() {});
            List<Question> questions = new ArrayList<>();
            for (Question dto : dtos) {
                if (dto.getQuestion() != null && dto.getOptions() != null && dto.getCorrectAnswer() != null) {
                    questions.add(new Question(dto.getQuestion(), dto.getOptions(), dto.getCorrectAnswer()));
                } else {
                    logger.warn("Skipping invalid question: {}", dto);
                }
            }
            return questions;
        } catch (Exception e) {
            logger.error("Error parsing questions from JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error parsing questions from JSON: " + e.getMessage(), e);
        }
    }


    @Transactional
    public void saveQuiz(Quiz quiz) {
        quizRepository.save(quiz);
    }

    @Transactional
    public Quiz saveQuizWithQuestions(Quiz quiz) {
        logger.info("Saving Quiz: ID = {}", quiz.getId());

        // Save quiz first to generate its ID
        Quiz savedQuiz = quizRepository.save(quiz);
        logger.info("Saved Quiz: ID = {}", savedQuiz.getId());

        // Set quiz reference and save questions
        for (Question question : quiz.getQuestions()) {
            logger.info("Saving Question: ID = {}, User Response = {}", question.getId(), question.getUserResponse());
            question.setQuiz(savedQuiz);
            Question savedQuestion = questionRepository.save(question);
            logger.info("Saved Question: ID = {}", savedQuestion.getId());
        }

        return savedQuiz;
    }


    @Transactional
    public Quiz saveQuizWithQuestions(Quiz quiz, List<Question> questions) {
        Quiz managedQuiz = quizRepository.findById(quiz.getId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        for (Question question : questions) {
            Question managedQuestion = questionRepository.findById(question.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Question not found"));
            managedQuestion.setUserResponse(question.getUserResponse());
            questionRepository.save(managedQuestion);
        }

        return quizRepository.save(managedQuiz);
    }


    @Transactional
    public void updateQuestionResponses(List<Question> updatedQuestions) {
        for (Question updatedQuestion : updatedQuestions) {
            logger.info("Updating Question: ID = {}", updatedQuestion.getId());
            Question managedQuestion = questionRepository.findById(updatedQuestion.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Question not found"));
            managedQuestion.setUserResponse(updatedQuestion.getUserResponse());
            questionRepository.save(managedQuestion);
            logger.info("Updated Question: ID = {}, User Response = {}", managedQuestion.getId(), managedQuestion.getUserResponse());
        }
    }




    public List<Quiz> getQuizzesByUser(Long userId) {
        return quizRepository.findByUserId(userId);
    }

    public List<Quiz> getQuizzesByProgrammingLanguage(String programmingLanguage) {
        return quizRepository.findByProgrammingLanguage(programmingLanguage);
    }

    public QuizResult saveQuizResult(QuizResult result) {
        return quizResultRepository.save(result);
    }
    public void saveUserResponses(Map<String, String> userAnswers, Quiz quiz) {
        List<Question> questions = quiz.getQuestions();

        for (Question question : questions) {
            String userAnswer = userAnswers.get(String.valueOf(question.getId()));
            question.setUserResponse(userAnswer); // Directly set the user response
        }

        // Save updated questions using the QuestionRepository
        questionRepository.saveAll(questions);
    }


    private String cleanResponse(String response) {
        // Locate the start '[' and end ']' of the JSON array
        int startIndex = response.indexOf('[');
        int endIndex = response.lastIndexOf(']');

        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            throw new IllegalStateException("Response does not contain a valid JSON array.");
        }

        // Extract and return the JSON array
        return response.substring(startIndex, endIndex + 1).trim();
    }

    public Quiz getQuizById(Long quizId, Long userId) {
        return quizRepository.findByIdAndUserId(quizId, userId);
    }

    public UserResponse getUserResponseByQuizId(Long quizId) {
        return userResponseRepository.findByQuizId(quizId)
                .orElseThrow(() -> new RuntimeException("UserResponse not found for quizId: " + quizId));
    }

    public void saveQuestions(List<Question> questions) {
        questionRepository.saveAll(questions);
    }

    @Transactional
    public Question mergeQuestion(Question question) {
        return questionRepository.findById(question.getId())
                .orElseThrow(() -> new RuntimeException("Question not found for ID: " + question.getId()));
    }



}
