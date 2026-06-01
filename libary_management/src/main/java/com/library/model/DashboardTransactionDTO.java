package com.library.model;

import java.time.LocalDateTime;

public class DashboardTransactionDTO {
    private long transactionId;
    private String memberCode;
    private String memberName;
    private int booksBorrowedCount;
    private LocalDateTime borrowDate;
    private String processedBy;

    public DashboardTransactionDTO() {}

    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public int getBooksBorrowedCount() { return booksBorrowedCount; }
    public void setBooksBorrowedCount(int booksBorrowedCount) { this.booksBorrowedCount = booksBorrowedCount; }

    public LocalDateTime getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDateTime borrowDate) { this.borrowDate = borrowDate; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
}
