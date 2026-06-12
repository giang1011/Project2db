package com.library.repository;

import com.library.model.AdminStatisticsDTO;
import com.library.model.AdminStatisticsDTO.TopItem;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminStatisticsDAO {

    public AdminStatisticsDTO getStatistics(LocalDate fromDate, LocalDate toDate) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            int totalBorrow = getTotalBorrow(conn, fromDate, toDate);
            int totalReturned = getTotalReturned(conn, fromDate, toDate);
            double totalFines = getTotalFines(conn, fromDate, toDate);
            Map<String, Integer> borrowedByDate = getBorrowedBooksByDate(conn, fromDate, toDate);
            Map<String, Integer> booksByCategory = getBooksByCategory(conn, fromDate, toDate);
            List<TopItem> topBooks = getTopBooks(conn, fromDate, toDate, 5);
            List<TopItem> topMembers = getTopMembers(conn, fromDate, toDate, 5);

            return new AdminStatisticsDTO(totalBorrow, totalReturned, totalFines, 
                borrowedByDate, booksByCategory, topBooks, topMembers);
        }
    }

    private int getTotalBorrow(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COUNT(*) FROM BorrowItems bi JOIN BorrowTransactions bt ON bi.TransactionID = bt.TransactionID WHERE CAST(bt.BorrowDate AS DATE) >= ? AND CAST(bt.BorrowDate AS DATE) <= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private int getTotalReturned(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ReturnItems ri JOIN ReturnTransactions rt ON ri.ReturnTransactionID = rt.ReturnTransactionID WHERE CAST(rt.ReturnDate AS DATE) >= ? AND CAST(rt.ReturnDate AS DATE) <= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private double getTotalFines(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT ISNULL(SUM(Amount), 0) FROM Fines WHERE CAST(IssuedAt AS DATE) >= ? AND CAST(IssuedAt AS DATE) <= ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    private Map<String, Integer> getBorrowedBooksByDate(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT CAST(bt.BorrowDate AS DATE) as BDate, COUNT(*) as Cnt " +
                     "FROM BorrowItems bi JOIN BorrowTransactions bt ON bi.TransactionID = bt.TransactionID " +
                     "WHERE CAST(bt.BorrowDate AS DATE) >= ? AND CAST(bt.BorrowDate AS DATE) <= ? " +
                     "GROUP BY CAST(bt.BorrowDate AS DATE) ORDER BY BDate";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getDate("BDate").toString(), rs.getInt("Cnt"));
                }
            }
        }
        return result;
    }

    private Map<String, Integer> getBooksByCategory(Connection conn, LocalDate from, LocalDate to) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        String sql = "SELECT c.CategoryName, COUNT(*) as Cnt " +
                     "FROM BorrowItems bi JOIN BorrowTransactions bt ON bi.TransactionID = bt.TransactionID " +
                     "JOIN BookCopies bc ON bi.CopyID = bc.CopyID " +
                     "JOIN BookCategories bcat ON bc.BookID = bcat.BookID " +
                     "JOIN Categories c ON bcat.CategoryID = c.CategoryID " +
                     "WHERE CAST(bt.BorrowDate AS DATE) >= ? AND CAST(bt.BorrowDate AS DATE) <= ? " +
                     "GROUP BY c.CategoryName ORDER BY Cnt DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(from));
            stmt.setDate(2, Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("CategoryName"), rs.getInt("Cnt"));
                }
            }
        }
        return result;
    }

    private List<TopItem> getTopBooks(Connection conn, LocalDate from, LocalDate to, int limit) throws SQLException {
        List<TopItem> result = new ArrayList<>();
        String sql = "SELECT TOP (?) b.Title, COUNT(*) as BorrowCount " +
                     "FROM BorrowItems bi JOIN BorrowTransactions bt ON bi.TransactionID = bt.TransactionID " +
                     "JOIN BookCopies bc ON bi.CopyID = bc.CopyID " +
                     "JOIN Books b ON bc.BookID = b.BookID " +
                     "WHERE CAST(bt.BorrowDate AS DATE) >= ? AND CAST(bt.BorrowDate AS DATE) <= ? " +
                     "GROUP BY b.BookID, b.Title ORDER BY BorrowCount DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setDate(2, Date.valueOf(from));
            stmt.setDate(3, Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new TopItem(rs.getString("Title"), rs.getInt("BorrowCount")));
                }
            }
        }
        return result;
    }

    private List<TopItem> getTopMembers(Connection conn, LocalDate from, LocalDate to, int limit) throws SQLException {
        List<TopItem> result = new ArrayList<>();
        String sql = "SELECT TOP (?) m.FullName, COUNT(*) as BorrowCount " +
                     "FROM BorrowItems bi JOIN BorrowTransactions bt ON bi.TransactionID = bt.TransactionID " +
                     "JOIN Members m ON bt.MemberID = m.MemberID " +
                     "WHERE CAST(bt.BorrowDate AS DATE) >= ? AND CAST(bt.BorrowDate AS DATE) <= ? " +
                     "GROUP BY m.MemberID, m.FullName ORDER BY BorrowCount DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setDate(2, Date.valueOf(from));
            stmt.setDate(3, Date.valueOf(to));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new TopItem(rs.getString("FullName"), rs.getInt("BorrowCount")));
                }
            }
        }
        return result;
    }
}
