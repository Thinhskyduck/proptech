package com.example.proptech.dto.response;

import com.example.proptech.enums.AppointmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.sql.Timestamp;

@Data
public class AppointmentResponseDto {
    private Long appointmentId;
    private ListingSummaryResponseDto listingSummary; // Chỉ thông tin tóm tắt của listing
    private UserSummaryResponseDto customer;
    private UserSummaryResponseDto realtor;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    private Timestamp scheduledAt;
    private AppointmentStatus status;
    private String notesCustomer;
    private String notesRealtor;
    private Timestamp createdAt;
}