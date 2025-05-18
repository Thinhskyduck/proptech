package com.example.proptech.service.impl;

import com.example.proptech.dto.request.AppointmentRequestDto;
import com.example.proptech.dto.response.AppointmentResponseDto;
import com.example.proptech.dto.response.ListingSummaryResponseDto;
import com.example.proptech.dto.response.UserSummaryResponseDto;
import com.example.proptech.entity.Appointment;
import com.example.proptech.entity.Listing;
import com.example.proptech.entity.User;
import com.example.proptech.enums.AppointmentStatus;
import com.example.proptech.enums.ListingStatus;
import com.example.proptech.enums.ListingType;
import com.example.proptech.enums.RoleType;
import com.example.proptech.exception.BadRequestException;
import com.example.proptech.exception.ResourceNotFoundException;
import com.example.proptech.repository.AppointmentRepository;
import com.example.proptech.repository.ListingRepository;
import com.example.proptech.repository.UserRepository;
import com.example.proptech.service.AppointmentService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentServiceImpl.class);

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  UserRepository userRepository,
                                  ListingRepository listingRepository,
                                  ModelMapper modelMapper) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public AppointmentResponseDto createAppointment(AppointmentRequestDto requestDto, String customerPhone) {
        User customer = userRepository.findByPhone(customerPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "phone", customerPhone));

        if (customer.getRole() != RoleType.CUSTOMER && customer.getRole() != RoleType.REALTOR) { // Realtor cũng có thể đặt lịch như customer
            throw new AccessDeniedException("Only customers or realtors can create appointments for viewing.");
        }

        Listing listing = listingRepository.findById(requestDto.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", requestDto.getListingId()));

        if (listing.getStatus() != ListingStatus.APPROVED) {
            throw new BadRequestException("Cannot create appointment for a listing that is not approved.");
        }

        if (listing.getRealtor().getUserId().equals(customer.getUserId())) {
            throw new BadRequestException("Realtor cannot create an appointment for their own listing as a customer.");
        }

        // Kiểm tra xem customer đã có lịch hẹn active cho listing này chưa (tránh spam)
        // Ví dụ: kiểm tra trong vòng 24h tới hoặc xem có lịch PENDING/CONFIRMED không
        Timestamp now = Timestamp.from(Instant.now());
        Timestamp twentyFourHoursLater = Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS));
        if (appointmentRepository.existsActiveAppointmentForListingByCustomerInTimeRange(
                listing.getListingId(), customer.getUserId(), now, twentyFourHoursLater)) {
            // Bạn có thể điều chỉnh logic này, ví dụ chỉ check PENDING/CONFIRMED mà không giới hạn thời gian
            // Hoặc cho phép đặt nhiều lịch nếu các lịch cũ đã COMPLETED/CANCELLED
            // Hiện tại là check xem có lịch nào (trừ CANCELLED) trong 24h tới không
            Page<Appointment> existingAppointments = appointmentRepository.findByCustomerAndStatusOrderByScheduledAtDesc(customer, AppointmentStatus.PENDING, Pageable.unpaged());
            if(existingAppointments.hasContent() && existingAppointments.getContent().stream().anyMatch(a -> a.getListing().getListingId().equals(listing.getListingId()))) {
                throw new BadRequestException("You already have a PENDING appointment for this listing.");
            }
            existingAppointments = appointmentRepository.findByCustomerAndStatusOrderByScheduledAtDesc(customer, AppointmentStatus.CONFIRMED, Pageable.unpaged());
            if(existingAppointments.hasContent() && existingAppointments.getContent().stream().anyMatch(a -> a.getListing().getListingId().equals(listing.getListingId()))) {
                throw new BadRequestException("You already have a CONFIRMED appointment for this listing.");
            }
        }


        Appointment appointment = new Appointment(
                listing,
                customer,
                listing.getRealtor(), // Realtor của listing
                requestDto.getScheduledAt(),
                requestDto.getNotesCustomer()
        );
        // status mặc định là PENDING

        Appointment savedAppointment = appointmentRepository.save(appointment);
        logger.info("Appointment created with ID: {} by Customer: {} for Listing: {}", savedAppointment.getAppointmentId(), customer.getUserId(), listing.getListingId());
        // TODO: Gửi thông báo cho Realtor về lịch hẹn mới

        return mapToAppointmentResponseDto(savedAppointment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponseDto> getMyAppointments(String userPhone, String roleContext, Pageable pageable) {
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("User", "phone", userPhone));

        Page<Appointment> appointmentsPage;
        if ("customer".equalsIgnoreCase(roleContext)) {
            appointmentsPage = appointmentRepository.findByCustomerOrderByScheduledAtDesc(user, pageable);
        } else if ("realtor".equalsIgnoreCase(roleContext)) {
            appointmentsPage = appointmentRepository.findByRealtorOrderByScheduledAtDesc(user, pageable);
        } else {
            throw new BadRequestException("Invalid roleContext. Must be 'customer' or 'realtor'.");
        }

        return appointmentsPage.map(this::mapToAppointmentResponseDto);
    }

    @Override
    @Transactional
    public AppointmentResponseDto confirmAppointment(Long appointmentId, String realtorPhone) {
        User realtor = userRepository.findByPhone(realtorPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Realtor", "phone", realtorPhone));
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (!appointment.getRealtor().getUserId().equals(realtor.getUserId())) {
            throw new AccessDeniedException("You are not authorized to confirm this appointment.");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Only PENDING appointments can be confirmed. Current status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        logger.info("Appointment ID: {} confirmed by Realtor: {}", savedAppointment.getAppointmentId(), realtor.getUserId());
        // TODO: Gửi thông báo cho Customer về việc lịch hẹn đã được xác nhận

        return mapToAppointmentResponseDto(savedAppointment);
    }

    @Override
    @Transactional
    public AppointmentResponseDto cancelAppointment(Long appointmentId, String userPhone, String cancelReason) {
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("User", "phone", userPhone));
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        AppointmentStatus newStatus;
        boolean canCancel = false;

        if (appointment.getCustomer().getUserId().equals(user.getUserId())) { // Customer hủy
            if (appointment.getStatus() == AppointmentStatus.PENDING || appointment.getStatus() == AppointmentStatus.CONFIRMED) {
                newStatus = AppointmentStatus.CANCELLED_BY_CUSTOMER;
                canCancel = true;
                logger.info("Appointment ID: {} cancelled by Customer: {}. Reason: {}", appointmentId, user.getUserId(), cancelReason);
                // TODO: Gửi thông báo cho Realtor
            } else {
                throw new BadRequestException("Customer can only cancel PENDING or CONFIRMED appointments. Current status: " + appointment.getStatus());
            }
        } else if (appointment.getRealtor().getUserId().equals(user.getUserId())) { // Realtor hủy
            if (appointment.getStatus() == AppointmentStatus.PENDING || appointment.getStatus() == AppointmentStatus.CONFIRMED) {
                newStatus = AppointmentStatus.CANCELLED_BY_REALTOR;
                canCancel = true;
                logger.info("Appointment ID: {} cancelled by Realtor: {}. Reason: {}", appointmentId, user.getUserId(), cancelReason);
                // TODO: Gửi thông báo cho Customer
            } else {
                throw new BadRequestException("Realtor can only cancel PENDING or CONFIRMED appointments. Current status: " + appointment.getStatus());
            }
        } else {
            throw new AccessDeniedException("You are not authorized to cancel this appointment.");
        }

        if (canCancel) {
            appointment.setStatus(newStatus);
            if (user.getRole() == RoleType.REALTOR && cancelReason != null) {
                appointment.setNotesRealtor((appointment.getNotesRealtor() == null ? "" : appointment.getNotesRealtor() + "\n") + "Cancellation reason: " + cancelReason);
            } else if (cancelReason != null) {
                appointment.setNotesCustomer((appointment.getNotesCustomer() == null ? "" : appointment.getNotesCustomer() + "\n") + "Cancellation reason: " + cancelReason);
            }
            Appointment savedAppointment = appointmentRepository.save(appointment);
            return mapToAppointmentResponseDto(savedAppointment);
        }
        // Trường hợp này không nên xảy ra nếu logic trên đúng
        throw new BadRequestException("Appointment cannot be cancelled in its current state: " + appointment.getStatus());
    }

    @Override
    @Transactional
    public AppointmentResponseDto completeAppointment(Long appointmentId, String realtorPhone) {
        User realtor = userRepository.findByPhone(realtorPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Realtor", "phone", realtorPhone));
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", appointmentId));

        if (!appointment.getRealtor().getUserId().equals(realtor.getUserId())) {
            throw new AccessDeniedException("You are not authorized to complete this appointment.");
        }
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Only CONFIRMED appointments can be completed. Current status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        logger.info("Appointment ID: {} marked as COMPLETED by Realtor: {}", savedAppointment.getAppointmentId(), realtor.getUserId());
        // TODO: Gửi thông báo cho Customer (nếu cần)

        return mapToAppointmentResponseDto(savedAppointment);
    }

    // Helper method để map Appointment sang AppointmentResponseDto
    private AppointmentResponseDto mapToAppointmentResponseDto(Appointment appointment) {
        AppointmentResponseDto dto = modelMapper.map(appointment, AppointmentResponseDto.class);
        // Map ListingSummary tóm tắt
        if (appointment.getListing() != null) {
            ListingSummaryResponseDto listingSummary = modelMapper.map(appointment.getListing(), ListingSummaryResponseDto.class);
            // Custom map cho price, area, bedrooms, primaryImageUrl cho listingSummary nếu cần
            Listing listing = appointment.getListing();
            listingSummary.setPrice(listing.getListingType() == ListingType.SALE ? listing.getSalePrice() : listing.getRentalPrice());
            if (listing.getFeatures() != null) {
                listingSummary.setArea(listing.getFeatures().getArea());
                listingSummary.setBedrooms(listing.getFeatures().getBedrooms());
            }
            if (listing.getImages() != null && !listing.getImages().isEmpty()) {
                listing.getImages().stream().filter(img -> img != null && img.isPrimary()).findFirst()
                        .ifPresent(img -> listingSummary.setPrimaryImageUrl(img.getImageUrl()));
                if(listingSummary.getPrimaryImageUrl() == null) {
                    listing.getImages().stream().filter(img -> img != null).findFirst()
                            .ifPresent(img -> listingSummary.setPrimaryImageUrl(img.getImageUrl()));
                }
            }
            dto.setListingSummary(listingSummary);
        }
        // Map UserSummary cho customer và realtor
        if (appointment.getCustomer() != null) {
            dto.setCustomer(modelMapper.map(appointment.getCustomer(), UserSummaryResponseDto.class));
        }
        if (appointment.getRealtor() != null) {
            dto.setRealtor(modelMapper.map(appointment.getRealtor(), UserSummaryResponseDto.class));
        }
        return dto;
    }
}