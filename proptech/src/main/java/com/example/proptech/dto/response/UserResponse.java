package com.example.proptech.dto.response;

import com.example.proptech.enums.RoleType;
import com.example.proptech.enums.UserStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class UserResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String phone;
    private RoleType role;
    private UserStatus status;
    private String avatarUrl;
    private BigDecimal balance;
    private Timestamp createdAt;
}