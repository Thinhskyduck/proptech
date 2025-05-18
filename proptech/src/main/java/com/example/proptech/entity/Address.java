package com.example.proptech.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
// Bỏ import Lombok nếu bạn không dùng, hoặc thêm lại nếu dùng
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor; // Thêm AllArgsConstructor nếu bạn muốn

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // Thêm constructor với tất cả các field (tùy chọn)
@Entity
@Table(name = "addresses", indexes = {
        @Index(name = "idx_addresses_location", columnList = "city, district, ward") // Index cho tìm kiếm
})
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;

    @NotBlank(message = "Street address is required")
    @Size(max = 255)
    @Column(name = "street_address", nullable = false)
    private String streetAddress; // Số nhà, tên đường cụ thể

    @NotBlank(message = "Ward is required")
    @Size(max = 100)
    @Column(nullable = false)
    private String ward; // Phường/Xã

    @NotBlank(message = "District is required")
    @Size(max = 100)
    @Column(nullable = false)
    private String district; // Quận/Huyện

    @NotBlank(message = "City is required")
    @Size(max = 100)
    @Column(nullable = false)
    private String city; // Tỉnh/Thành phố

    // Một Address có thể được sử dụng bởi nhiều Listings (One-to-Many)
    // Tuy nhiên, trong thiết kế hiện tại, một Listing có một Address (Many-to-One từ Listing)
    // Nếu bạn muốn một Address có thể được tham chiếu bởi nhiều Listing (ví dụ, tòa nhà chung cư có nhiều căn hộ đăng bán)
    // thì quan hệ sẽ là OneToMany từ Address đến Listing, và Listing sẽ có @ManyToOne đến Address.
    // Thiết kế hiện tại (Listing @ManyToOne Address) là phổ biến và đơn giản hơn cho trường hợp mỗi tin đăng có địa chỉ riêng.
    // Nếu bạn muốn chia sẻ Address, cấu trúc sẽ phức tạp hơn.
    // Tạm thời không cần List<Listing> ở đây nếu mỗi Listing có Address riêng.
    // @OneToMany(mappedBy = "address", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Listing> listings = new ArrayList<>();


    // equals and hashCode (Quan trọng cho JPA entities)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(addressId, address.addressId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressId);
    }

    // toString (Tùy chọn, hữu ích cho debugging)
    @Override
    public String toString() {
        return "Address{" +
                "addressId=" + addressId +
                ", streetAddress='" + streetAddress + '\'' +
                ", ward='" + ward + '\'' +
                ", district='" + district + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}