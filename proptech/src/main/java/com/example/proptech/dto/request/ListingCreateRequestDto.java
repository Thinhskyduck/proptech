package com.example.proptech.dto.request;

import com.example.proptech.enums.ListingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ListingCreateRequestDto {
    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull(message = "Address information is required")
    @Valid // Quan trọng: để validate các trường bên trong AddressRequestDto
    private AddressRequestDto address;

    @NotNull(message = "Property type ID is required")
    private Long propertyTypeId; // ID của PropertyType

    @NotNull(message = "Listing type is required (SALE or RENTAL)")
    private ListingType listingType;

    @DecimalMin(value = "0.0", inclusive = true, message = "Sale price cannot be negative")
    private BigDecimal salePrice; // Nullable nếu là RENTAL

    @DecimalMin(value = "0.0", inclusive = true, message = "Rental price cannot be negative")
    private BigDecimal rentalPrice; // Nullable nếu là SALE

    @NotNull(message = "Property features are required")
    @Valid // Quan trọng: để validate các trường bên trong PropertyFeatureRequestDto
    private PropertyFeatureRequestDto features;

    // Phí đăng bài có thể được xác định bởi hệ thống dựa trên loại tin, khu vực,...
    // Hoặc realtor có thể chọn gói tin và phí được tính.
    // Tạm thời, giả sử phí được set cố định hoặc tính toán ở backend.
    // private BigDecimal postingFeeAmount; // Nếu client gửi lên
}