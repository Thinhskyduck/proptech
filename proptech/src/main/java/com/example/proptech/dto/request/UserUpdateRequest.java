package com.example.proptech.dto.request;

import jakarta.validation.constraints.Size;
// Bỏ import Lombok nếu không dùng

// Bỏ @Data nếu không dùng Lombok
public class UserUpdateRequest {

    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    @Size(max = 255, message = "Avatar URL must be less than 255 characters")
    // Bạn có thể thêm @URL nếu muốn validate nó là một URL hợp lệ
    private String avatarUrl;

    // Thêm các trường khác nếu bạn cho phép cập nhật, ví dụ:
    // private String newEmail; (Cần xử lý xác thực email mới)

    // Constructors
    public UserUpdateRequest() {
    }

    public UserUpdateRequest(String fullName, String avatarUrl) {
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
    }

    // Getters
    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    // Setters
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}