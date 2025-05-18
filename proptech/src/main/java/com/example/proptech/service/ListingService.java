package com.example.proptech.service;

import com.example.proptech.dto.request.ListingCreateRequestDto;
import com.example.proptech.dto.request.ListingUpdateRequestDto;
import com.example.proptech.dto.response.ListingDetailResponseDto;
import com.example.proptech.dto.response.ListingImageResponseDto;
import com.example.proptech.dto.response.ListingSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ListingService {

    // Cho Realtor
    ListingDetailResponseDto createListing(ListingCreateRequestDto createRequest, String realtorPhone);
    ListingDetailResponseDto updateListing(Long listingId, ListingUpdateRequestDto updateRequest, String realtorPhone);
    void deleteListing(Long listingId, String realtorPhone);
    Page<ListingSummaryResponseDto> getMyListings(String realtorPhone, Pageable pageable); // Lấy tin của realtor
    List<ListingImageResponseDto> uploadListingImages(Long listingId, String realtorPhone, MultipartFile[] files);
    void deleteListingImage(Long listingId, Long imageId, String realtorPhone);
    void setPrimaryImage(Long listingId, Long imageId, String realtorPhone);


    // Cho Public (Khách và Khách vãng lai)
    Page<ListingSummaryResponseDto> getPublicListings(Pageable pageable, String city, String district, Long propertyTypeId, String listingType); // Tìm kiếm cơ bản
    ListingDetailResponseDto getPublicListingDetails(Long listingId);

    // Cho Admin
    Page<ListingSummaryResponseDto> getListingsByStatus(String status, Pageable pageable); // Lấy tin theo trạng thái (PENDING_APPROVAL, PAID_PENDING_APPROVAL)
    ListingDetailResponseDto approveListing(Long listingId, Long adminId);
    ListingDetailResponseDto rejectListing(Long listingId, String reason, Long adminId);
}