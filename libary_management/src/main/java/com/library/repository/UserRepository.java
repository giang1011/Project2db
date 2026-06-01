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
        String query = "SELECT UserID, Username, FullName, Role, Status FROM Users WHERE Username = ? AND PasswordHash = ?";
        
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
                    user.setRole(rs.getString("Role"));
                    user.setStatus(rs.getString("Status"));
                    return user;
                }
            }
        }
        return null; // Khong tim thay user hoac sai mat khau
    }
}
