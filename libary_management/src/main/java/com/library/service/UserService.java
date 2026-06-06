package com.library.service;

import com.library.model.User;
import com.library.repository.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository userRepository;

    public UserService() {
        this.userRepository = new UserRepository();
    }

    public List<User> getAllUsers() throws Exception {
        return userRepository.findAll();
    }

    public void addUser(User user, String password) throws Exception {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (user.getRole() == null || (!user.getRole().equals("ADMIN") && !user.getRole().equals("LIBRARIAN"))) {
            throw new IllegalArgumentException("Invalid role. Must be ADMIN or LIBRARIAN.");
        }

        user.setStatus("ACTIVE");
        
        boolean success = userRepository.addUser(user, password);
        if (!success) {
            throw new Exception("Failed to add user.");
        }
    }

    public void updateUser(User user, String newPassword) throws Exception {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (user.getRole() == null || (!user.getRole().equals("ADMIN") && !user.getRole().equals("LIBRARIAN"))) {
            throw new IllegalArgumentException("Invalid role. Must be ADMIN or LIBRARIAN.");
        }
        if (user.getStatus() == null || (!user.getStatus().equals("ACTIVE") && !user.getStatus().equals("INACTIVE"))) {
            throw new IllegalArgumentException("Invalid status. Must be ACTIVE or INACTIVE.");
        }

        boolean success = userRepository.updateUser(user, newPassword);
        if (!success) {
            throw new Exception("Failed to update user.");
        }
    }
}
