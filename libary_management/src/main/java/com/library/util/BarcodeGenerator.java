package com.library.util;

import java.time.Year;
import java.util.UUID;

public class BarcodeGenerator {
    
    /**
     * Sinh Barcode tự động theo định dạng: BOOK-{Năm}-{Mã ngẫu nhiên 6 ký tự}
     * Ví dụ: BOOK-2026-A1B2C3
     * Đảm bảo tính unique và dễ quét trong thư viện thực tế.
     */
    public static String generateBarcode() {
        int currentYear = Year.now().getValue();
        // Get first 6 characters of UUID as random code
        String randomStr = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return String.format("BOOK-%d-%s", currentYear, randomStr);
    }
}

