package com.library.model;

import java.time.LocalDate;

public class BorrowItemDTO {
    private long copyId;
    private String barcode;
    private String title;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private String status;
    private String shelfLocation;
    private String coverImage;
    
    public BorrowItemDTO() {}

    public BorrowItemDTO(long copyId, String barcode, String title, LocalDate borrowDate, LocalDate dueDate, String status, String shelfLocation, String coverImage) {
        this.copyId = copyId;
        this.barcode = barcode;
        this.title = title;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.status = status;
        this.shelfLocation = shelfLocation;
        this.coverImage = coverImage;
    }

    public long getCopyId() { return copyId; }
    public void setCopyId(long copyId) { this.copyId = copyId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getBorrowDate() { return borrowDate; }
    public void setBorrowDate(LocalDate borrowDate) { this.borrowDate = borrowDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
}
