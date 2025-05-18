package com.example.proptech.repository;

import com.example.proptech.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    // Có thể thêm các phương thức tìm kiếm tùy chỉnh nếu cần
}