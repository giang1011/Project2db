package com.library.repository;

import com.library.model.FineDTO;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FineDAO {

    public List<FineDTO> getAllFines() throws SQLException {
        String sql = "SELECT f.FineID, f.MemberID, m.MemberCode, m.FullName, " +
                     "f.BorrowItemID, b.Title AS BookTitle, bc.Barcode, " +
                     "f.FineType, f.Amount, f.PaidAmount, f.Status, f.IssuedAt, f.PaidAt, f.Notes " +
                     "FROM Fines f " +
                     "JOIN Members m ON f.MemberID = m.MemberID " +
                     "LEFT JOIN BorrowItems bi ON f.BorrowItemID = bi.BorrowItemID " +
                     "LEFT JOIN BookCopies bc ON bi.CopyID = bc.CopyID " +
                     "LEFT JOIN Books b ON bc.BookID = b.BookID " +
                     "ORDER BY f.IssuedAt DESC";
                     
        List<FineDTO> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
             
            while (rs.next()) {
                list.add(mapResultSetToFineDTO(rs));
            }
        }
        return list;
    }

    public void updateFinePayment(long fineId, java.math.BigDecimal amountPaid) throws SQLException {
        String sql = "UPDATE Fines SET PaidAmount = PaidAmount + ?, " +
                     "Status = CASE WHEN PaidAmount + ? >= Amount THEN 'PAID' ELSE 'PARTIAL' END, " +
                     "PaidAt = CASE WHEN PaidAmount + ? >= Amount THEN SYSDATETIME() ELSE PaidAt END " +
                     "WHERE FineID = ?";
                     
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setBigDecimal(1, amountPaid);
            stmt.setBigDecimal(2, amountPaid);
            stmt.setBigDecimal(3, amountPaid);
            stmt.setLong(4, fineId);
            
            stmt.executeUpdate();
        }
    }

    private FineDTO mapResultSetToFineDTO(ResultSet rs) throws SQLException {
        FineDTO dto = new FineDTO();
        dto.setFineId(rs.getLong("FineID"));
        dto.setMemberId(rs.getLong("MemberID"));
        dto.setMemberCode(rs.getString("MemberCode"));
        dto.setMemberName(rs.getString("FullName"));
        
        long borrowItemId = rs.getLong("BorrowItemID");
        if (!rs.wasNull()) {
            dto.setBorrowItemId(borrowItemId);
            dto.setBookTitle(rs.getString("BookTitle"));
            dto.setBarcode(rs.getString("Barcode"));
        }
        
        dto.setFineType(rs.getString("FineType"));
        dto.setAmount(rs.getBigDecimal("Amount"));
        dto.setPaidAmount(rs.getBigDecimal("PaidAmount"));
        dto.setStatus(rs.getString("Status"));
        
        if (rs.getTimestamp("IssuedAt") != null) {
            dto.setIssuedAt(rs.getTimestamp("IssuedAt").toLocalDateTime());
        }
        if (rs.getTimestamp("PaidAt") != null) {
            dto.setPaidAt(rs.getTimestamp("PaidAt").toLocalDateTime());
        }
        dto.setNotes(rs.getString("Notes"));
        return dto;
    }
}
