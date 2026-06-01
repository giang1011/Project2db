package com.library.repository;

import com.library.model.DashboardMetrics;
import com.library.model.DashboardTransactionDTO;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DashboardDAO {

    public DashboardMetrics getMetrics() throws SQLException {
        DashboardMetrics metrics = new DashboardMetrics();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Borrowing Books
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM BorrowItems WHERE Status = 'BORROWING'")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) metrics.setBorrowingBooks(rs.getInt(1));
                }
            }
            
            // 2. Overdue Books
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM BorrowItems WHERE Status = 'OVERDUE' OR (Status = 'BORROWING' AND DueDate < CAST(GETDATE() AS DATE))")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) metrics.setOverdueBooks(rs.getInt(1));
                }
            }
            
            // 3. Pending Profiles
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM MemberStudentProfiles WHERE StudentVerificationStatus = 'PENDING'")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) metrics.setPendingProfiles(rs.getInt(1));
                }
            }
            
            // 4. Unpaid Fines
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(DISTINCT MemberID) FROM Fines WHERE Status IN ('UNPAID', 'PARTIAL')")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) metrics.setUnpaidFines(rs.getInt(1));
                }
            }
        }
        return metrics;
    }

    public List<DashboardTransactionDTO> getTodaysTransactions() throws SQLException {
        List<DashboardTransactionDTO> list = new ArrayList<>();
        String sql = "SELECT bt.TransactionID, m.MemberCode, m.FullName, " +
                     "(SELECT COUNT(*) FROM BorrowItems bi WHERE bi.TransactionID = bt.TransactionID) AS BooksCount, " +
                     "bt.BorrowDate, u.FullName AS ProcessedBy " +
                     "FROM BorrowTransactions bt " +
                     "JOIN Members m ON bt.MemberID = m.MemberID " +
                     "JOIN Users u ON bt.BorrowedBy = u.UserID " +
                     "WHERE CAST(bt.BorrowDate AS DATE) = CAST(GETDATE() AS DATE) " +
                     "ORDER BY bt.BorrowDate DESC";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
             
            while (rs.next()) {
                DashboardTransactionDTO dto = new DashboardTransactionDTO();
                dto.setTransactionId(rs.getLong("TransactionID"));
                dto.setMemberCode(rs.getString("MemberCode"));
                dto.setMemberName(rs.getString("FullName"));
                dto.setBooksBorrowedCount(rs.getInt("BooksCount"));
                if (rs.getTimestamp("BorrowDate") != null) {
                    dto.setBorrowDate(rs.getTimestamp("BorrowDate").toLocalDateTime());
                }
                dto.setProcessedBy(rs.getString("ProcessedBy"));
                list.add(dto);
            }
        }
        return list;
    }
}
