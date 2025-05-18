package com.example.proptech.service.impl;

import com.example.proptech.service.EmailService; // << IMPORT
import com.example.proptech.dto.request.LoginRequest;
import com.example.proptech.dto.request.NewPasswordRequest;
import com.example.proptech.dto.request.OtpValidationRequest;
import com.example.proptech.dto.request.RegisterRequest;
import com.example.proptech.dto.response.JwtResponse;
import com.example.proptech.dto.response.UserResponse;
import com.example.proptech.entity.PasswordReset;
import com.example.proptech.entity.User;
import com.example.proptech.enums.RoleType;
import com.example.proptech.exception.BadRequestException;
import com.example.proptech.exception.ResourceNotFoundException;
import com.example.proptech.repository.PasswordResetRepository;
import com.example.proptech.repository.UserRepository;
import com.example.proptech.security.jwt.JwtUtils;
import com.example.proptech.security.services.UserDetailsImpl;
import com.example.proptech.service.AuthService;
import org.modelmapper.ModelMapper; // Nếu bạn dùng ModelMapper
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired(required = false) // Để optional nếu bạn không dùng ModelMapper cho mọi thứ
    private ModelMapper modelMapper;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private EmailService emailService; // << TIÊM EMAIL SERVICE

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRATION_MINUTES = 10; // OTP hết hạn sau 10 phút


    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByPhone(registerRequest.getPhone())) {
            throw new BadRequestException("Error: Phone number is already taken!");
        }
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Error: Email is already in use!");
        }

        User user = new User();
        user.setFullName(registerRequest.getFullName());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(registerRequest.getRole());
        // Các trường khác như balance, status đã có default

        User savedUser = userRepository.save(user);

        if (modelMapper != null) {
            return modelMapper.map(savedUser, UserResponse.class);
        }
        // Chuyển đổi thủ công nếu không có modelMapper
        UserResponse response = new UserResponse();
        response.setUserId(savedUser.getUserId());
        response.setEmail(savedUser.getEmail());
        // ... các trường khác
        return response;
    }

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getPhone(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(), // getUsername() trả về phone
                userDetails.getEmail(),
                roles);
    }

    private String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Xóa các OTP cũ của user này (nếu có)
        passwordResetRepository.deleteAllByUser(user);

        String otp = generateOtp();
        PasswordReset passwordResetToken = new PasswordReset();
        passwordResetToken.setUser(user);
        passwordResetToken.setToken(otp); // Lưu OTP
        passwordResetToken.setExpiresAt(Timestamp.from(Instant.now().plus(OTP_EXPIRATION_MINUTES, ChronoUnit.MINUTES)));
        passwordResetRepository.save(passwordResetToken);

        // Gửi email chứa OTP
        emailService.sendOtpEmail(user.getEmail(), otp);
        // Ban đầu, để test, bạn có thể log OTP ra console:
        // logger.info("Generated OTP for user {}: {}", email, otp);
    }

    @Override
    @Transactional
    public boolean validateOtp(OtpValidationRequest otpValidationRequest) {
        User user = userRepository.findByEmail(otpValidationRequest.getIdentifier()) // Giả sử identifier là email
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + otpValidationRequest.getIdentifier()));

        PasswordReset passwordResetToken = passwordResetRepository.findByUserAndToken(user, otpValidationRequest.getOtp())
                .orElseThrow(() -> new BadRequestException("Invalid OTP."));

        if (passwordResetToken.getExpiresAt().before(Timestamp.from(Instant.now()))) {
            passwordResetRepository.delete(passwordResetToken); // Xóa token hết hạn
            throw new BadRequestException("OTP has expired.");
        }
        // Không xóa token ở đây vội, để resetPassword còn dùng để xác nhận
        return true;
    }

    @Override
    @Transactional
    public void resetPassword(NewPasswordRequest newPasswordRequest) {
        // SỬA Ở ĐÂY: Dùng getEmail() thay vì getIdentifier()
        User user = userRepository.findByEmail(newPasswordRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + newPasswordRequest.getEmail()));

        // Nếu bạn đang validate OTP ở đây (theo Cách 3), bạn sẽ dùng newPasswordRequest.getOtp()
        PasswordReset passwordResetToken = passwordResetRepository
                .findByUserAndToken(user, newPasswordRequest.getOtp()) // Giả sử NewPasswordRequest có getOtp()
                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP. Please request a new one or validate OTP again."));

        if (passwordResetToken.getExpiresAt().before(Timestamp.from(Instant.now()))) {
            passwordResetRepository.delete(passwordResetToken); // Xóa token hết hạn
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        user.setPassword(passwordEncoder.encode(newPasswordRequest.getNewPassword()));
        userRepository.save(user);

        passwordResetRepository.delete(passwordResetToken);
        // Hoặc passwordResetRepository.deleteAllByUser(user);
    }
}