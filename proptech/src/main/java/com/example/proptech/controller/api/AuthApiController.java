package com.example.proptech.controller.api;

import com.example.proptech.dto.request.LoginRequest;
import com.example.proptech.dto.request.NewPasswordRequest;
import com.example.proptech.dto.request.OtpValidationRequest;
import com.example.proptech.dto.request.PasswordResetRequest;
import com.example.proptech.dto.request.RegisterRequest;
import com.example.proptech.dto.response.ApiResponse;
import com.example.proptech.dto.response.JwtResponse;
import com.example.proptech.dto.response.UserResponse;
import com.example.proptech.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600) // Cho phép CORS từ mọi nguồn, tùy chỉnh nếu cần
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse userResponse = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(userResponse, "User registered successfully!"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse, "User logged in successfully!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        authService.initiatePasswordReset(passwordResetRequest.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP has been sent to your email if it exists in our system."));
    }

    @PostMapping("/validate-otp")
    public ResponseEntity<ApiResponse<String>> validateOtp(@Valid @RequestBody OtpValidationRequest otpValidationRequest) {
        boolean isValid = authService.validateOtp(otpValidationRequest);
        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success(null,"OTP is valid. You can now reset your password."));
        } else {
            // BadRequestException sẽ được ném từ service nếu OTP sai/hết hạn và GlobalExceptionHandler sẽ xử lý
            // Hoặc bạn có thể trả về lỗi cụ thể ở đây
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(),"Invalid or expired OTP."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody NewPasswordRequest newPasswordRequest) {
        // Giả định validateOtp đã được gọi thành công trước đó và có cơ chế đảm bảo điều này
        // (ví dụ: client gửi một token tạm thời nhận được từ validate-otp thành công)
        // Để đơn giản, hiện tại service sẽ kiểm tra lại user.
        authService.resetPassword(newPasswordRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Password has been reset successfully."));
    }
}