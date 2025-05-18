package com.example.proptech.repository;

import com.example.proptech.entity.User;
import com.example.proptech.entity.UserTransaction;
import com.example.proptech.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTransactionRepository extends JpaRepository<UserTransaction, Long> {
    Page<UserTransaction> findByUserOrderByTransactionDateDesc(User user, Pageable pageable);
    // Thêm các phương thức tìm kiếm khác nếu cần
    // Trong UserTransactionRepository.java
    boolean existsByVnpayTransactionIdAndTransactionType(String vnpayTransactionId, TransactionType transactionType);}