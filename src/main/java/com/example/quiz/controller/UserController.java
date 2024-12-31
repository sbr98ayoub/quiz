package com.example.quiz.controller;

import com.example.quiz.domain.User;
import com.example.quiz.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> userData) {
        String email = userData.get("email");
        String password = userData.get("password");
        String fullName = userData.get("fullName");

        // Check if email already exists
        if (userService.findByEmail(email).isPresent()) {
            return ResponseEntity.status(409).body("Email is already in use.");
        }

        // Create and save a new user
        User user = new User();
        user.setEmail(email);
        user.setPassword(password); // Use password encryption here, e.g., BCrypt
        user.setFullName(fullName);
        user.setCreatedAt(LocalDateTime.now());

        userService.saveUser(user);

        return ResponseEntity.ok("User registered successfully.");
    }

    // Authenticate a user
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        User user = userService.findByEmail(email).orElse(null);
        if (user == null || !userService.validatePassword(password, user.getPassword())) {
            return ResponseEntity.status(401).body("Invalid email or password.");
        }

        // Optionally generate and return a token here
        return ResponseEntity.ok(Map.of(
                "message", "Login successful.",
                "userId", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName()
        ));
    }
}
