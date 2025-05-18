package com.example.proptech.dto.response;

import lombok.Data;

@Data
public class UserSummaryResponseDto {
    private Long userId;
    private String fullName;
    private String phone;
    private String avatarUrl;
}