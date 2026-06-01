package com.library.service;

import com.library.model.DashboardMetrics;
import com.library.model.DashboardTransactionDTO;
import com.library.repository.DashboardDAO;

import java.sql.SQLException;
import java.util.List;

public class DashboardService {
    private final DashboardDAO dashboardDAO;

    public DashboardService() {
        this.dashboardDAO = new DashboardDAO();
    }

    public DashboardMetrics getMetrics() throws SQLException {
        return dashboardDAO.getMetrics();
    }

    public List<DashboardTransactionDTO> getTodaysTransactions() throws SQLException {
        return dashboardDAO.getTodaysTransactions();
    }
}
