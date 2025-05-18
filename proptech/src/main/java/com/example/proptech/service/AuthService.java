package com.example.proptech.service;

import com.example.proptech.dto.request.LoginRequest;
import com.example.proptech.dto.request.NewPasswordRequest;
import com.example.proptech.dto.request.OtpValidationRequest;
import com.example.proptech.dto.request.RegisterRequest;
import com.example.proptech.dto.response.JwtResponse;
import com.example.proptech.dto.response.UserResponse;

public interface AuthService {
    UserResponse registerUser(RegisterRequest registerRequest);
    JwtResponse authenticateUser(LoginRequest loginRequest);
    void initiatePasswordReset(String email);
    boolean validateOtp(OtpValidationRequest otpValidationRequest); // Trả về true nếu OTP hợp lệ
    void resetPassword(NewPasswordRequest newPasswordRequest);
}