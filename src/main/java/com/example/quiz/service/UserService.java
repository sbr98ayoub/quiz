    package com.example.quiz.service;

    import com.example.quiz.domain.User;
    import com.example.quiz.repository.UserRepository;
    import org.hibernate.dialect.lock.OptimisticEntityLockException;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.retry.annotation.Backoff;
    import org.springframework.retry.annotation.Retryable;
    import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
    import org.springframework.stereotype.Service;

    import java.util.Optional;

    @Service
    public class UserService {

        private final UserRepository userRepository;
        private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


        @Autowired
        public UserService(UserRepository userRepository) {
            this.userRepository = userRepository;
        }

        public User getUserById(Long userId) {
            return userRepository.findById(userId).orElse(null);
        }

        public User saveUser(User user) {
            if (user.getId() != null) {
                // Existing user update: Fetch the current user from the DB
                Optional<User> existingUser = userRepository.findById(user.getId());
                if (existingUser.isPresent()) {
                    String currentPassword = existingUser.get().getPassword();
                    if (!user.getPassword().equals(currentPassword)) {
                        // Encode the password only if it has been changed
                        user.setPassword(passwordEncoder.encode(user.getPassword()));
                    } else {
                        // Keep the current password if it hasn't changed
                        user.setPassword(currentPassword);
                    }
                }
            } else {

                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            return userRepository.save(user);
        }

        public boolean validatePassword(String rawPassword, String encodedPassword) {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        }

        public Optional<User> findByEmail(String email) {
            return userRepository.findByEmail(email);
        }
        public Optional<User> findById(Long id ) {
            return userRepository.findById(id);
        }
    }
