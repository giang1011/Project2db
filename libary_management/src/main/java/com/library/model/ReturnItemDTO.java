package com.library.model;

import java.time.LocalDate;

public class ReturnItemDTO {
    private long borrowItemId;
    private long transactionId;
    private long copyId;
    private long memberId;
    private String barcode;
    private String title;
    private String borrowerName;
    private LocalDate dueDate;
    private String coverImage;

    public ReturnItemDTO() {}

    public long getBorrowItemId() { return borrowItemId; }
    public void setBorrowItemId(long borrowItemId) { this.borrowItemId = borrowItemId; }

    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }

    public long getCopyId() { return copyId; }
    public void setCopyId(long copyId) { this.copyId = copyId; }

    public long getMemberId() { return memberId; }
    public void setMemberId(long memberId) { this.memberId = memberId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
}
