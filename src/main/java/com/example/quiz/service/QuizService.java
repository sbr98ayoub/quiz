package com.example.quiz.service;

import com.example.quiz.model.Question;
import com.example.quiz.model.Quiz;
import com.example.quiz.model.QuizResult;
import com.example.quiz.model.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;

@Service
public class QuizService {

    private static final Logger logger = LoggerFactory.getLogger(QuizService.class);

    private static final String LLAMA_API_URL = "http://localhost:11434/api/generate";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private Quiz currentQuiz;

    public QuizService(ObjectMapper objectMapper) {
        this.webClient = WebClient.builder()
                .baseUrl(LLAMA_API_URL)
                .build();
        this.objectMapper = objectMapper;
        this.currentQuiz = null;
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
        Map<Integer, String> corrections = new HashMap<>();
        List<String> detailedFeedback = new ArrayList<>();
        int score = 0;

        for (int i = 0; i < quiz.getQuestions().size(); i++) {
            Question question = quiz.getQuestions().get(i);
            String userAnswer = userResponse.getUserAnswers().get("question_" + i);
            String correctAnswer = question.getCorrectAnswer();

            if (correctAnswer.equals(userAnswer)) {
                score++;
                detailedFeedback.add("Question " + (i + 1) + ": Correct!");
            } else {
                corrections.put(i, correctAnswer);
                detailedFeedback.add("Question " + (i + 1) + ": Incorrect. Correct answer is " + correctAnswer);
            }
        }


        return new QuizResult(score, corrections, detailedFeedback);
    }


    private String extractQuizJson(String jsonResponse) {
        try {
            // Clean and sanitize the response
            String sanitizedResponse = sanitizeJson(jsonResponse);

            JsonNode root = objectMapper.readTree(sanitizedResponse);
            if (root.isArray()) {
                return root.toString();
            }

            StringBuilder quizBuilder = new StringBuilder();
            root.forEach(node -> {
                if (node.has("response")) {
                    quizBuilder.append(node.get("response").asText());
                }
            });

            String result = quizBuilder.toString().trim();
            if (result.isEmpty()) {
                throw new IllegalStateException("Extracted quiz JSON is empty.");
            }

            return result;
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
        if (!sanitizedResponse.startsWith("{") && !sanitizedResponse.startsWith("[")) {
            sanitizedResponse = "{" + sanitizedResponse;
        }
        if (!sanitizedResponse.endsWith("}") && !sanitizedResponse.endsWith("]")) {
            sanitizedResponse = sanitizedResponse + "}";
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
}
