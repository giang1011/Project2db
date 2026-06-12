package com.library.service;

import com.library.model.ActivityLogDTO;
import com.library.repository.ActivityLogDAO;

import java.sql.SQLException;
import java.util.List;

public class ActivityLogService {
    private final ActivityLogDAO activityLogDAO;

    public ActivityLogService() {
        this.activityLogDAO = new ActivityLogDAO();
    }

    public List<ActivityLogDTO> getAllLogs(String keyword) throws SQLException {
        return activityLogDAO.getAllLogs(keyword);
    }

    public void logAction(long userId, String action, String oldValue, String newValue) {
        // Run log action on Virtual Thread to not block the main transaction thread
        Thread.ofVirtual().name("LogActionVirtualThread").start(() -> {
            try {
                activityLogDAO.logAction(userId, action, oldValue, newValue);
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Failed to log action: " + action);
            }
        });
    }
}
