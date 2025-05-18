package com.example.proptech.repository;

import com.example.proptech.entity.Favorite;
import com.example.proptech.entity.Listing;
import com.example.proptech.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndListing(User user, Listing listing);
    Page<Favorite> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    boolean existsByUserAndListing_ListingId(User user, Long listingId);
    void deleteByUserAndListing_ListingId(User user, Long listingId);
}