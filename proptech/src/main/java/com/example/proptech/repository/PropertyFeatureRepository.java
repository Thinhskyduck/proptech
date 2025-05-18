package com.example.proptech.repository;

import com.example.proptech.entity.PropertyFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PropertyFeatureRepository extends JpaRepository<PropertyFeature, Long> {
    // Optional<PropertyFeature> findByListing_ListingId(Long listingId); // Nếu cần tìm feature theo listingId
}