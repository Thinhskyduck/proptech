package com.example.proptech.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PropertyFeatureResponseDto {
    // private Long featureId; // Có thể không cần trả về ID của feature
    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal area;
    private Boolean parking;
    private Integer numFloors;
    private Short yearBuilt;
    private String legalStatus;
}