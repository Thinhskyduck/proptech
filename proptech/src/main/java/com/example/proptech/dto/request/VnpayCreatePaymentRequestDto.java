package com.example.proptech.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VnpayCreatePaymentRequestDto {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000.00", message = "Minimum deposit amount is 10,000 VND") // VNPAY có quy định tối thiểu
    private BigDecimal amount;

    private String orderInfo; // Optional: Thông tin đơn hàng/giao dịch
}