package com.example.proptech.entity;

import com.example.proptech.enums.PropertyCategory; // Enum bạn đã tạo hoặc sẽ tạo
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
// Bỏ import Lombok nếu bạn không dùng, hoặc thêm lại nếu dùng
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;

import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // Thêm constructor với tất cả các field (tùy chọn)
@Entity
@Table(name = "property_types", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name") // Đảm bảo tên loại hình là duy nhất
})
public class PropertyType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long typeId;

    @NotBlank(message = "Property type name is required")
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String name; // Ví dụ: "Căn hộ chung cư", "Nhà phố", "Biệt thự", "Đất nền", "Văn phòng"

    @NotNull(message = "Property category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) // RESIDENTIAL, COMMERCIAL
    private PropertyCategory category;

    // Một PropertyType có thể được sử dụng bởi nhiều Listings (One-to-Many)
    // Tuy nhiên, thường chúng ta không cần duyệt từ PropertyType sang Listings
    // nên có thể không cần List<Listing> ở đây để giữ entity đơn giản.
    // @OneToMany(mappedBy = "propertyType", fetch = FetchType.LAZY)
    // private List<Listing> listings = new ArrayList<>();


    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyType that = (PropertyType) o;
        return Objects.equals(typeId, that.typeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeId);
    }

    // toString
    @Override
    public String toString() {
        return "PropertyType{" +
                "typeId=" + typeId +
                ", name='" + name + '\'' +
                ", category=" + category +
                '}';
    }
}