package com.example.proptech.repository;

import com.example.proptech.entity.Listing;
import com.example.proptech.entity.User;
import com.example.proptech.enums.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Cho tìm kiếm động
import org.springframework.stereotype.Repository;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    // Tìm tin đăng của một realtor cụ thể
    Page<Listing> findByRealtor(User realtor, Pageable pageable);
    Page<Listing> findByRealtorAndStatus(User realtor, ListingStatus status, Pageable pageable);

    // Tìm các tin đăng theo trạng thái (ví dụ: cho admin duyệt)
    Page<Listing> findByStatus(ListingStatus status, Pageable pageable);

    // JpaSpecificationExecutor sẽ được dùng để xây dựng các query tìm kiếm động
    // ví dụ: tìm theo thành phố, quận, loại hình, giá, diện tích,...
}