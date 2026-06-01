package com.library.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FineDTO {
    private long fineId;
    private long memberId;
    private String memberCode;
    private String memberName;
    private Long borrowItemId;
    private String bookTitle;
    private String barcode;
    private String fineType;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime paidAt;
    private String notes;

    public FineDTO() {}

    public long getFineId() { return fineId; }
    public void setFineId(long fineId) { this.fineId = fineId; }

    public long getMemberId() { return memberId; }
    public void setMemberId(long memberId) { this.memberId = memberId; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public Long getBorrowItemId() { return borrowItemId; }
    public void setBorrowItemId(Long borrowItemId) { this.borrowItemId = borrowItemId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getFineType() { return fineType; }
    public void setFineType(String fineType) { this.fineType = fineType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
