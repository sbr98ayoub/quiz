package com.example.quiz.controller;

import com.example.quiz.domain.User;
import com.example.quiz.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

//    @PostMapping("/register")
//    public ResponseEntity<String> registerUser(@RequestBody User user) {
//        user.setCreatedAt(LocalDateTime.now());
//        boolean isRegistered = userService.registerUser(user);
//        if (isRegistered) {
//            return ResponseEntity.status(201).body("User registered successfully!");
//        }
//        return ResponseEntity.badRequest().body("Email already exists!");
//    }
}

