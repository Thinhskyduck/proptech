package com.example.proptech.repository;

import com.example.proptech.entity.Listing;
import com.example.proptech.entity.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {
    List<ListingImage> findByListing(Listing listing);
    void deleteByListingAndImageUrl(Listing listing, String imageUrl); // Nếu cần xóa ảnh cụ thể
}