package com.example.proptech.controller.api;

import com.example.proptech.config.VnpayConfig; // Bạn sẽ tạo file config này
import com.example.proptech.dto.request.VnpayCreatePaymentRequestDto;
import com.example.proptech.dto.response.ApiResponse;
import com.example.proptech.security.services.UserDetailsImpl;
import com.example.proptech.service.UserService;
import com.example.proptech.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/payment/vnpay")
public class PaymentApiController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentApiController.class);

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserService userService; // Để lấy thông tin user hiện tại

    // Endpoint để Realtor tạo yêu cầu thanh toán
    @PostMapping("/create-payment")
    public ResponseEntity<ApiResponse<String>> createPaymentUrl(
            @RequestBody VnpayCreatePaymentRequestDto paymentRequestDto,
            HttpServletRequest req) throws UnsupportedEncodingException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not authenticated."));
        }
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // --- Logic tạo URL VNPAY ---
        // Các thông số này bạn cần lấy từ VnpayConfig hoặc hardcode cho demo
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_OrderInfo = paymentRequestDto.getOrderInfo() != null ? paymentRequestDto.getOrderInfo() : "Nap tien user " + userId;
        String orderType = "billpayment"; // Hoặc các loại khác VNPAY hỗ trợ
        String vnp_TxnRef = VnpayConfig.getRandomNumber(8); // Mã giao dịch của bạn, phải là duy nhất
        String vnp_IpAddr = VnpayConfig.getIpAddress(req);
        String vnp_TmnCode = VnpayConfig.vnp_TmnCode; // Lấy từ config

        long amount = paymentRequestDto.getAmount().multiply(new BigDecimal(100)).longValue(); // VNPAY yêu cầu amount * 100

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        // vnp_Params.put("vnp_BankCode", "NCB"); // Bỏ qua để VNPAY hiển thị cổng chọn ngân hàng
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn"); // Ngôn ngữ vn hoặc en
        vnp_Params.put("vnp_ReturnUrl", VnpayConfig.vnp_ReturnUrl + "?userId=" + userId); // Thêm userId vào returnUrl để xử lý
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15); // Thời gian hết hạn thanh toán (ví dụ: 15 phút)
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build data to hash
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnpayConfig.hmacSHA512(VnpayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VnpayConfig.vnp_PayUrl + "?" + queryUrl;

        logger.info("VNPAY Payment URL created: {}", paymentUrl);
        return ResponseEntity.ok(ApiResponse.success(paymentUrl, "VNPAY payment URL created successfully."));
    }

    // Endpoint để VNPAY redirect về sau khi người dùng thanh toán (Return URL)
    // Hoặc bạn có thể dùng một endpoint khác cho IPN (Instant Payment Notification)
    @GetMapping("/return")
    public ResponseEntity<ApiResponse<String>> vnpayReturn(
            @RequestParam Map<String, String> queryParams,
            @RequestParam("userId") Long userId // Lấy userId từ query param đã truyền khi tạo URL
    ) {
        // --- Logic xử lý kết quả từ VNPAY ---
        // Tham khảo tài liệu VNPAY để xác thực chữ ký (vnp_SecureHash) và các tham số khác
        // Ví dụ đơn giản:
        String vnp_ResponseCode = queryParams.get("vnp_ResponseCode");
        String vnp_TransactionNo = queryParams.get("vnp_TransactionNo"); // Mã giao dịch của VNPAY
        String vnp_Amount = queryParams.get("vnp_Amount"); // Số tiền * 100

        // TODO: Xác thực chữ ký vnp_SecureHash trước khi xử lý

        logger.info("VNPAY Return Data: {}", queryParams);
        logger.info("Processing VNPAY return for userId: {}", userId);


        if ("00".equals(vnp_ResponseCode)) { // Giao dịch thành công
            BigDecimal amountDecimal = new BigDecimal(vnp_Amount).divide(new BigDecimal(100));
            String orderInfo = queryParams.getOrDefault("vnp_OrderInfo", "Deposit from VNPAY");

            // Gọi WalletService để cập nhật số dư và ghi nhận giao dịch
            try {
                walletService.processVnpayReturn(userId, amountDecimal, vnp_TransactionNo, orderInfo);
                // Chuyển hướng người dùng về trang thông báo thành công trên frontend của bạn
                // Hoặc trả về thông báo thành công
                logger.info("Payment successful for userId: {}, amount: {}, VNP TxnNo: {}", userId, amountDecimal, vnp_TransactionNo);
                // Bạn có thể trả về URL để redirect client:
                // String redirectUrl = "http://your-frontend-url/payment-success?txnRef=" + queryParams.get("vnp_TxnRef");
                // return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
                return ResponseEntity.ok(ApiResponse.success(null,"Payment successful. Your balance has been updated. Transaction No: " + vnp_TransactionNo));
            } catch (Exception e) {
                logger.error("Error processing VNPAY return for userId {}: {}", userId, e.getMessage());
                // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),"Error processing payment: " + e.getMessage()));
                // Chuyển hướng về trang lỗi hoặc thông báo lỗi
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),"Error processing payment. Please contact support. VNP TxnNo: " + vnp_TransactionNo));
            }
        } else {
            // Giao dịch thất bại hoặc bị hủy
            logger.warn("Payment failed or cancelled for userId: {}. VNPAY Response Code: {}", userId, vnp_ResponseCode);
            // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(),"Payment failed or cancelled. Response Code: " + vnp_ResponseCode));
            // Chuyển hướng về trang lỗi hoặc thông báo lỗi
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(),"Payment failed or cancelled. VNP TxnNo: " + vnp_TransactionNo));
        }
    }
}