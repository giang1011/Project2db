package com.library.repository;

import com.library.model.Author;
import com.library.model.BookDTO;
import com.library.model.Category;
import com.library.model.Publisher;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.List;
import com.library.model.Book;

public class BookDAO {

    // Thêm sách mới vào bảng Books
    public long insertBook(Connection conn, BookDTO bookDTO) throws Exception {
        String query = "INSERT INTO Books (ISBN, Title, PublisherID, PublicationYear, Language, Description, PageCount, CoverImage, CreatedBy) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, bookDTO.getIsbn());
            stmt.setString(2, bookDTO.getTitle());
            stmt.setInt(3, bookDTO.getPublisherId());
            stmt.setInt(4, bookDTO.getPublicationYear());
            stmt.setString(5, bookDTO.getLanguage());
            stmt.setString(6, bookDTO.getDescription());
            stmt.setInt(7, bookDTO.getPageCount());
            stmt.setString(8, bookDTO.getCoverImage());
            
            if (bookDTO.getCreatedBy() != null) {
                stmt.setLong(9, bookDTO.getCreatedBy());
            } else {
                stmt.setNull(9, java.sql.Types.BIGINT);
            }

            stmt.executeUpdate();

            // Lấy BookID vừa được tạo
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new Exception("Thêm sách thất bại, không thể lấy BookID.");
                }
            }
        }
    }

    // Thêm danh sách tác giả cho sách
    public void insertBookAuthors(Connection conn, long bookId, List<Integer> authorIds) throws Exception {
        if (authorIds == null || authorIds.isEmpty()) return;
        
        String query = "INSERT INTO BookAuthors (BookID, AuthorID) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (Integer authorId : authorIds) {
                stmt.setLong(1, bookId);
                stmt.setInt(2, authorId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    // Thêm danh sách thể loại cho sách
    public void insertBookCategories(Connection conn, long bookId, List<Integer> categoryIds) throws Exception {
        if (categoryIds == null || categoryIds.isEmpty()) return;

        String query = "INSERT INTO BookCategories (BookID, CategoryID) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            for (Integer categoryId : categoryIds) {
                stmt.setLong(1, bookId);
                stmt.setInt(2, categoryId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    // Thêm bản sao của sách (Book Copy)
    public void insertBookCopy(Connection conn, long bookId, String barcode, String shelfLocation, LocalDate acquisitionDate) throws Exception {
        String query = "INSERT INTO BookCopies (BookID, Barcode, ShelfLocation, AcquisitionDate, PhysicalCondition, CirculationStatus, IsReferenceOnly, IsDeleted) " +
                       "VALUES (?, ?, ?, ?, 'GOOD', 'AVAILABLE', 0, 0)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, bookId);
            stmt.setString(2, barcode);
            stmt.setString(3, shelfLocation);
            
            if (acquisitionDate != null) {
                stmt.setDate(4, java.sql.Date.valueOf(acquisitionDate));
            } else {
                stmt.setNull(4, java.sql.Types.DATE);
            }
            
            stmt.executeUpdate();
        }
    }

    // Lấy danh sách tác giả
    public List<Author> getAllAuthors() throws Exception {
        List<Author> list = new ArrayList<>();
        String query = "SELECT AuthorID, AuthorName FROM Authors ORDER BY AuthorName";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Author(rs.getInt("AuthorID"), rs.getString("AuthorName")));
            }
        }
        return list;
    }

    // Lấy danh sách thể loại
    public List<Category> getAllCategories() throws Exception {
        List<Category> list = new ArrayList<>();
        String query = "SELECT CategoryID, CategoryName FROM Categories ORDER BY CategoryName";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("CategoryID"), rs.getString("CategoryName")));
            }
        }
        return list;
    }

    // Lấy danh sách nhà xuất bản
    public List<Publisher> getAllPublishers() throws Exception {
        List<Publisher> list = new ArrayList<>();
        String query = "SELECT PublisherID, PublisherName FROM Publishers ORDER BY PublisherName";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Publisher(rs.getInt("PublisherID"), rs.getString("PublisherName")));
            }
        }
        return list;
    }

    // Lấy danh sách sách đầy đủ cho màn hình Quản lý sách
    public List<Book> getAllBooksDetails() throws Exception {
        List<Book> list = new ArrayList<>();
        String query = 
            "SELECT b.BookID, b.ISBN, b.Title, p.PublisherName, b.PublicationYear, b.Language, b.PageCount, b.CoverImage, " +
            "(SELECT STRING_AGG(a.AuthorName, ', ') FROM BookAuthors ba JOIN Authors a ON ba.AuthorID = a.AuthorID WHERE ba.BookID = b.BookID) AS Authors, " +
            "(SELECT STRING_AGG(c.CategoryName, ', ') FROM BookCategories bc JOIN Categories c ON bc.CategoryID = c.CategoryID WHERE bc.BookID = b.BookID) AS Categories, " +
            "(SELECT COUNT(*) FROM BookCopies bc WHERE bc.BookID = b.BookID AND bc.IsDeleted = 0) AS TotalCopies, " +
            "(SELECT COUNT(*) FROM BookCopies bc WHERE bc.BookID = b.BookID AND bc.CirculationStatus = 'AVAILABLE' AND bc.IsDeleted = 0) AS AvailableCopies " +
            "FROM Books b " +
            "LEFT JOIN Publishers p ON b.PublisherID = p.PublisherID " +
            "WHERE b.IsDeleted = 0 " +
            "ORDER BY b.CreatedAt DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Book book = new Book();
                book.setBookId(rs.getLong("BookID"));
                book.setIsbn(rs.getString("ISBN"));
                book.setTitle(rs.getString("Title"));
                book.setPublisherName(rs.getString("PublisherName"));
                book.setPublicationYear(rs.getInt("PublicationYear"));
                book.setLanguage(rs.getString("Language"));
                book.setPageCount(rs.getInt("PageCount"));
                book.setCoverImage(rs.getString("CoverImage"));
                book.setAuthors(rs.getString("Authors"));
                book.setCategories(rs.getString("Categories"));
                book.setTotalCopies(rs.getInt("TotalCopies"));
                book.setAvailableCopies(rs.getInt("AvailableCopies"));
                list.add(book);
            }
        }
        return list;
    }
    // Thêm tác giả nhanh
    public Author insertAuthor(String authorName) throws Exception {
        String query = "INSERT INTO Authors (AuthorName) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, authorName);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return new Author(rs.getInt(1), authorName);
            }
        }
        throw new Exception("Không thể thêm Tác giả");
    }

    // Thêm thể loại nhanh
    public Category insertCategory(String categoryName) throws Exception {
        String query = "INSERT INTO Categories (CategoryName) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, categoryName);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return new Category(rs.getInt(1), categoryName);
            }
        }
        throw new Exception("Không thể thêm Thể loại");
    }

    // Thêm nhà xuất bản nhanh
    public Publisher insertPublisher(String publisherName) throws Exception {
        String query = "INSERT INTO Publishers (PublisherName) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, publisherName);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) return new Publisher(rs.getInt(1), publisherName);
            }
        }
        throw new Exception("Không thể thêm Nhà xuất bản");
    }
}
