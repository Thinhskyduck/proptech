package com.example.proptech.dto.response;

import lombok.Data;

@Data
public class AddressResponseDto {
    private Long addressId;
    private String streetAddress;
    private String ward;
    private String district;
    private String city;
}