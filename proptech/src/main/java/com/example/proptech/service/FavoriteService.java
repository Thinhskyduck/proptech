package com.example.proptech.service;

import com.example.proptech.dto.response.ListingSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FavoriteService {
    void addFavorite(Long listingId, String userPhone);
    void removeFavorite(Long listingId, String userPhone);
    Page<ListingSummaryResponseDto> getMyFavorites(String userPhone, Pageable pageable);
    boolean isListingFavoritedByUser(Long listingId, Long userId); // Helper method
}