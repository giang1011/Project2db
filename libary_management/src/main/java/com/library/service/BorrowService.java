package com.library.service;

import com.library.model.BorrowItemDTO;
import com.library.model.Member;
import com.library.repository.BorrowDAO;

import java.sql.SQLException;
import java.util.List;

public class BorrowService {

    private final BorrowDAO borrowDAO;

    public BorrowService() {
        this.borrowDAO = new BorrowDAO();
    }

    public Member validateAndGetMember(String memberCode) throws SQLException {
        if (memberCode == null || memberCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã độc giả không được để trống.");
        }
        
        Member member = borrowDAO.findMemberByCode(memberCode.trim());
        if (member == null) {
            throw new SQLException("Không tìm thấy độc giả với mã: " + memberCode);
        }

        if (!"ACTIVE".equals(member.getStatus())) {
            throw new SQLException("Thẻ độc giả bị khóa hoặc không hoạt động.");
        }

        if (member.getMembershipEndDate() != null && member.getMembershipEndDate().isBefore(java.time.LocalDate.now())) {
            throw new SQLException("Thẻ độc giả bị khóa hoặc không hoạt động.");
        }

        return member;
    }

    public int getAvailableBorrowLimit(Member member) throws SQLException {
        int currentlyBorrowed = borrowDAO.getBorrowedBooksCount(member.getMemberId());
        int remaining = member.getMaxBorrowBooks() - currentlyBorrowed;
        return Math.max(0, remaining);
    }

    public BorrowItemDTO validateAndGetBookCopy(String barcode, int borrowDays) throws SQLException {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vạch không được để trống.");
        }

        BorrowItemDTO dto = borrowDAO.findBookCopyByBarcode(barcode.trim());
        if (dto == null) {
            throw new SQLException("Không tìm thấy cuốn sách với mã vạch: " + barcode);
        }

        // Set borrow date and due date
        dto.setBorrowDate(java.time.LocalDate.now());
        dto.setDueDate(java.time.LocalDate.now().plusDays(borrowDays));
        
        return dto;
    }

    public void checkoutBooks(Long memberId, Long userId, List<BorrowItemDTO> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mượn trống.");
        }
        if (userId == null) {
            throw new IllegalStateException("Không có thông tin người dùng đang đăng nhập.");
        }
        
        borrowDAO.checkoutBooks(memberId, userId, items);
    }

    public com.library.model.ReturnItemDTO findActiveBorrowItemByBarcode(String barcode) throws SQLException {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vạch không được để trống.");
        }
        com.library.model.ReturnItemDTO dto = borrowDAO.findActiveBorrowItemByBarcode(barcode.trim());
        if (dto == null) {
            throw new SQLException("Không tìm thấy thông tin đang mượn cho mã vạch này.");
        }
        return dto;
    }

    public List<com.library.model.ReturnItemDTO> findActiveBorrowItemsByMemberCode(String memberCode) throws SQLException {
        if (memberCode == null || memberCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã độc giả không được để trống.");
        }
        return borrowDAO.findActiveBorrowItemsByMemberCode(memberCode.trim());
    }

    public FineCalculationResult calculateFines(com.library.model.ReturnItemDTO item, String condition) {
        java.math.BigDecimal totalFine = java.math.BigDecimal.ZERO;
        StringBuilder reason = new StringBuilder();

        // 1. Overdue fine
        long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(item.getDueDate(), java.time.LocalDate.now());
        if (overdueDays > 0) {
            java.math.BigDecimal finePerDay = borrowDAO.getSystemSettingDecimal("OVERDUE_FINE_PER_DAY", new java.math.BigDecimal("5000"));
            java.math.BigDecimal overdueFine = finePerDay.multiply(new java.math.BigDecimal(overdueDays));
            totalFine = totalFine.add(overdueFine);
            reason.append("Quá hạn ").append(overdueDays).append(" ngày. ");
        }

        // 2. Condition fine
        if ("DAMAGED".equals(condition)) {
            // Flat fee for damaged
            java.math.BigDecimal damagedFine = new java.math.BigDecimal("50000"); 
            totalFine = totalFine.add(damagedFine);
            reason.append("Sách bị hư hỏng. ");
        } else if ("LOST".equals(condition)) {
            // Flat fee for lost (since no price in DB)
            java.math.BigDecimal lostFine = new java.math.BigDecimal("200000"); 
            totalFine = totalFine.add(lostFine);
            reason.append("Làm mất sách. ");
        }

        if (totalFine.compareTo(java.math.BigDecimal.ZERO) == 0) {
            reason.append("Sách trả đúng hạn, tình trạng tốt.");
        }

        return new FineCalculationResult(totalFine, reason.toString().trim());
    }

    public void checkinBook(com.library.model.ReturnItemDTO item, Long userId, String condition, String notes, java.math.BigDecimal fineAmount, boolean isFinePaid) throws SQLException {
        if (item == null) {
            throw new IllegalArgumentException("Thông tin trả sách không hợp lệ.");
        }
        if (userId == null) {
            throw new IllegalStateException("Không có thông tin người dùng đang đăng nhập.");
        }
        borrowDAO.checkinBook(item, userId, condition, notes, fineAmount, isFinePaid);
    }

    public static class FineCalculationResult {
        public java.math.BigDecimal amount;
        public String reason;
        public FineCalculationResult(java.math.BigDecimal amount, String reason) {
            this.amount = amount;
            this.reason = reason;
        }
    }
}
