package com.example.proptech.dto.response;

import com.example.proptech.enums.ListingStatus;
import com.example.proptech.enums.ListingType;
import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
public class ListingDetailResponseDto {
    private Long listingId;
    private String title;
    private String description;
    private AddressResponseDto address;
    private PropertyTypeResponseDto propertyType;
    private ListingStatus status;
    private ListingType listingType;
    private BigDecimal salePrice;
    private BigDecimal rentalPrice;
    private UserSummaryResponseDto realtor; // Thông tin người đăng
    private BigDecimal postingFeeAmount;
    private boolean postingFeePaid;
    private Timestamp expiresAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private PropertyFeatureResponseDto features;
    private List<ListingImageResponseDto> images;
    // Các thông tin khác nếu cần
}