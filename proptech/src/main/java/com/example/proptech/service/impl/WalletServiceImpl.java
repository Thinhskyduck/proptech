package com.example.proptech.service.impl;

import com.example.proptech.entity.User;
import com.example.proptech.entity.UserTransaction;
import com.example.proptech.enums.TransactionType;
import com.example.proptech.exception.InsufficientBalanceException;
import com.example.proptech.exception.ResourceNotFoundException;
import com.example.proptech.repository.UserRepository;
import com.example.proptech.repository.UserTransactionRepository;
import com.example.proptech.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.proptech.enums.TransactionType; // Đảm bảo import đúng

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Override
    @Transactional
    public void deductUserBalance(Long userId, BigDecimal amountToDeduct) { // Thay đổi ở đây
        if (amountToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount to deduct must be positive.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (user.getBalance().compareTo(amountToDeduct) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Required: " + amountToDeduct + ", Available: " + user.getBalance());
        }
        user.setBalance(user.getBalance().subtract(amountToDeduct));
        userRepository.save(user);
        // Không tạo UserTransaction ở đây nữa
    }

    @Override
    @Transactional
    public void processVnpayReturn(Long userId, BigDecimal amount, String vnpTransactionNo, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Kiểm tra xem vnpTransactionNo đã được xử lý chưa để tránh cộng tiền nhiều lần
        if (userTransactionRepository.existsByVnpayTransactionIdAndTransactionType(vnpTransactionNo, TransactionType.DEPOSIT)) {
            System.out.println("VNPAY transaction " + vnpTransactionNo + " already processed.");
            return;
        }

        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        UserTransaction transaction = new UserTransaction();
        transaction.setUser(user);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(amount); // Số tiền cộng là dương
        transaction.setDescription(description != null ? description : "Deposit via VNPAY - TxnNo: " + vnpTransactionNo);
        transaction.setVnpayTransactionId(vnpTransactionNo); // Lưu mã giao dịch của VNPAY
        userTransactionRepository.save(transaction);
        System.out.println("User " + userId + " balance updated. New balance: " + user.getBalance());
    }

    // Bỏ phương thức refundFunds nếu không dùng
    /*
    @Override
    @Transactional
    public void refundFunds(Long userId, BigDecimal amountToRefund, String description, Long relatedListingId) {
        // ...
    }
    */
}