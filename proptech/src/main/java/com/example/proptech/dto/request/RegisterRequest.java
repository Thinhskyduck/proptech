package com.example.proptech.dto.request;

import com.example.proptech.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data; // Hoặc @Getter @Setter @ToString

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String password;

    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters")
    // Bạn có thể thêm @Pattern nếu muốn validate định dạng số điện thoại cụ thể
    private String phone;

    @NotNull(message = "Role is required")
    private RoleType role; // Sẽ là CUSTOMER hoặc REALTOR từ client
}