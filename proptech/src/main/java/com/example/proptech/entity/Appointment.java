package com.example.proptech.entity;

import com.example.proptech.enums.AppointmentStatus; // Sẽ tạo enum này
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
// Bỏ import Lombok nếu không dùng
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    private Long appointmentId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false) // Người đặt lịch (ROLE_CUSTOMER)
    private User customer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realtor_id", nullable = false) // Realtor của tin đăng đó
    private User realtor;

    @NotNull(message = "Scheduled time is required")
    @Future(message = "Scheduled time must be in the future")
    @Column(name = "scheduled_at", nullable = false)
    private Timestamp scheduledAt; // Thời gian hẹn

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING; // Mặc định là PENDING

    @Column(name = "notes_customer", columnDefinition = "TEXT")
    private String notesCustomer; // Ghi chú từ customer

    @Column(name = "notes_realtor", columnDefinition = "TEXT")
    private String notesRealtor; // Ghi chú từ realtor

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;


    public Appointment(Listing listing, User customer, User realtor, Timestamp scheduledAt, String notesCustomer) {
        this.listing = listing;
        this.customer = customer;
        this.realtor = realtor;
        this.scheduledAt = scheduledAt;
        this.notesCustomer = notesCustomer;
        this.status = AppointmentStatus.PENDING;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Appointment that = (Appointment) o;
        return Objects.equals(appointmentId, that.appointmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appointmentId);
    }
}