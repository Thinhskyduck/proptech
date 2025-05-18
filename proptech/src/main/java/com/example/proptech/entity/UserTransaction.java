package com.example.proptech.entity;

import com.example.proptech.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_transactions")
public class UserTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Người dùng sở hữu giao dịch này

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private TransactionType transactionType;

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // Số tiền giao dịch (dương cho DEPOSIT/REFUND, âm cho POSTING_FEE/WITHDRAWAL)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Mô tả giao dịch, ví dụ: "Phí đăng tin #123", "Nạp tiền qua VNPay"

    // Optional: Liên kết đến listing nếu giao dịch này liên quan đến một tin đăng cụ thể
    // Cách 1: Lưu trực tiếp listing_id (đơn giản hơn)
    @Column(name = "related_listing_id")
    private Long relatedListingId;

    // Cách 2: Tạo quan hệ ManyToOne (nếu bạn muốn join dễ dàng, nhưng có thể null)
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "related_listing_id", referencedColumnName = "listing_id")
    // private Listing relatedListing;


    // Optional: ID giao dịch từ cổng thanh toán (nếu là DEPOSIT hoặc WITHDRAWAL)
    @Column(name = "vnpay_transaction_id", length = 100)
    private String vnpayTransactionId;


    @CreationTimestamp
    @Column(name = "transaction_date", updatable = false)
    private Timestamp transactionDate;


    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTransaction that = (UserTransaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "UserTransaction{" +
                "transactionId=" + transactionId +
                ", userId=" + (user != null ? user.getUserId() : null) +
                ", transactionType=" + transactionType +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", relatedListingId=" + relatedListingId +
                ", vnpayTransactionId='" + vnpayTransactionId + '\'' +
                ", transactionDate=" + transactionDate +
                '}';
    }
}