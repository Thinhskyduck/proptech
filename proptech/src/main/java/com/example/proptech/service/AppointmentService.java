package com.example.proptech.service;

import com.example.proptech.dto.request.AppointmentRequestDto;
import com.example.proptech.dto.response.AppointmentResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppointmentService {
    AppointmentResponseDto createAppointment(AppointmentRequestDto requestDto, String customerPhone);
    Page<AppointmentResponseDto> getMyAppointments(String userPhone, String roleContext, Pageable pageable); // roleContext: "customer" hoặc "realtor"
    AppointmentResponseDto confirmAppointment(Long appointmentId, String realtorPhone);
    AppointmentResponseDto cancelAppointment(Long appointmentId, String userPhone, String cancelReason); // User có thể là customer hoặc realtor
    AppointmentResponseDto completeAppointment(Long appointmentId, String realtorPhone);
}