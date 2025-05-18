package com.example.proptech.security.services;

import com.example.proptech.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String phone; // Sẽ là username cho Spring Security
    private String email;
    private String fullName;
    private BigDecimal balance;


    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String phone, String email, String fullName, BigDecimal balance, String password,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.phone = phone;
        this.email = email;
        this.fullName = fullName;
        this.balance = balance;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user) {
        // Chuyển đổi RoleType của bạn thành List<GrantedAuthority>
        // Ví dụ đơn giản là thêm tiền tố "ROLE_" cho tên role
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        return new UserDetailsImpl(
                user.getUserId(),
                user.getPhone(),
                user.getEmail(),
                user.getFullName(),
                user.getBalance(),
                user.getPassword(),
                authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return phone; // Sử dụng phone làm username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Bạn có thể kiểm tra user.getStatus() ở đây nếu cần
        return true; // Giả sử ACTIVE là không bị khóa
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Bạn có thể kiểm tra user.getStatus() == UserStatus.ACTIVE ở đây
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}