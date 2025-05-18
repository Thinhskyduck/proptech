package com.example.proptech.service.impl;

import com.example.proptech.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async; // Để gửi email bất đồng bộ
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${proptech.app.mail.from}") // Lấy từ application.properties
    private String mailFrom;

    @Override
    @Async // Đánh dấu để Spring thực thi phương thức này trong một thread riêng
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Your Password Reset OTP - PropTech");
            message.setText("Dear User,\n\nYour One Time Password (OTP) to reset your password is: "
                    + otp
                    + "\n\nThis OTP is valid for 10 minutes."
                    + "\n\nIf you did not request this, please ignore this email."
                    + "\n\nRegards,\nThe PropTech Team");

            javaMailSender.send(message);
            logger.info("OTP email sent successfully to {}", toEmail);
        } catch (MailException e) {
            logger.error("Error sending OTP email to {}: {}", toEmail, e.getMessage());
            // Có thể throw một custom exception ở đây để controller biết và xử lý
            // hoặc chỉ log lỗi và user sẽ không nhận được email
        }
    }
}