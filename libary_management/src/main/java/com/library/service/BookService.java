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

public class BookService {
    private final BookDAO bookDAO;

    public BookService() {
        this.bookDAO = new BookDAO();
    }

    /**
     * Thêm sách mới với toàn bộ các bước được quản lý bằng Transaction
     * Rollback nếu bất kỳ bước nào thất bại.
     */
    public void addBook(BookDTO bookDTO) throws Exception {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Tắt AutoCommit để bắt đầu Transaction
            conn.setAutoCommit(false); 

            // Bước 1: Thêm vào bảng Books, lấy BookID
            long bookId = bookDAO.insertBook(conn, bookDTO);

            // Bước 2: Thêm liên kết Tác giả vào bảng BookAuthors
            bookDAO.insertBookAuthors(conn, bookId, bookDTO.getAuthorIds());

            // Bước 3: Thêm liên kết Thể loại vào bảng BookCategories
            bookDAO.insertBookCategories(conn, bookId, bookDTO.getCategoryIds());

            // Bước 4: Thêm vào bảng BookCopies theo số lượng CopyCount
            int copyCount = bookDTO.getCopyCount();
            if (copyCount <= 0) {
                throw new Exception("Số lượng bản sao (CopyCount) phải lớn hơn 0");
            }
            
            for (int i = 0; i < copyCount; i++) {
                // Generate barcode động cho từng copy
                String barcode = BarcodeGenerator.generateBarcode();
                bookDAO.insertBookCopy(conn, bookId, barcode, bookDTO.getShelfLocation(), bookDTO.getAcquisitionDate());
            }

            // Commit Transaction khi tất cả đều thành công
            conn.commit(); 
        } catch (Exception e) {
            // Nếu có lỗi, Rollback toàn bộ dữ liệu
            if (conn != null) {
                try {
                    conn.rollback(); 
                } catch (Exception rollbackEx) {
                    throw new Exception("Lỗi nghiêm trọng khi rollback transaction: " + rollbackEx.getMessage(), e);
                }
            }
            // Quăng lỗi ra cho Controller xử lý hiển thị
            throw e;
        } finally {
            if (conn != null) {
                try {
                    // Trả lại trạng thái AutoCommit và đóng connection
                    conn.setAutoCommit(true); 
                    conn.close();
                } catch (Exception closeEx) {
                    // Cảnh báo log lỗi đóng connection
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
}
