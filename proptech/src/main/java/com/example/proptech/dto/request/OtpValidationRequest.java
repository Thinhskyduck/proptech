package com.example.proptech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OtpValidationRequest {
    @NotBlank(message = "Email is required") // Hoặc một định danh khác nếu bạn không dùng email
    private String identifier; // Có thể là email hoặc một token tạm thời bạn trả về sau khi gửi OTP

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be 6 digits") // Giả sử OTP 6 chữ số
    private String otp;
}