package com.example.proptech.controller.api;

import com.example.proptech.dto.response.ApiResponse;
import com.example.proptech.dto.response.ListingSummaryResponseDto;
import com.example.proptech.security.services.UserDetailsImpl;
import com.example.proptech.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("isAuthenticated()") // Hầu hết các API yêu thích cần đăng nhập
public class FavoriteApiController {

    @Autowired
    private FavoriteService favoriteService;

    private String getCurrentUserPhone() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getUsername(); // Phone
        }
        return null;
    }
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getId();
        }
        return null;
    }


    @PostMapping("/{listingId}")
    public ResponseEntity<ApiResponse<String>> addFavorite(@PathVariable Long listingId) {
        String userPhone = getCurrentUserPhone();
        if (userPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not authenticated."));
        }
        favoriteService.addFavorite(listingId, userPhone);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(null, "Listing added to favorites."));
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<ApiResponse<String>> removeFavorite(@PathVariable Long listingId) {
        String userPhone = getCurrentUserPhone();
        if (userPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not authenticated."));
        }
        favoriteService.removeFavorite(listingId, userPhone);
        return ResponseEntity.ok(ApiResponse.success(null, "Listing removed from favorites."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ListingSummaryResponseDto>>> getMyFavorites(
            @PageableDefault(size = 10, sort = "createdAt,desc") Pageable pageable) {
        String userPhone = getCurrentUserPhone();
        if (userPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not authenticated."));
        }
        Page<ListingSummaryResponseDto> favorites = favoriteService.getMyFavorites(userPhone, pageable);
        return ResponseEntity.ok(ApiResponse.success(favorites, "Fetched favorite listings."));
    }

    @GetMapping("/status/{listingId}") // API để kiểm tra một tin có được yêu thích hay không
    public ResponseEntity<ApiResponse<Boolean>> getFavoriteStatus(@PathVariable Long listingId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not authenticated."));
        }
        boolean isFavorited = favoriteService.isListingFavoritedByUser(listingId, userId);
        return ResponseEntity.ok(ApiResponse.success(isFavorited, "Favorite status fetched."));
    }
}