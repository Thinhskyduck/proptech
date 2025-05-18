package com.example.proptech.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String phone;
    private String email;
    private List<String> roles; // Hoặc chỉ một RoleType role;

    public JwtResponse(String accessToken, Long id, String phone, String email, List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.phone = phone;
        this.email = email;
        this.roles = roles;
    }
}