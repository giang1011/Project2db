package com.library.service;

import com.library.model.AdminStatisticsDTO;
import com.library.repository.AdminStatisticsDAO;
import java.sql.SQLException;
import java.time.LocalDate;

public class AdminStatisticsService {
    private final AdminStatisticsDAO adminStatisticsDAO;

    public AdminStatisticsService() {
        this.adminStatisticsDAO = new AdminStatisticsDAO();
    }

    public AdminStatisticsDTO getStatistics(LocalDate fromDate, LocalDate toDate) throws SQLException {
        return adminStatisticsDAO.getStatistics(fromDate, toDate);
    }
}
