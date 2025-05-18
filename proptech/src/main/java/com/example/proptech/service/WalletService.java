package com.example.proptech.service;

import java.math.BigDecimal;

public interface WalletService {
    void deductUserBalance(Long userId, BigDecimal amount); // Chỉ trừ tiền
    void processVnpayReturn(Long userId, BigDecimal amount, String vnpTransactionNo, String description);
}