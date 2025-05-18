package com.example.proptech.entity;

import jakarta.persistence.*;
// Bỏ import Lombok nếu không dùng
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "property_features")
public class PropertyFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Hoặc dùng listing_id làm PK nếu bạn muốn 1-1 chặt chẽ hơn và không cần ID riêng
    @Column(name = "feature_id")
    private Long featureId;

    // Quan hệ OneToOne với Listing (bên sở hữu Foreign Key)
    @OneToOne(fetch = FetchType.LAZY, optional = false) // optional=false nghĩa là PropertyFeature phải luôn có Listing
    @JoinColumn(name = "listing_id", nullable = false, unique = true) // unique = true để đảm bảo 1-1
    private Listing listing;

    private Integer bedrooms;
    private Integer bathrooms;

    @Column(precision = 10, scale = 2)
    private BigDecimal area; // Diện tích

    private Boolean parking = false; // default false

    @Column(name = "num_floors")
    private Integer numFloors;

    @Column(name = "year_built")
    private Short yearBuilt;

    @Column(name = "legal_status", length = 100)
    private String legalStatus;

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyFeature that = (PropertyFeature) o;
        return Objects.equals(featureId, that.featureId); // Hoặc Objects.equals(listing, that.listing) nếu listing_id là PK
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureId); // Hoặc Objects.hash(listing)
    }
}