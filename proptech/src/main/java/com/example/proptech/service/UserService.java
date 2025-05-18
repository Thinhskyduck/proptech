package com.example.proptech.service;

import com.example.proptech.dto.request.ChangePasswordRequest;
import com.example.proptech.dto.request.UserUpdateRequest;
import com.example.proptech.dto.response.UserResponse;
import com.example.proptech.entity.User; // Import User entity

import java.util.Optional;

public interface UserService {
    Optional<User> findByPhone(String phone); // Trả về Entity User
    Optional<User> findByEmail(String email); // Trả về Entity User
    UserResponse getUserProfile(String phone); // Lấy thông tin user hiện tại
    UserResponse updateUserProfile(String phone, UserUpdateRequest userUpdateRequest);
    void changePassword(String phone, ChangePasswordRequest changePasswordRequest);
}