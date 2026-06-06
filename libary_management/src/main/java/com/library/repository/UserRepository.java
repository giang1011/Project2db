package com.library.repository;

import com.library.model.User;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserRepository {

    public User findByUsernameAndPassword(String username, String password) throws Exception {
        // Lay thong tin user tu database 
        // Luu y: Do yeu cau truyen mat khau truc tiep (PasswordHash) nen hien tai code se so sanh equals() truc tiep chuoi truyen vao
        // Trong moi truong thuc te, day se la noi su dung BCrypt.checkpw() hoac bam mat khau nguoi dung nhap vao truoc khi so sanh.
        String query = "SELECT UserID, Username, FullName, Email, Role, Status FROM Users WHERE Username = ? AND PasswordHash = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
             
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getLong("UserID"));
                    user.setUsername(rs.getString("Username"));
                    user.setFullName(rs.getString("FullName"));
                    user.setEmail(rs.getString("Email"));
                    user.setRole(rs.getString("Role"));
                    user.setStatus(rs.getString("Status"));
                    return user;
                }
            }
        }
        return null; // Khong tim thay user hoac sai mat khau
    }

    public java.util.List<User> findAll() throws Exception {
        java.util.List<User> users = new java.util.ArrayList<>();
        String query = "SELECT UserID, Username, FullName, Email, Role, Status FROM Users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setUserId(rs.getLong("UserID"));
                user.setUsername(rs.getString("Username"));
                user.setFullName(rs.getString("FullName"));
                user.setEmail(rs.getString("Email"));
                user.setRole(rs.getString("Role"));
                user.setStatus(rs.getString("Status"));
                users.add(user);
            }
        }
        return users;
    }

    public boolean addUser(User user, String password) throws Exception {
        String query = "INSERT INTO Users (Username, PasswordHash, FullName, Email, Role, Status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, password); // Hashing should be done here if BCrypt was used
            stmt.setString(3, user.getFullName());
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                stmt.setString(4, user.getEmail());
            } else {
                stmt.setNull(4, java.sql.Types.VARCHAR);
            }
            stmt.setString(5, user.getRole());
            stmt.setString(6, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateUser(User user, String newPassword) throws Exception {
        boolean updatePassword = (newPassword != null && !newPassword.trim().isEmpty());
        StringBuilder query = new StringBuilder("UPDATE Users SET Username = ?, FullName = ?, Email = ?, Role = ?, Status = ?");
        if (updatePassword) {
            query.append(", PasswordHash = ?");
        }
        query.append(" WHERE UserID = ?");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getFullName());
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                stmt.setString(3, user.getEmail());
            } else {
                stmt.setNull(3, java.sql.Types.VARCHAR);
            }
            stmt.setString(4, user.getRole());
            stmt.setString(5, user.getStatus());

            int paramIndex = 6;
            if (updatePassword) {
                stmt.setString(paramIndex++, newPassword); // Hashing would apply here
            }
            stmt.setLong(paramIndex, user.getUserId());

            return stmt.executeUpdate() > 0;
        }
    }
}
