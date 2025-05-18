package com.example.proptech.config;

import com.example.proptech.dto.response.UserSummaryResponseDto; // << THÊM DÒNG NÀY

import com.example.proptech.dto.response.ListingSummaryResponseDto;
import com.example.proptech.entity.Listing;
import com.example.proptech.enums.ListingType;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.modelmapper.Converter; // Thêm import này
import org.modelmapper.spi.MappingContext; // Thêm import này

@Configuration
public class ModelMapperConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        // Chiến lược khớp chặt chẽ hơn (tùy chọn, nhưng tốt cho việc tránh lỗi ngầm)
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

        // Cấu hình mapping cụ thể cho Listing -> ListingSummaryResponseDto cho trường 'price'
        modelMapper.typeMap(Listing.class, ListingSummaryResponseDto.class)
                .addMappings(mapper -> {
                    // Bỏ qua việc map tự động cho trường 'price' trước
                    mapper.skip(ListingSummaryResponseDto::setPrice);
                    // Bỏ qua việc map tự động cho trường 'area' và 'bedrooms' vì chúng sẽ được lấy từ features
                    mapper.skip(ListingSummaryResponseDto::setArea);
                    mapper.skip(ListingSummaryResponseDto::setBedrooms);
                    // Bỏ qua việc map tự động cho trường 'primaryImageUrl'
                    mapper.skip(ListingSummaryResponseDto::setPrimaryImageUrl);
                    // Bỏ qua việc map tự động cho trường 'realtor' vì ta sẽ map UserSummaryResponseDto
                    mapper.skip(ListingSummaryResponseDto::setRealtor);
                })
                .setPostConverter(context -> { // Sử dụng PostConverter để set giá trị sau khi các mapping khác đã hoàn tất
                    Listing source = context.getSource();
                    ListingSummaryResponseDto destination = context.getDestination();

                    // Logic để set price
                    if (source.getListingType() == ListingType.SALE) {
                        destination.setPrice(source.getSalePrice());
                    } else if (source.getListingType() == ListingType.RENTAL) {
                        destination.setPrice(source.getRentalPrice());
                    }

                    // Logic để set area và bedrooms từ features
                    if (source.getFeatures() != null) {
                        destination.setArea(source.getFeatures().getArea());
                        destination.setBedrooms(source.getFeatures().getBedrooms());
                    }

                    // Logic để set primaryImageUrl
                    if (source.getImages() != null && !source.getImages().isEmpty()) {
                        source.getImages().stream()
                                .filter(img -> img != null && img.isPrimary()) // Thêm check null cho img
                                .findFirst()
                                .ifPresent(img -> destination.setPrimaryImageUrl(img.getImageUrl()));
                        // Nếu không có ảnh primary, lấy ảnh đầu tiên
                        if (destination.getPrimaryImageUrl() == null) {
                            source.getImages().stream()
                                    .filter(img -> img != null) // Thêm check null
                                    .findFirst()
                                    .ifPresent(img -> destination.setPrimaryImageUrl(img.getImageUrl()));
                        }
                    }

                    // Logic để map realtor
                    if (source.getRealtor() != null) {
                        destination.setRealtor(modelMapper.map(source.getRealtor(), UserSummaryResponseDto.class));
                    }


                    return destination;
                });

        return modelMapper;
    }
}