package com.library.repository;

import com.library.model.ActivityLogDTO;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {
    public List<ActivityLogDTO> getAllLogs(String searchKeyword) throws SQLException {
        List<ActivityLogDTO> logs = new ArrayList<>();
        String sql = "SELECT al.LogID, u.FullName, al.Action, al.OldValue, al.NewValue, al.CreatedAt " +
                     "FROM ActivityLogs al " +
                     "JOIN Users u ON al.UserID = u.UserID ";
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql += "WHERE u.FullName LIKE ? OR al.Action LIKE ? ";
        }
        sql += "ORDER BY al.CreatedAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                String searchPattern = "%" + searchKeyword + "%";
                stmt.setString(1, searchPattern);
                stmt.setString(2, searchPattern);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(new ActivityLogDTO(
                        rs.getLong("LogID"),
                        rs.getString("FullName"),
                        rs.getString("Action"),
                        rs.getString("OldValue"),
                        rs.getString("NewValue"),
                        rs.getTimestamp("CreatedAt").toLocalDateTime()
                    ));
                }
            }
        }
        return logs;
    }

    public void logAction(long userId, String action, String oldValue, String newValue) throws SQLException {
        String sql = "INSERT INTO ActivityLogs (UserID, Action, OldValue, NewValue, CreatedAt) VALUES (?, ?, ?, ?, SYSDATETIME())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, action);
            stmt.setString(3, oldValue);
            stmt.setString(4, newValue);
            stmt.executeUpdate();
        }
    }
}
