package com.example.quiz.controller;

import com.example.quiz.domain.User;
import com.example.quiz.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        user.setPassword(password);
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
                "fullName", user.getFullName(),
                "profileImage",user.getProfileImage(),
                "phone",user.getPhone()
        ));
    }
    @PostMapping("/updateProfile")
    public ResponseEntity<?> updateProfile(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "fullName", required = false) String fullName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        // Retrieve the user from the database
        User user = userService.getUserById(userId);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found."));
        }

        // Update fields if provided
        if (fullName != null) user.setFullName(fullName);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);

        // Handle password: Update only if a new password is provided
        if (password != null && !password.isEmpty() && !userService.validatePassword(password, user.getPassword())) {
            user.setPassword(password); // Pass new password to `saveUser` for encoding
        }

        // Handle profile image upload
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = userId + "_" + file.getOriginalFilename();
                String uploadDir = "profile-images/";
                Path path = Paths.get(uploadDir + fileName);
                Files.createDirectories(path.getParent());
                Files.write(path, file.getBytes());
                user.setProfileImage(fileName);
            } catch (IOException e) {
                return ResponseEntity.status(500).body(Map.of("error", "Error uploading image."));
            }
        }

        // Save the updated user
        userService.saveUser(user);

        return ResponseEntity.ok(Map.of("message", "Profile updated successfully."));
    }



}
