package com.example.proptech.enums;

public enum ListingStatus {
    DRAFT, // Tin nháp, realtor đang soạn, chưa submit, chưa trả phí
    PENDING_PAYMENT, // Realtor đã submit, chờ thanh toán phí
    PAID_PENDING_APPROVAL, // Đã trả phí, chờ Admin duyệt
    PENDING_APPROVAL, // (Có thể dùng cái này thay cho PAID_PENDING_APPROVAL nếu không tách bạch rõ việc trả phí và chờ duyệt)
    APPROVED, // Đã được duyệt, hiển thị public
    REJECTED, // Bị từ chối
    SOLD,     // Đã bán (cho tin SALE)
    RENTED,   // Đã cho thuê (cho tin RENTAL)
    EXPIRED   // Hết hạn
}