package com.library.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUtil {

    private static final String UPLOAD_DIR = "uploads/books/";

    /**
     * Copy ảnh được chọn vào thư mục uploads/books/ và trả về đường dẫn tương đối.
     * @param sourceFile File ảnh được chọn từ FileChooser
     * @return Đường dẫn tương đối lưu trong Database (VD: uploads/books/book_abc.jpg)
     * @throws IOException Nếu có lỗi trong quá trình copy
     */
    public static String saveImage(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            return null;
        }

        // Create directory if not exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Create safe filename with UUID to avoid duplication
        String originalName = sourceFile.getName();
        String extension = "";
        int extIndex = originalName.lastIndexOf(".");
        if (extIndex > 0) {
            extension = originalName.substring(extIndex);
        }
        
        String safeFileName = "book_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        Path destinationPath = uploadPath.resolve(safeFileName);

        // Copy file (Overwrite if exists)
        Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path with / for consistent DB storage
        return UPLOAD_DIR + safeFileName;
    }
}

