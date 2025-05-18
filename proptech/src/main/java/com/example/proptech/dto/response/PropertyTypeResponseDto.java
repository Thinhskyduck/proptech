package com.example.proptech.dto.response;

import com.example.proptech.enums.PropertyCategory;
import lombok.Data;

@Data
public class PropertyTypeResponseDto {
    private Long typeId;
    private String name;
    private PropertyCategory category;
}