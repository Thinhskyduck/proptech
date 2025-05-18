package com.example.proptech.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.sql.Timestamp; // Hoặc LocalDateTime nếu bạn muốn xử lý timezone

@Data
public class AppointmentRequestDto {
    @NotNull(message = "Listing ID is required")
    private Long listingId;

    @NotNull(message = "Scheduled time is required")
    @Future(message = "Scheduled time must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh") // Ví dụ format
    private Timestamp scheduledAt; // Client gửi dạng "2025-06-15 14:30:00"

    private String notesCustomer;
}