package com.example.proptech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ListingRejectRequestDto {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}