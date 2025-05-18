package com.example.proptech.repository;

import com.example.proptech.entity.Appointment;
import com.example.proptech.entity.User;
import com.example.proptech.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Lấy lịch hẹn của customer
    Page<Appointment> findByCustomerOrderByScheduledAtDesc(User customer, Pageable pageable);
    Page<Appointment> findByCustomerAndStatusOrderByScheduledAtDesc(User customer, AppointmentStatus status, Pageable pageable);

    // Lấy lịch hẹn của realtor (các lịch người khác đặt cho tin của realtor)
    Page<Appointment> findByRealtorOrderByScheduledAtDesc(User realtor, Pageable pageable);
    Page<Appointment> findByRealtorAndStatusOrderByScheduledAtDesc(User realtor, AppointmentStatus status, Pageable pageable);

    // Kiểm tra xem customer đã đặt lịch cho listing này trong khoảng thời gian gần đó chưa (tránh spam)
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Appointment a WHERE a.listing.listingId = :listingId " +
            "AND a.customer.userId = :customerId " +
            "AND a.status <> com.example.proptech.enums.AppointmentStatus.CANCELLED_BY_CUSTOMER " +
            "AND a.status <> com.example.proptech.enums.AppointmentStatus.CANCELLED_BY_REALTOR " +
            "AND a.scheduledAt BETWEEN :startTime AND :endTime")
    boolean existsActiveAppointmentForListingByCustomerInTimeRange(
            @Param("listingId") Long listingId,
            @Param("customerId") Long customerId,
            @Param("startTime") Timestamp startTime,
            @Param("endTime") Timestamp endTime);
}