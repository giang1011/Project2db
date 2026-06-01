package com.library.model;

public class DashboardMetrics {
    private int borrowingBooks;
    private int overdueBooks;
    private int pendingProfiles;
    private int unpaidFines;

    public DashboardMetrics() {}

    public int getBorrowingBooks() { return borrowingBooks; }
    public void setBorrowingBooks(int borrowingBooks) { this.borrowingBooks = borrowingBooks; }

    public int getOverdueBooks() { return overdueBooks; }
    public void setOverdueBooks(int overdueBooks) { this.overdueBooks = overdueBooks; }

    public int getPendingProfiles() { return pendingProfiles; }
    public void setPendingProfiles(int pendingProfiles) { this.pendingProfiles = pendingProfiles; }

    public int getUnpaidFines() { return unpaidFines; }
    public void setUnpaidFines(int unpaidFines) { this.unpaidFines = unpaidFines; }
}
