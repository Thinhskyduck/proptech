package com.example.proptech.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PropertyFeatureRequestDto {
    @Min(value = 0, message = "Number of bedrooms cannot be negative")
    private Integer bedrooms;

    @Min(value = 0, message = "Number of bathrooms cannot be negative")
    private Integer bathrooms;

    @DecimalMin(value = "0.0", inclusive = false, message = "Area must be greater than 0") // Hoặc inclusive = true nếu cho phép diện tích = 0
    private BigDecimal area;

    private Boolean parking; // Mặc định sẽ là false nếu không gửi

    @Min(value = 0, message = "Number of floors cannot be negative")
    private Integer numFloors;

    private Short yearBuilt;

    @Size(max = 100)
    private String legalStatus;
}