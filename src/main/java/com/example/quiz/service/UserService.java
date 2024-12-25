package com.example.quiz.service;

import com.example.quiz.domain.User;
import com.example.quiz.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

//    public boolean registerUser(User user) {
//        if (userRepository.existsByEmail(user.getEmail())) {
//            return false;
//        }
//        userRepository.save(user);
//        return true;
//    }
}
