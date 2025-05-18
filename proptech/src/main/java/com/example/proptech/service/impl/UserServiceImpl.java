package com.example.proptech.service.impl;

import com.example.proptech.dto.request.ChangePasswordRequest;
import com.example.proptech.dto.request.UserUpdateRequest;
import com.example.proptech.dto.response.UserResponse;
import com.example.proptech.entity.User;
import com.example.proptech.exception.BadRequestException;
import com.example.proptech.exception.ResourceNotFoundException;
import com.example.proptech.repository.UserRepository;
import com.example.proptech.security.services.UserDetailsImpl;
import com.example.proptech.service.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private ModelMapper modelMapper;

    @Override
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserResponse getUserProfile(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with phone: " + phone));
        if (modelMapper != null) {
            return modelMapper.map(user, UserResponse.class);
        }
        // Chuyển đổi thủ công
        UserResponse response = new UserResponse();
        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setBalance(user.getBalance());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(String phone, UserUpdateRequest userUpdateRequest) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with phone: " + phone));

        if (StringUtils.hasText(userUpdateRequest.getFullName())) {
            user.setFullName(userUpdateRequest.getFullName());
        }
        if (StringUtils.hasText(userUpdateRequest.getAvatarUrl())) {
            user.setAvatarUrl(userUpdateRequest.getAvatarUrl());
        }
        // Thêm các trường khác nếu cần cập nhật

        User updatedUser = userRepository.save(user);
        if (modelMapper != null) {
            return modelMapper.map(updatedUser, UserResponse.class);
        }
        // Chuyển đổi thủ công
        UserResponse response = new UserResponse();
        // ... (lấy các trường từ updatedUser)
        return response;
    }

    @Override
    @Transactional
    public void changePassword(String phone, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with phone: " + phone));

        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect current password");
        }
        if (changePasswordRequest.getNewPassword().equals(changePasswordRequest.getCurrentPassword())) {
            throw new BadRequestException("New password cannot be the same as the current password");
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
    }

    // Helper method để lấy user đang đăng nhập (nếu cần)
    private UserDetailsImpl getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }
        return null; // Hoặc throw exception nếu user phải được xác thực
    }
}