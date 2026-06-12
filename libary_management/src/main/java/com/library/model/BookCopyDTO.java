package com.library.model;

import java.time.LocalDate;

public class BookCopyDTO {
    private long copyId;
    private long bookId;
    private String barcode;
    private String shelfLocation;
    private String physicalCondition;
    private String circulationStatus;
    private LocalDate acquisitionDate;

    public BookCopyDTO() {}

    public long getCopyId() { return copyId; }
    public void setCopyId(long copyId) { this.copyId = copyId; }

    public long getBookId() { return bookId; }
    public void setBookId(long bookId) { this.bookId = bookId; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public String getPhysicalCondition() { return physicalCondition; }
    public void setPhysicalCondition(String physicalCondition) { this.physicalCondition = physicalCondition; }

    public String getCirculationStatus() { return circulationStatus; }
    public void setCirculationStatus(String circulationStatus) { this.circulationStatus = circulationStatus; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }
}
