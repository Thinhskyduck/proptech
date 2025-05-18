package com.example.proptech.controller.api;

import com.example.proptech.dto.request.AppointmentRequestDto;
import com.example.proptech.dto.request.CancelAppointmentRequestDto; // Sẽ tạo DTO này
import com.example.proptech.dto.response.ApiResponse;
import com.example.proptech.dto.response.AppointmentResponseDto;
import com.example.proptech.security.services.UserDetailsImpl;
import com.example.proptech.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/appointments")
public class AppointmentApiController {

    @Autowired
    private AppointmentService appointmentService;

    private String getCurrentUserPhone() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getUsername(); // Phone
        }
        return null;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'REALTOR')") // Customer hoặc Realtor có thể tạo lịch hẹn
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> createAppointment(@Valid @RequestBody AppointmentRequestDto requestDto) {
        String customerPhone = getCurrentUserPhone();
        if (customerPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not authenticated."));
        }
        AppointmentResponseDto createdAppointment = appointmentService.createAppointment(requestDto, customerPhone);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(createdAppointment, "Appointment created successfully. Waiting for realtor confirmation."));
    }

    @GetMapping("/my-appointments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<AppointmentResponseDto>>> getMyAppointments(
            @RequestParam String roleContext, // "customer" hoặc "realtor"
            @PageableDefault(size = 10, sort = "scheduledAt,desc") Pageable pageable) {
        String userPhone = getCurrentUserPhone();
        if (userPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not authenticated."));
        }
        Page<AppointmentResponseDto> appointments = appointmentService.getMyAppointments(userPhone, roleContext, pageable);
        return ResponseEntity.ok(ApiResponse.success(appointments, "Fetched appointments successfully."));
    }

    @PutMapping("/{appointmentId}/confirm")
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> confirmAppointment(@PathVariable Long appointmentId) {
        String realtorPhone = getCurrentUserPhone();
        if (realtorPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Realtor not authenticated."));
        }
        AppointmentResponseDto confirmedAppointment = appointmentService.confirmAppointment(appointmentId, realtorPhone);
        return ResponseEntity.ok(ApiResponse.success(confirmedAppointment, "Appointment confirmed."));
    }

    @PutMapping("/{appointmentId}/cancel")
    @PreAuthorize("isAuthenticated()") // Cả Customer và Realtor đều có thể hủy
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> cancelAppointment(
            @PathVariable Long appointmentId,
            @RequestBody(required = false) CancelAppointmentRequestDto cancelRequest) { // Lý do hủy là tùy chọn
        String userPhone = getCurrentUserPhone();
        if (userPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "User not authenticated."));
        }
        String reason = (cancelRequest != null) ? cancelRequest.getReason() : null;
        AppointmentResponseDto cancelledAppointment = appointmentService.cancelAppointment(appointmentId, userPhone, reason);
        return ResponseEntity.ok(ApiResponse.success(cancelledAppointment, "Appointment cancelled."));
    }

    @PutMapping("/{appointmentId}/complete")
    @PreAuthorize("hasRole('REALTOR')")
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> completeAppointment(@PathVariable Long appointmentId) {
        String realtorPhone = getCurrentUserPhone();
        if (realtorPhone == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Realtor not authenticated."));
        }
        AppointmentResponseDto completedAppointment = appointmentService.completeAppointment(appointmentId, realtorPhone);
        return ResponseEntity.ok(ApiResponse.success(completedAppointment, "Appointment marked as completed."));
    }
}