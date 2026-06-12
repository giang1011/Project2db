package com.library.service;

import com.library.model.Author;
import com.library.model.BookDTO;
import com.library.model.Category;
import com.library.model.Publisher;
import com.library.model.Book;
import com.library.repository.BookDAO;
import com.library.util.BarcodeGenerator;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.util.List;
import com.library.util.UserSession;
import com.library.model.User;

public class BookService {
    private final BookDAO bookDAO;
    private final ActivityLogService activityLogService;

    public BookService() {
        this.bookDAO = new BookDAO();
        this.activityLogService = new ActivityLogService();
    }

    /**
     * Thêm sách mới với toàn bộ các bước được quản lý bằng Transaction
     * Rollback nếu bất kỳ bước nào thất bại.
     */
    public void addBook(BookDTO bookDTO) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Disable AutoCommit to begin Transaction
            conn.setAutoCommit(false); 

            // Step 1: Insert into Books, get BookID
            long bookId = bookDAO.insertBook(conn, bookDTO);

            // Step 2: Insert Author links into BookAuthors
            bookDAO.insertBookAuthors(conn, bookId, bookDTO.getAuthorIds());

            // Step 3: Insert Category links into BookCategories
            bookDAO.insertBookCategories(conn, bookId, bookDTO.getCategoryIds());

            // Step 4: Insert into BookCopies based on CopyCount
            int copyCount = bookDTO.getCopyCount();
            if (copyCount <= 0) {
                throw new Exception("Số lượng bản sao (CopyCount) phải lớn hơn 0");
            }
            
            for (int i = 0; i < copyCount; i++) {
                // Generate dynamic barcode for each copy
                String barcode = BarcodeGenerator.generateBarcode();
                bookDAO.insertBookCopy(conn, bookId, barcode, bookDTO.getShelfLocation(), bookDTO.getAcquisitionDate());
            }

            // Commit Transaction when all succeed
            conn.commit(); 
            
            // Log action after successful commit
            long userId = 1;
            User user = UserSession.getInstance().getLoggedInUser();
            if (user != null) userId = user.getUserId();
            activityLogService.logAction(userId, "Add Book", null, "New Book: " + bookDTO.getTitle() + " (" + copyCount + " copies)");
        } catch (Exception e) {
            // If error, Rollback all data
            if (conn != null) {
                try {
                    conn.rollback(); 
                } catch (Exception rollbackEx) {
                    throw new Exception("Lỗi nghiêm trọng khi rollback transaction: " + rollbackEx.getMessage(), e);
                }
            }
            // Throw error for Controller to display
            throw e;
        } finally {
            if (conn != null) {
                try {
                    // Restore AutoCommit state and close connection
                    conn.setAutoCommit(true); 
                    conn.close();
                } catch (Exception closeEx) {
                    // Warning log for connection close error
                }
            }
        }
    }

    public List<Author> getAllAuthors() throws Exception {
        return bookDAO.getAllAuthors();
    }

    public List<Category> getAllCategories() throws Exception {
        return bookDAO.getAllCategories();
    }

    public List<Publisher> getAllPublishers() throws Exception {
        return bookDAO.getAllPublishers();
    }

    public List<Book> getAllBooksDetails() throws Exception {
        return bookDAO.getAllBooksDetails();
    }

    public Author addAuthor(String name) throws Exception {
        if (name == null || name.trim().isEmpty()) throw new Exception("Tên tác giả không hợp lệ");
        return bookDAO.insertAuthor(name.trim());
    }

    public Category addCategory(String name) throws Exception {
        if (name == null || name.trim().isEmpty()) throw new Exception("Tên thể loại không hợp lệ");
        return bookDAO.insertCategory(name.trim());
    }

    public Publisher addPublisher(String name) throws Exception {
        if (name == null || name.trim().isEmpty()) throw new Exception("Tên nhà xuất bản không hợp lệ");
        return bookDAO.insertPublisher(name.trim());
    }

    public void lockBook(long bookId) throws Exception {
        bookDAO.lockBook(bookId);
        
        long userId = 1;
        User user = UserSession.getInstance().getLoggedInUser();
        if (user != null) userId = user.getUserId();
        
        activityLogService.logAction(userId, "Lock Book", null, "Locked Book ID: " + bookId);
    }

    public boolean recoverLostBookCopy(String barcode) throws Exception {
        if (barcode == null || barcode.trim().isEmpty()) {
            throw new Exception("Mã vạch không được để trống");
        }
        boolean success = bookDAO.recoverLostBookCopy(barcode.trim());
        if (success) {
            long userId = 1;
            User user = UserSession.getInstance().getLoggedInUser();
            if (user != null) userId = user.getUserId();
            activityLogService.logAction(userId, "Recover Book", null, "Recovered lost book with barcode: " + barcode);
        }
        return success;
    }

    public List<com.library.model.BookCopyDTO> getBookCopies(long bookId) throws Exception {
        return bookDAO.getBookCopies(bookId);
    }
}

