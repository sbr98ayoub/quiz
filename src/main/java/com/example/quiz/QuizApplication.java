package com.example.quiz;

import com.example.quiz.domain.User;
import com.example.quiz.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class QuizApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizApplication.class, args);
    }
    @Bean
    public CommandLineRunner updatePassword(UserService userService) {
        return args -> {
            Long userId =1L; // The ID of the user to update
            String newPassword = "123"; // The new raw password to set

            User user = userService.getUserById(userId);
            if (user != null) {
                user.setPassword(newPassword); // Set the raw password
                userService.saveUser(user); // Save the user (will hash the password)
                System.out.println("Password updated successfully for user with ID: " + userId);
            } else {
                System.out.println("User with ID " + userId + " not found.");
            }
        };
    }

}
