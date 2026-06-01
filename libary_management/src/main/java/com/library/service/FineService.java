package com.library.service;

import com.library.model.FineDTO;
import com.library.repository.FineDAO;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class FineService {
    private final FineDAO fineDAO;

    public FineService() {
        this.fineDAO = new FineDAO();
    }

    public List<FineDTO> getAllFines() throws SQLException {
        return fineDAO.getAllFines();
    }

    public void payFine(long fineId, BigDecimal amountToPay) throws SQLException {
        if (amountToPay == null || amountToPay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");
        }
        fineDAO.updateFinePayment(fineId, amountToPay);
    }
}
