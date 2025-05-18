package com.example.proptech.enums;

public enum AppointmentStatus {
    PENDING,    // Chờ Realtor xác nhận
    CONFIRMED,  // Realtor đã xác nhận
    COMPLETED,  // Buổi hẹn đã hoàn thành
    CANCELLED_BY_CUSTOMER,
    CANCELLED_BY_REALTOR,
    EXPIRED     // (Tùy chọn, nếu lịch hẹn quá hạn mà không ai làm gì)
}