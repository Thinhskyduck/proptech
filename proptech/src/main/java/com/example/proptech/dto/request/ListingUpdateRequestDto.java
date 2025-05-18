package com.example.proptech.dto.request;

import com.example.proptech.enums.ListingType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ListingUpdateRequestDto {
    @Size(max = 255)
    private String title;

    private String description;

    @Valid
    private AddressRequestDto address; // Cho phép cập nhật địa chỉ

    private Long propertyTypeId;

    private ListingType listingType;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal salePrice;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal rentalPrice;

    @Valid
    private PropertyFeatureRequestDto features; // Cho phép cập nhật features
}