package com.example.proptech.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp);
    // Có thể thêm các phương thức gửi email khác sau này
}