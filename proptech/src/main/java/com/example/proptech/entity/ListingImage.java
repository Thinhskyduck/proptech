package com.example.proptech.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
// Bỏ import Lombok nếu không dùng
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "listing_images")
public class ListingImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @NotBlank
    @Column(name = "image_url", nullable = false)
    private String imageUrl; // Sẽ lưu ID từ MongoDB hoặc đường dẫn file trên server

    @Column(name = "is_primary", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isPrimary = false;

    // Bạn có thể thêm order_index nếu muốn sắp xếp thứ tự ảnh
    // @Column(name = "order_index")
    // private Integer orderIndex;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private Timestamp uploadedAt;

    public ListingImage(Listing listing, String imageUrl, boolean isPrimary) {
        this.listing = listing;
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListingImage that = (ListingImage) o;
        return Objects.equals(imageId, that.imageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageId);
    }
}