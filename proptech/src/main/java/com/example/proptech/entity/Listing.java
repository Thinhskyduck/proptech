package com.example.proptech.entity;

import com.example.proptech.enums.ListingStatus; // Sẽ tạo enum này
import com.example.proptech.enums.ListingType;   // Sẽ tạo enum này
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
// Bỏ import Lombok nếu bạn không dùng, hoặc thêm lại nếu dùng
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
// @AllArgsConstructor // Cân nhắc nếu có quá nhiều trường
@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Long listingId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST) // Cascade PERSIST để khi lưu Listing, Address mới cũng được lưu nếu chưa có
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private PropertyType propertyType; // Đổi tên từ type_id thành propertyType cho đúng convention

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30) // Tăng length nếu tên status dài
    private ListingStatus status = ListingStatus.DRAFT; // Default là DRAFT

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false, length = 10)
    private ListingType listingType;

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "sale_price", precision = 15, scale = 2)
    private BigDecimal salePrice; // Có thể null nếu là RENTAL

    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "rental_price", precision = 15, scale = 2)
    private BigDecimal rentalPrice; // Có thể null nếu là SALE

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtor_id", nullable = false)
    private User realtor;

    @NotNull
    @Column(name = "posting_fee_amount", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal postingFeeAmount = BigDecimal.ZERO; // Phí đăng cho tin này

    // boolean posting_fee_paid; // Bạn đã có trường này trong DBML, có thể thêm vào đây.
    // Tuy nhiên, trạng thái thanh toán phí có thể được quản lý chặt chẽ hơn thông qua bảng UserTransaction
    // và trạng thái của Listing (ví dụ: PAID_PENDING_APPROVAL).
    // Nếu chỉ đơn giản là cờ, thì thêm vào:
    @Column(name = "posting_fee_paid", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean postingFeePaid = false;


    @Column(name = "expires_at")
    private Timestamp expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    // Relationship to PropertyFeatures (One-to-One)
    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private PropertyFeature features;

    // Relationship to ListingImages (One-to-Many)
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ListingImage> images = new ArrayList<>();

    // Helper methods for bidirectional relationships (nếu cần)
    public void addImage(ListingImage image) {
        images.add(image);
        image.setListing(this);
    }

    public void removeImage(ListingImage image) {
        images.remove(image);
        image.setListing(null);
    }

    public void setFeatures(PropertyFeature features) {
        if (features == null) {
            if (this.features != null) {
                this.features.setListing(null);
            }
        } else {
            features.setListing(this);
        }
        this.features = features;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Listing listing = (Listing) o;
        return Objects.equals(listingId, listing.listingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listingId);
    }
}