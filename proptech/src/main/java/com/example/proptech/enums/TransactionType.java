package com.example.proptech.enums;

public enum TransactionType {
    DEPOSIT,            // Nạp tiền vào ví (từ VNPAY)
    POSTING_FEE,        // Trừ tiền phí đăng bài
    // REFUND,          // Bỏ nếu không có cơ chế refund tự động hoặc admin refund
    ADMIN_ADJUSTMENT_ADD, // Admin có thể cộng tiền (nếu sau này cần cho trường hợp đặc biệt)
    ADMIN_ADJUSTMENT_SUBTRACT // Admin có thể trừ tiền (nếu sau này cần)
}