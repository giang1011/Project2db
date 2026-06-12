package com.library.repository;

import com.library.model.SystemSetting;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SystemSettingRepository {

    public List<SystemSetting> findAll() throws Exception {
        List<SystemSetting> settings = new ArrayList<>();
        String sql = "SELECT SettingID, SettingKey, SettingValue, DataType, Description, UpdatedAt FROM SystemSettings ORDER BY SettingKey";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                SystemSetting s = new SystemSetting();
                s.setSettingId(rs.getLong("SettingID"));
                s.setSettingKey(rs.getString("SettingKey"));
                s.setSettingValue(rs.getString("SettingValue"));
                s.setDataType(rs.getString("DataType"));
                s.setDescription(rs.getString("Description"));
                if (rs.getTimestamp("UpdatedAt") != null) {
                    s.setUpdatedAt(rs.getTimestamp("UpdatedAt").toLocalDateTime());
                }
                settings.add(s);
            }
        }
        return settings;
    }

    public boolean updateSettingValue(Long settingId, String newValue) throws Exception {
        String sql = "UPDATE SystemSettings SET SettingValue = ?, UpdatedAt = SYSDATETIME() WHERE SettingID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newValue);
            stmt.setLong(2, settingId);
            return stmt.executeUpdate() > 0;
        }
    }
}
