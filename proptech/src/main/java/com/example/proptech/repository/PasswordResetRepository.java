package com.example.proptech.repository;

import com.example.proptech.entity.PasswordReset;
import com.example.proptech.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {
    Optional<PasswordReset> findByTokenAndUser(String token, User user);
    Optional<PasswordReset> findByUserAndToken(User user, String token); // Thêm cái này để dễ tìm
    void deleteAllByUser(User user); // Để xóa các token cũ khi user yêu cầu token mới hoặc reset thành công
}