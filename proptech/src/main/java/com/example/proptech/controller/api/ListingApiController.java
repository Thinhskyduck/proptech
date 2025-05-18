package com.example.proptech.controller.api;

import com.example.proptech.dto.request.ListingCreateRequestDto;
import com.example.proptech.dto.request.ListingUpdateRequestDto;
import com.example.proptech.dto.response.ApiResponse;
import com.example.proptech.dto.response.ListingDetailResponseDto;
import com.example.proptech.dto.response.ListingImageResponseDto;
import com.example.proptech.dto.response.ListingSummaryResponseDto;
import com.example.proptech.security.services.UserDetailsImpl;
import com.example.proptech.service.ListingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/listings")
public class ListingApiController {

    @Autowired
    private ListingService listingService;

    private String getCurrentUserPhone() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getUsername(); // Username là phone
        }
        return null;
    }

    // --- REALTOR APIs ---
    @PostMapping
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<ApiResponse<ListingDetailResponseDto>> createListing(@Valid @RequestBody ListingCreateRequestDto createRequest) {
        String realtorPhone = getCurrentUserPhone();
        if (realtorPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Realtor not authenticated."));
        }
        ListingDetailResponseDto createdListing = listingService.createListing(createRequest, realtorPhone);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createdListing, "Listing created successfully. Pending admin approval."));
    }

    @GetMapping("/my-listings")
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<ApiResponse<Page<ListingSummaryResponseDto>>> getMyListings(@PageableDefault(size = 10, sort = "createdAt,desc") Pageable pageable) {
        String realtorPhone = getCurrentUserPhone();
        if (realtorPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Realtor not authenticated."));
        }
        Page<ListingSummaryResponseDto> myListings = listingService.getMyListings(realtorPhone, pageable);
        return ResponseEntity.ok(ApiResponse.success(myListings, "Fetched realtor's listings successfully."));
    }

    @PutMapping("/{listingId}")
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<ApiResponse<ListingDetailResponseDto>> updateListing(@PathVariable Long listingId,
                                                                               @Valid @RequestBody ListingUpdateRequestDto updateRequest) {
        String realtorPhone = getCurrentUserPhone();
        if (realtorPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Realtor not authenticated."));
        }
        ListingDetailResponseDto updatedListing = listingService.updateListing(listingId, updateRequest, realtorPhone);
        return ResponseEntity.ok(ApiResponse.success(updatedListing, "Listing updated successfully."));
    }

    @DeleteMapping("/{listingId}")
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<ApiResponse<String>> deleteListing(@PathVariable Long listingId) {
        String realtorPhone = getCurrentUserPhone();
        if (realtorPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Realtor not authenticated."));
        }
        listingService.deleteListing(listingId, realtorPhone);
        return ResponseEntity.ok(ApiResponse.success(null, "Listing deleted successfully."));
    }

    @PostMapping(value = "/{listingId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<ApiResponse<List<ListingImageResponseDto>>> uploadImages(@PathVariable Long listingId,
                                                                                   @RequestParam("files") MultipartFile[] files) {
        String realtorPhone = getCurrentUserPhone();
        if (realtorPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Realtor not authenticated."));
        }
        List<ListingImageResponseDto> uploadedImages = listingService.uploadListingImages(listingId, realtorPhone, files);
        return ResponseEntity.ok(ApiResponse.success(uploadedImages, "Images uploaded successfully."));
    }

    @DeleteMapping("/{listingId}/images/{imageId}")
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<ApiResponse<String>> deleteImage(@PathVariable Long listingId, @PathVariable Long imageId) {
        String realtorPhone = getCurrentUserPhone();
        if (realtorPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Realtor not authenticated."));
        }
        listingService.deleteListingImage(listingId, imageId, realtorPhone);
        return ResponseEntity.ok(ApiResponse.success(null, "Image deleted successfully."));
    }

    @PutMapping("/{listingId}/images/{imageId}/set-primary")
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<ApiResponse<String>> setPrimaryImage(@PathVariable Long listingId, @PathVariable Long imageId) {
        String realtorPhone = getCurrentUserPhone();
        if (realtorPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Realtor not authenticated."));
        }
        listingService.setPrimaryImage(listingId, imageId, realtorPhone);
        return ResponseEntity.ok(ApiResponse.success(null, "Primary image set successfully."));
    }


    // --- PUBLIC APIs ---
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ListingSummaryResponseDto>>> getAllPublicListings(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Long propertyTypeId,
            @RequestParam(required = false) String listingType) { // listingType nên là String để dễ parse

        Page<ListingSummaryResponseDto> listings = listingService.getPublicListings(pageable, city, district, propertyTypeId, listingType);
        return ResponseEntity.ok(ApiResponse.success(listings, "Public listings fetched successfully."));
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<ApiResponse<ListingDetailResponseDto>> getPublicListingDetails(@PathVariable Long listingId) {
        ListingDetailResponseDto listingDetails = listingService.getPublicListingDetails(listingId);
        return ResponseEntity.ok(ApiResponse.success(listingDetails, "Listing details fetched successfully."));
    }
}