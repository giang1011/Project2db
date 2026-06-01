package com.library.service;

import com.library.model.User;
import com.library.repository.UserRepository;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService() {
        this.userRepository = new UserRepository();
    }

    public User authenticate(String username, String password) throws Exception {
        return userRepository.findByUsernameAndPassword(username, password);
    }
}
