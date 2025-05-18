package com.example.proptech.dto.response;

import com.example.proptech.enums.ListingStatus;
import com.example.proptech.enums.ListingType;
import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class ListingSummaryResponseDto {
    private Long listingId;
    private String title;
    private AddressResponseDto address; // Chỉ cần thành phố hoặc quận/huyện tóm tắt
    private PropertyTypeResponseDto propertyType;
    private ListingType listingType;
    private BigDecimal price; // Giá chung (salePrice hoặc rentalPrice)
    private BigDecimal area; // Từ PropertyFeature
    private Integer bedrooms; // Từ PropertyFeature
    private String primaryImageUrl; // Lấy ảnh isPrimary = true
    private ListingStatus status;
    private UserSummaryResponseDto realtor; // DTO tóm tắt thông tin realtor
    private Timestamp createdAt;
    private boolean isFavorite; // (Sẽ thêm sau khi có chức năng favorite)
}