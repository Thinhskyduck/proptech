package com.example.proptech.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
// Bỏ import Lombok
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.Objects; // Thêm import

// Bỏ @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor
@Entity
@Table(name = "password_resets")
public class PasswordReset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reset_id")
    private Long resetId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(nullable = false)
    private String token; // OTP

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Timestamp expiresAt;

    // Constructors
    public PasswordReset() {
    }

    public PasswordReset(User user, String token, Timestamp expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public Long getResetId() {
        return resetId;
    }

    public void setResetId(Long resetId) {
        this.resetId = resetId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordReset that = (PasswordReset) o;
        return Objects.equals(resetId, that.resetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resetId);
    }

    @Override
    public String toString() {
        return "PasswordReset{" +
                "resetId=" + resetId +
                ", user=" + (user != null ? user.getUserId() : null) + // Tránh NullPointerException
                ", token='" + token + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}