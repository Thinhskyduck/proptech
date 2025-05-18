package com.example.proptech.controller.api;

import com.example.proptech.dto.request.ChangePasswordRequest;
import com.example.proptech.dto.request.UserUpdateRequest;
import com.example.proptech.dto.response.ApiResponse;
import com.example.proptech.dto.response.UserResponse;
import com.example.proptech.security.services.UserDetailsImpl; // Import UserDetailsImpl
import com.example.proptech.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Cho phép phân quyền dựa trên role
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserApiController {

    @Autowired
    private UserService userService;

    private String getCurrentUserPhone() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getUsername(); // getUsername() trả về phone
        }
        // Hoặc throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        return null;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()") // Chỉ user đã đăng nhập mới được truy cập
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        String currentUserPhone = getCurrentUserPhone();
        if (currentUserPhone == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "User not authenticated"));
        }
        UserResponse userProfile = userService.getUserProfile(currentUserPhone);
        return ResponseEntity.ok(ApiResponse.success(userProfile, "User profile fetched successfully."));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(@Valid @RequestBody UserUpdateRequest userUpdateRequest) {
        String currentUserPhone = getCurrentUserPhone();
        if (currentUserPhone == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "User not authenticated"));
        }
        UserResponse updatedUser = userService.updateUserProfile(currentUserPhone, userUpdateRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile updated successfully."));
    }

    @PutMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        String currentUserPhone = getCurrentUserPhone();
        if (currentUserPhone == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "User not authenticated"));
        }
        userService.changePassword(currentUserPhone, changePasswordRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully."));
    }
}