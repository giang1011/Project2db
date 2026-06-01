package com.library.model;

import java.time.LocalDate;
import java.util.List;

public class BookDTO {
    private String isbn;
    private String title;
    private int publisherId;
    private int publicationYear;
    private String language;
    private String description;
    private int pageCount;
    private String coverImage; // Path: uploads/books/...
    
    // Many-to-many references
    private List<Integer> authorIds;
    private List<Integer> categoryIds;
    
    // Copies info
    private int copyCount;
    private String shelfLocation;
    private LocalDate acquisitionDate;
    
    private Long createdBy; // UserID

    // Getters and Setters
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPublisherId() { return publisherId; }
    public void setPublisherId(int publisherId) { this.publisherId = publisherId; }

    public int getPublicationYear() { return publicationYear; }
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public List<Integer> getAuthorIds() { return authorIds; }
    public void setAuthorIds(List<Integer> authorIds) { this.authorIds = authorIds; }

    public List<Integer> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Integer> categoryIds) { this.categoryIds = categoryIds; }

    public int getCopyCount() { return copyCount; }
    public void setCopyCount(int copyCount) { this.copyCount = copyCount; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
