package com.library.repository;

import com.library.model.BorrowItemDTO;
import com.library.model.Member;
import com.library.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.library.model.ReturnItemDTO;

public class BorrowDAO {
    private static final Logger logger = LoggerFactory.getLogger(BorrowDAO.class);

    public Member findMemberByCode(String memberCode) throws SQLException {
        String sql = "SELECT MemberID, MemberCode, FullName, MemberType, Status, MaxBorrowBooks, BorrowDurationDays, MembershipEndDate " +
                     "FROM Members WHERE MemberCode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, memberCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Member m = new Member();
                    m.setMemberId(rs.getLong("MemberID"));
                    m.setMemberCode(rs.getString("MemberCode"));
                    m.setFullName(rs.getString("FullName"));
                    m.setMemberType(rs.getString("MemberType"));
                    m.setStatus(rs.getString("Status"));
                    m.setMaxBorrowBooks(rs.getInt("MaxBorrowBooks"));
                    m.setBorrowDurationDays(rs.getInt("BorrowDurationDays"));
                    if (rs.getDate("MembershipEndDate") != null) {
                        m.setMembershipEndDate(rs.getDate("MembershipEndDate").toLocalDate());
                    }
                    return m;
                }
            }
        }
        return null;
    }

    public int getBorrowedBooksCount(Long memberId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM BorrowItems bi " +
                     "JOIN BorrowTransactions bt ON bi.TransactionID = bt.TransactionID " +
                     "WHERE bt.MemberID = ? AND bi.Status IN ('BORROWING', 'OVERDUE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, memberId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public BorrowItemDTO findBookCopyByBarcode(String barcode) throws SQLException {
        String sql = "SELECT bc.CopyID, bc.Barcode, bc.CirculationStatus, bc.IsDeleted, bc.IsReferenceOnly, bc.ShelfLocation, b.Title, b.CoverImage " +
                     "FROM BookCopies bc " +
                     "JOIN Books b ON bc.BookID = b.BookID " +
                     "WHERE bc.Barcode = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, barcode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getBoolean("IsDeleted")) {
                        throw new SQLException("Sách này đã bị xóa khỏi hệ thống.");
                    }
                    if (rs.getBoolean("IsReferenceOnly")) {
                        throw new SQLException("Đây là sách tham khảo, không được phép mượn về.");
                    }
                    if (!"AVAILABLE".equals(rs.getString("CirculationStatus"))) {
                        throw new SQLException("Sách này đang không sẵn sàng để mượn (Trạng thái: " + rs.getString("CirculationStatus") + ").");
                    }
                    
                    BorrowItemDTO dto = new BorrowItemDTO();
                    dto.setCopyId(rs.getLong("CopyID"));
                    dto.setBarcode(rs.getString("Barcode"));
                    dto.setTitle(rs.getString("Title"));
                    dto.setStatus(rs.getString("CirculationStatus"));
                    dto.setShelfLocation(rs.getString("ShelfLocation"));
                    dto.setCoverImage(rs.getString("CoverImage"));
                    return dto;
                }
            }
        }
        return null;
    }

    public void checkoutBooks(Long memberId, Long userId, List<BorrowItemDTO> items) throws SQLException {
        String insertTxSql = "INSERT INTO BorrowTransactions (MemberID, BorrowedBy) VALUES (?, ?)";
        String insertItemSql = "INSERT INTO BorrowItems (TransactionID, CopyID, DueDate, Status) VALUES (?, ?, ?, 'BORROWING')";
        String updateCopySql = "UPDATE BookCopies SET CirculationStatus = 'BORROWED' WHERE CopyID = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction

            long transactionId = -1;
            try (PreparedStatement stmtTx = conn.prepareStatement(insertTxSql, Statement.RETURN_GENERATED_KEYS)) {
                stmtTx.setLong(1, memberId);
                stmtTx.setLong(2, userId);
                stmtTx.executeUpdate();
                try (ResultSet rs = stmtTx.getGeneratedKeys()) {
                    if (rs.next()) {
                        transactionId = rs.getLong(1);
                    } else {
                        throw new SQLException("Lỗi khi tạo giao dịch mượn sách.");
                    }
                }
            }

            try (PreparedStatement stmtItem = conn.prepareStatement(insertItemSql);
                 PreparedStatement stmtCopy = conn.prepareStatement(updateCopySql)) {
                
                for (BorrowItemDTO item : items) {
                    // Thêm vào BorrowItems
                    stmtItem.setLong(1, transactionId);
                    stmtItem.setLong(2, item.getCopyId());
                    stmtItem.setDate(3, Date.valueOf(item.getDueDate()));
                    stmtItem.addBatch();

                    // Cập nhật BookCopies
                    stmtCopy.setLong(1, item.getCopyId());
                    stmtCopy.addBatch();
                }

                stmtItem.executeBatch();
                stmtCopy.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { logger.error("Rollback failed", ex); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { logger.error("Close connection failed", e); }
            }
        }
    }

    public ReturnItemDTO findActiveBorrowItemByBarcode(String barcode) throws SQLException {
        String sql = "SELECT bi.BorrowItemID, bt.TransactionID, bc.CopyID, bt.MemberID, bc.Barcode, b.Title, b.CoverImage, m.FullName, bi.DueDate " +
                     "FROM BorrowItems bi " +
                     "JOIN BookCopies bc ON bi.CopyID = bc.CopyID " +
                     "JOIN Books b ON bc.BookID = b.BookID " +
                     "JOIN BorrowTransactions bt ON bi.TransactionID = bt.TransactionID " +
                     "JOIN Members m ON bt.MemberID = m.MemberID " +
                     "WHERE bc.Barcode = ? AND bi.Status IN ('BORROWING', 'OVERDUE')";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, barcode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ReturnItemDTO dto = new ReturnItemDTO();
                    dto.setBorrowItemId(rs.getLong("BorrowItemID"));
                    dto.setTransactionId(rs.getLong("TransactionID"));
                    dto.setCopyId(rs.getLong("CopyID"));
                    dto.setMemberId(rs.getLong("MemberID"));
                    dto.setBarcode(rs.getString("Barcode"));
                    dto.setTitle(rs.getString("Title"));
                    dto.setCoverImage(rs.getString("CoverImage"));
                    dto.setBorrowerName(rs.getString("FullName"));
                    if (rs.getDate("DueDate") != null) {
                        dto.setDueDate(rs.getDate("DueDate").toLocalDate());
                    }
                    return dto;
                }
            }
        }
        return null;
    }

    public List<ReturnItemDTO> findActiveBorrowItemsByMemberCode(String memberCode) throws SQLException {
        String sql = "SELECT bi.BorrowItemID, bt.TransactionID, bc.CopyID, bt.MemberID, bc.Barcode, b.Title, b.CoverImage, m.FullName, bi.DueDate " +
                     "FROM BorrowItems bi " +
                     "JOIN BookCopies bc ON bi.CopyID = bc.CopyID " +
                     "JOIN Books b ON bc.BookID = b.BookID " +
                     "JOIN BorrowTransactions bt ON bi.TransactionID = bt.TransactionID " +
                     "JOIN Members m ON bt.MemberID = m.MemberID " +
                     "WHERE m.MemberCode = ? AND bi.Status IN ('BORROWING', 'OVERDUE')";
        
        List<ReturnItemDTO> items = new java.util.ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, memberCode);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ReturnItemDTO dto = new ReturnItemDTO();
                    dto.setBorrowItemId(rs.getLong("BorrowItemID"));
                    dto.setTransactionId(rs.getLong("TransactionID"));
                    dto.setCopyId(rs.getLong("CopyID"));
                    dto.setMemberId(rs.getLong("MemberID"));
                    dto.setBarcode(rs.getString("Barcode"));
                    dto.setTitle(rs.getString("Title"));
                    dto.setCoverImage(rs.getString("CoverImage"));
                    dto.setBorrowerName(rs.getString("FullName"));
                    if (rs.getDate("DueDate") != null) {
                        dto.setDueDate(rs.getDate("DueDate").toLocalDate());
                    }
                    items.add(dto);
                }
            }
        }
        return items;
    }

    public BigDecimal getSystemSettingDecimal(String key, BigDecimal defaultValue) {
        String sql = "SELECT SettingValue FROM SystemSettings WHERE SettingKey = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new BigDecimal(rs.getString("SettingValue"));
                }
            }
        } catch (Exception e) {
            logger.warn("Could not load setting {}, using default", key);
        }
        return defaultValue;
    }

    public void checkinBook(ReturnItemDTO item, Long userId, String condition, String notes, BigDecimal fineAmount, boolean isFinePaid) throws SQLException {
        String insertReturnTxSql = "INSERT INTO ReturnTransactions (MemberID, ProcessedBy) VALUES (?, ?)";
        String insertReturnItemSql = "INSERT INTO ReturnItems (ReturnTransactionID, BorrowItemID, ReturnedCondition, Notes) VALUES (?, ?, ?, ?)";
        String updateBorrowItemSql = "UPDATE BorrowItems SET Status = ?, ReturnDate = SYSDATETIME() WHERE BorrowItemID = ?";
        String updateCopySql = "UPDATE BookCopies SET CirculationStatus = ?, PhysicalCondition = ? WHERE CopyID = ?";
        String insertFineSql = "INSERT INTO Fines (MemberID, BorrowItemID, FineType, Amount, PaidAmount, Status) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Return Transactions
            long returnTxId = -1;
            try (PreparedStatement stmtTx = conn.prepareStatement(insertReturnTxSql, Statement.RETURN_GENERATED_KEYS)) {
                stmtTx.setLong(1, item.getMemberId());
                stmtTx.setLong(2, userId);
                stmtTx.executeUpdate();
                try (ResultSet rs = stmtTx.getGeneratedKeys()) {
                    if (rs.next()) returnTxId = rs.getLong(1);
                    else throw new SQLException("Lỗi tạo giao dịch trả sách.");
                }
            }

            // 2. Return Items
            try (PreparedStatement stmtRetItem = conn.prepareStatement(insertReturnItemSql)) {
                stmtRetItem.setLong(1, returnTxId);
                stmtRetItem.setLong(2, item.getBorrowItemId());
                stmtRetItem.setString(3, condition);
                stmtRetItem.setString(4, notes);
                stmtRetItem.executeUpdate();
            }

            // 3. Update BorrowItem
            String borrowItemStatus = "LOST".equals(condition) ? "LOST" : "RETURNED";
            try (PreparedStatement stmtBorrowItem = conn.prepareStatement(updateBorrowItemSql)) {
                stmtBorrowItem.setString(1, borrowItemStatus);
                stmtBorrowItem.setLong(2, item.getBorrowItemId());
                stmtBorrowItem.executeUpdate();
            }

            // 4. Update Book Copy
            String copyCirculation = "LOST".equals(condition) ? "LOST" : "AVAILABLE";
            try (PreparedStatement stmtCopy = conn.prepareStatement(updateCopySql)) {
                stmtCopy.setString(1, copyCirculation);
                stmtCopy.setString(2, condition);
                stmtCopy.setLong(3, item.getCopyId());
                stmtCopy.executeUpdate();
            }

            // 5. Create Fine if needed
            if (fineAmount != null && fineAmount.compareTo(BigDecimal.ZERO) > 0) {
                String fineType = "OVERDUE";
                if ("DAMAGED".equals(condition)) fineType = "DAMAGED";
                else if ("LOST".equals(condition)) fineType = "LOST";
                
                try (PreparedStatement stmtFine = conn.prepareStatement(insertFineSql)) {
                    stmtFine.setLong(1, item.getMemberId());
                    stmtFine.setLong(2, item.getBorrowItemId());
                    stmtFine.setString(3, fineType);
                    stmtFine.setBigDecimal(4, fineAmount);
                    
                    if (isFinePaid) {
                        stmtFine.setBigDecimal(5, fineAmount);
                        stmtFine.setString(6, "PAID");
                    } else {
                        stmtFine.setBigDecimal(5, BigDecimal.ZERO);
                        stmtFine.setString(6, "UNPAID");
                    }
                    stmtFine.executeUpdate();
                    
                    if (isFinePaid) {
                        // Update PaidAt since we set it paid
                        try (PreparedStatement updateFinePaid = conn.prepareStatement("UPDATE Fines SET PaidAt = SYSDATETIME() WHERE MemberID = ? AND BorrowItemID = ?")) {
                            updateFinePaid.setLong(1, item.getMemberId());
                            updateFinePaid.setLong(2, item.getBorrowItemId());
                            updateFinePaid.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { logger.error("Rollback failed", ex); }
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { logger.error("Close connection failed", e); }
            }
        }
    }
}
