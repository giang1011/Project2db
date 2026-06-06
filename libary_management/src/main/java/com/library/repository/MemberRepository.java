package com.library.repository;

import com.library.model.Member;
import com.library.model.MemberStudentProfile;
import com.library.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MemberRepository {

    private static final Logger logger = LoggerFactory.getLogger(MemberRepository.class);

    public boolean saveMember(Member member, MemberStudentProfile studentProfile) throws SQLException {
        String insertMemberSql = "INSERT INTO Members (MemberCode, FullName, Email, Phone, MemberType, " +
                "DateOfBirth, Address, MembershipStartDate, MembershipEndDate, MaxBorrowBooks, " +
                "BorrowDurationDays, Status, CreatedBy, UpdatedBy) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String insertStudentProfileSql = "INSERT INTO MemberStudentProfiles (MemberID, SchoolName, StudentCode, " +
                "StudentStatus, StudentVerificationStatus) VALUES (?, ?, ?, ?, ?)";

        Connection connection = null;
        PreparedStatement memberStmt = null;
        PreparedStatement studentProfileStmt = null;
        ResultSet generatedKeys = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false); // Begin transaction
            
            // Sinh ma tu dong
            String generatedCode = generateMemberCode(connection, member.getMemberType());
            member.setMemberCode(generatedCode);

            memberStmt = connection.prepareStatement(insertMemberSql, Statement.RETURN_GENERATED_KEYS);
            memberStmt.setString(1, member.getMemberCode());
            memberStmt.setString(2, member.getFullName());
            memberStmt.setString(3, member.getEmail());
            memberStmt.setString(4, member.getPhone());
            memberStmt.setString(5, member.getMemberType());
            memberStmt.setDate(6, member.getDateOfBirth() != null ? Date.valueOf(member.getDateOfBirth()) : null);
            memberStmt.setString(7, member.getAddress());
            memberStmt.setDate(8, Date.valueOf(member.getMembershipStartDate()));
            memberStmt.setDate(9, Date.valueOf(member.getMembershipEndDate()));
            memberStmt.setInt(10, member.getMaxBorrowBooks());
            memberStmt.setInt(11, member.getBorrowDurationDays());
            memberStmt.setString(12, member.getStatus() != null ? member.getStatus() : "ACTIVE");
            
            if (member.getCreatedBy() != null) {
                memberStmt.setLong(13, member.getCreatedBy());
            } else {
                memberStmt.setNull(13, java.sql.Types.BIGINT);
            }
            if (member.getUpdatedBy() != null) {
                memberStmt.setLong(14, member.getUpdatedBy());
            } else {
                memberStmt.setNull(14, java.sql.Types.BIGINT);
            }

            int affectedRows = memberStmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating member failed, no rows affected.");
            }

            generatedKeys = memberStmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                long memberId = generatedKeys.getLong(1);
                member.setMemberId(memberId);

                if ("STUDENT".equals(member.getMemberType()) && studentProfile != null) {
                    studentProfileStmt = connection.prepareStatement(insertStudentProfileSql);
                    studentProfileStmt.setLong(1, memberId);
                    studentProfileStmt.setString(2, studentProfile.getSchoolName());
                    studentProfileStmt.setString(3, studentProfile.getStudentCode());
                    studentProfileStmt.setString(4, studentProfile.getStudentStatus() != null ? studentProfile.getStudentStatus() : "ACTIVE");
                    studentProfileStmt.setString(5, studentProfile.getStudentVerificationStatus() != null ? studentProfile.getStudentVerificationStatus() : "PENDING");
                    
                    studentProfileStmt.executeUpdate();
                }
            } else {
                throw new SQLException("Creating member failed, no ID obtained.");
            }

            connection.commit(); // Commit transaction
            return true;

        } catch (SQLException e) {
            if (connection != null) {
                try {
                    logger.error("Transaction failed, attempting to rollback", e);
                    connection.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            throw e;
        } finally {
            if (generatedKeys != null) {
                try { generatedKeys.close(); } catch (SQLException e) { logger.error("Error closing ResultSet", e); }
            }
            if (memberStmt != null) {
                try { memberStmt.close(); } catch (SQLException e) { logger.error("Error closing Statement", e); }
            }
            if (studentProfileStmt != null) {
                try { studentProfileStmt.close(); } catch (SQLException e) { logger.error("Error closing Statement", e); }
            }
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error closing Connection", e);
                }
            }
        }
    }

    /**
     * Ham sinh ma thanh vien tu dong dua tren loai thanh vien va nam hien tai.
     * Kiem tra ma lon nhat trong nam de tang so thu tu len 1.
     */
    private String generateMemberCode(Connection connection, String memberType) throws SQLException {
        int year = java.time.Year.now().getValue();
        String prefix = "STUDENT".equals(memberType) ? "STU" : "NOR";
        String searchPattern = prefix + year + "%";
        
        String sql = "SELECT MAX(MemberCode) FROM Members WHERE MemberCode LIKE ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, searchPattern);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getString(1) != null) {
                    String maxCode = rs.getString(1);
                    try {
                        // Lay 4 ky tu cuoi cung lam so thu tu
                        String seqStr = maxCode.substring(maxCode.length() - 4);
                        int nextSeq = Integer.parseInt(seqStr) + 1;
                        return prefix + year + String.format("%04d", nextSeq);
                    } catch (Exception e) {
                        logger.error("Loi khi parse ma thanh vien: " + maxCode, e);
                        // Neu co loi parse, fallback ve 0001 hoac ban the xu ly tiep
                        return prefix + year + "0001";
                    }
                } else {
                    // Chua co ma nao trong nam nay
                    return prefix + year + "0001";
                }
            }
        }
    }

    /**
     * Lay tat ca doc gia tu Database
     */
    public java.util.List<Member> getAllMembers() throws SQLException {
        java.util.List<Member> members = new java.util.ArrayList<>();
        String query = "SELECT MemberID, MemberCode, FullName, Email, Phone, MemberType, " +
                       "DateOfBirth, Address, MembershipStartDate, MembershipEndDate, " +
                       "MaxBorrowBooks, BorrowDurationDays, Status " +
                       "FROM Members ORDER BY CreatedAt DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
             
            while (rs.next()) {
                Member member = new Member();
                member.setMemberId(rs.getLong("MemberID"));
                member.setMemberCode(rs.getString("MemberCode"));
                member.setFullName(rs.getString("FullName"));
                member.setEmail(rs.getString("Email"));
                member.setPhone(rs.getString("Phone"));
                member.setMemberType(rs.getString("MemberType"));
                if (rs.getDate("DateOfBirth") != null) {
                    member.setDateOfBirth(rs.getDate("DateOfBirth").toLocalDate());
                }
                member.setAddress(rs.getString("Address"));
                if (rs.getDate("MembershipStartDate") != null) {
                    member.setMembershipStartDate(rs.getDate("MembershipStartDate").toLocalDate());
                }
                if (rs.getDate("MembershipEndDate") != null) {
                    member.setMembershipEndDate(rs.getDate("MembershipEndDate").toLocalDate());
                }
                member.setMaxBorrowBooks(rs.getInt("MaxBorrowBooks"));
                member.setBorrowDurationDays(rs.getInt("BorrowDurationDays"));
                member.setStatus(rs.getString("Status"));
                
                members.add(member);
            }
        }
        return members;
    }

    /**
     * Cap nhat thong tin doc gia
     */
    public boolean updateMember(Member member) throws SQLException {
        String sql = "UPDATE Members SET FullName = ?, Email = ?, Phone = ?, Address = ? WHERE MemberID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setString(1, member.getFullName());
            stmt.setString(2, member.getEmail());
            stmt.setString(3, member.getPhone());
            stmt.setString(4, member.getAddress());
            stmt.setLong(5, member.getMemberId());
            
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Dinh chi doc gia (Soft delete)
     */
    public boolean suspendMember(Long memberId) throws SQLException {
        String sql = "UPDATE Members SET Status = 'SUSPENDED' WHERE MemberID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
             
            stmt.setLong(1, memberId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean renewMember(Long memberId, java.time.LocalDate newStartDate, java.time.LocalDate newEndDate, java.math.BigDecimal amount, Long processedBy) throws SQLException {
        String updateMemberSql = "UPDATE Members SET MembershipStartDate = ?, MembershipEndDate = ?, Status = 'ACTIVE' WHERE MemberID = ?";
        String insertPaymentSql = "INSERT INTO MembershipPayments (MemberID, Amount, PaymentType, ProcessedBy) VALUES (?, ?, 'RENEWAL', ?)";
        
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);
            
            try (PreparedStatement memberStmt = connection.prepareStatement(updateMemberSql)) {
                memberStmt.setDate(1, java.sql.Date.valueOf(newStartDate));
                memberStmt.setDate(2, java.sql.Date.valueOf(newEndDate));
                memberStmt.setLong(3, memberId);
                memberStmt.executeUpdate();
            }
            
            try (PreparedStatement paymentStmt = connection.prepareStatement(insertPaymentSql)) {
                paymentStmt.setLong(1, memberId);
                paymentStmt.setBigDecimal(2, amount);
                if (processedBy != null) {
                    paymentStmt.setLong(3, processedBy);
                } else {
                    paymentStmt.setNull(3, java.sql.Types.BIGINT);
                }
                paymentStmt.executeUpdate();
            }
            
            connection.commit();
            return true;
        } catch (SQLException e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ex) { logger.error("Loi rollback", ex); }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error closing Connection", e);
                }
            }
        }
    }

    public int updateExpiredMembersAndCountStudent() throws SQLException {
        String updateSql = "UPDATE Members SET Status = 'EXPIRED' WHERE Status = 'ACTIVE' AND MembershipEndDate < CAST(GETDATE() AS DATE)";
        String countSql = "SELECT COUNT(*) FROM Members WHERE MemberType = 'STUDENT' AND Status = 'EXPIRED'";
        
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             PreparedStatement countStmt = conn.prepareStatement(countSql)) {
             
             stmt.executeUpdate(updateSql);
             
             try (ResultSet rs = countStmt.executeQuery()) {
                 if (rs.next()) {
                     return rs.getInt(1);
                 }
             }
        }
        return 0;
    }

    public boolean confirmStudentToNormal(Long memberId, Long processedBy) throws SQLException {
        String updateMemberSql = "UPDATE Members SET MemberType = 'NORMAL', MaxBorrowBooks = 3, BorrowDurationDays = 14 WHERE MemberID = ? AND MemberType = 'STUDENT' AND Status = 'EXPIRED'";
        String insertPaymentSql = "INSERT INTO MembershipPayments (MemberID, Amount, PaymentType, PaymentMethod, ProcessedBy) VALUES (?, ?, 'NEW_CARD', 'CASH', ?)";
        
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);
            
            int updatedRows = 0;
            try (PreparedStatement stmt = connection.prepareStatement(updateMemberSql)) {
                stmt.setLong(1, memberId);
                updatedRows = stmt.executeUpdate();
            }
            
            if (updatedRows > 0) {
                try (PreparedStatement paymentStmt = connection.prepareStatement(insertPaymentSql)) {
                    paymentStmt.setLong(1, memberId);
                    paymentStmt.setBigDecimal(2, new java.math.BigDecimal("100000")); // MEMBERSHIP_FEE_NORMAL
                    if (processedBy != null) {
                        paymentStmt.setLong(3, processedBy);
                    } else {
                        paymentStmt.setNull(3, java.sql.Types.BIGINT);
                    }
                    paymentStmt.executeUpdate();
                }
                connection.commit();
                return true;
            } else {
                connection.rollback();
                return false;
            }
        } catch (SQLException e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ex) { logger.error("Loi rollback", ex); }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    logger.error("Error closing Connection", e);
                }
            }
        }
    }

    public java.util.List<Member> searchMembers(String keyword) throws SQLException {
        java.util.List<Member> members = new java.util.ArrayList<>();
        String query = "SELECT MemberID, MemberCode, FullName, Email, Phone, MemberType, " +
                       "DateOfBirth, Address, MembershipStartDate, MembershipEndDate, " +
                       "MaxBorrowBooks, BorrowDurationDays, Status " +
                       "FROM Members " +
                       "WHERE FullName LIKE ? OR MemberCode LIKE ? " +
                       "ORDER BY FullName ASC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
             
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Member member = new Member();
                    member.setMemberId(rs.getLong("MemberID"));
                    member.setMemberCode(rs.getString("MemberCode"));
                    member.setFullName(rs.getString("FullName"));
                    member.setEmail(rs.getString("Email"));
                    member.setPhone(rs.getString("Phone"));
                    member.setMemberType(rs.getString("MemberType"));
                    if (rs.getDate("DateOfBirth") != null) {
                        member.setDateOfBirth(rs.getDate("DateOfBirth").toLocalDate());
                    }
                    member.setAddress(rs.getString("Address"));
                    if (rs.getDate("MembershipStartDate") != null) {
                        member.setMembershipStartDate(rs.getDate("MembershipStartDate").toLocalDate());
                    }
                    if (rs.getDate("MembershipEndDate") != null) {
                        member.setMembershipEndDate(rs.getDate("MembershipEndDate").toLocalDate());
                    }
                    member.setMaxBorrowBooks(rs.getInt("MaxBorrowBooks"));
                    member.setBorrowDurationDays(rs.getInt("BorrowDurationDays"));
                    member.setStatus(rs.getString("Status"));
                    
                    members.add(member);
                }
            }
        }
        return members;
    }
}
