package com.example.proptech.controller.api;

import com.example.proptech.dto.request.ListingRejectRequestDto; // Sẽ tạo DTO này
import com.example.proptech.dto.response.ApiResponse;
import com.example.proptech.dto.response.ListingDetailResponseDto;
import com.example.proptech.dto.response.ListingSummaryResponseDto;
import com.example.proptech.security.services.UserDetailsImpl;
import com.example.proptech.service.ListingService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/listings") // Prefix /admin cho các API của admin liên quan đến listing
@PreAuthorize("hasRole('ADMIN')") // Tất cả các API trong controller này yêu cầu ROLE_ADMIN
public class AdminApiController {

    @Autowired
    private ListingService listingService;

    private Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        // Should not happen if @PreAuthorize("hasRole('ADMIN')") works correctly
        throw new IllegalStateException("Admin not authenticated or Principal is not UserDetailsImpl");
    }

    @GetMapping("/status/{status}") // Ví dụ: /api/admin/listings/status/PAID_PENDING_APPROVAL
    public ResponseEntity<ApiResponse<Page<ListingSummaryResponseDto>>> getListingsByStatus(
            @PathVariable String status,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ListingSummaryResponseDto> listings = listingService.getListingsByStatus(status.toUpperCase(), pageable);
        return ResponseEntity.ok(ApiResponse.success(listings, "Listings fetched for status: " + status));
    }

    @PutMapping("/{listingId}/approve")
    public ResponseEntity<ApiResponse<ListingDetailResponseDto>> approveListing(@PathVariable Long listingId) {
        Long adminId = getCurrentAdminId();
        ListingDetailResponseDto approvedListing = listingService.approveListing(listingId, adminId);
        return ResponseEntity.ok(ApiResponse.success(approvedListing, "Listing approved successfully."));
    }

    @PutMapping("/{listingId}/reject")
    public ResponseEntity<ApiResponse<ListingDetailResponseDto>> rejectListing(
            @PathVariable Long listingId,
            @Valid @RequestBody ListingRejectRequestDto rejectRequest) { // DTO chứa lý do
        Long adminId = getCurrentAdminId();
        ListingDetailResponseDto rejectedListing = listingService.rejectListing(listingId, rejectRequest.getReason(), adminId);
        return ResponseEntity.ok(ApiResponse.success(rejectedListing, "Listing rejected successfully."));
    }
}