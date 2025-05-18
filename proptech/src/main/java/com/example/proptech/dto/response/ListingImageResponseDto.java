package com.example.proptech.dto.response;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class ListingImageResponseDto {
    private Long imageId;
    private String imageUrl;
    private boolean isPrimary;
    private Timestamp uploadedAt;
}