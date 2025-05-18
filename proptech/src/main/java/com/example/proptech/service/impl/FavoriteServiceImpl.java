package com.example.proptech.service.impl;

import com.example.proptech.dto.response.AddressResponseDto;
import com.example.proptech.dto.response.ListingSummaryResponseDto;
import com.example.proptech.dto.response.PropertyTypeResponseDto;
import com.example.proptech.dto.response.UserSummaryResponseDto;
import com.example.proptech.entity.Favorite;
import com.example.proptech.entity.Listing;
import com.example.proptech.entity.ListingImage;
import com.example.proptech.entity.User;
import com.example.proptech.enums.ListingType;
import com.example.proptech.exception.BadRequestException;
import com.example.proptech.exception.ResourceNotFoundException;
import com.example.proptech.repository.FavoriteRepository;
import com.example.proptech.repository.ListingRepository;
import com.example.proptech.repository.UserRepository;
import com.example.proptech.service.FavoriteService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final Logger logger = LoggerFactory.getLogger(FavoriteServiceImpl.class);

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public FavoriteServiceImpl(FavoriteRepository favoriteRepository,
                               UserRepository userRepository,
                               ListingRepository listingRepository,
                               ModelMapper modelMapper) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public void addFavorite(Long listingId, String userPhone) {
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("User", "phone", userPhone));
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (favoriteRepository.existsByUserAndListing_ListingId(user, listingId)) {
            throw new BadRequestException("Listing is already in favorites.");
        }

        Favorite favorite = new Favorite(user, listing);
        favoriteRepository.save(favorite);
        logger.info("User {} favorited Listing {}", user.getUserId(), listing.getListingId());
    }

    @Override
    @Transactional
    public void removeFavorite(Long listingId, String userPhone) {
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("User", "phone", userPhone));
        // Lấy đối tượng Listing từ database để sử dụng cho logger (và có thể cho các logic khác nếu cần)
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));
        // Kiểm tra xem listing có tồn tại không (tùy chọn, vì deleteByUserAndListing_ListingId sẽ không báo lỗi nếu không tìm thấy)
        if (!listingRepository.existsById(listingId)) {
            throw new ResourceNotFoundException("Listing", "id", listingId);
        }

        if (!favoriteRepository.existsByUserAndListing_ListingId(user, listingId)) {
            throw new BadRequestException("Listing is not in favorites to remove.");
        }

        favoriteRepository.deleteByUserAndListing_ListingId(user, listingId);
        logger.info("User {} unfavorited Listing {}", user.getUserId(), listing.getListingId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingSummaryResponseDto> getMyFavorites(String userPhone, Pageable pageable) {
        User user = userRepository.findByPhone(userPhone)
                .orElseThrow(() -> new ResourceNotFoundException("User", "phone", userPhone));

        Page<Favorite> favoritesPage = favoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        return favoritesPage.map(favorite -> {
            Listing listing = favorite.getListing();
            ListingSummaryResponseDto dto = modelMapper.map(listing, ListingSummaryResponseDto.class);
            // Map các trường còn lại cho summary (price, area, bedrooms, primaryImage, realtor)
            dto.setPrice(listing.getListingType() == ListingType.SALE ? listing.getSalePrice() : listing.getRentalPrice());
            if (listing.getFeatures() != null) {
                dto.setArea(listing.getFeatures().getArea());
                dto.setBedrooms(listing.getFeatures().getBedrooms());
            }
            if (listing.getImages() != null && !listing.getImages().isEmpty()) {
                listing.getImages().stream().filter(img -> img != null && img.isPrimary()).findFirst()
                        .ifPresent(img -> dto.setPrimaryImageUrl(img.getImageUrl()));
                if(dto.getPrimaryImageUrl() == null) { // Nếu không có ảnh primary, lấy ảnh đầu tiên
                    listing.getImages().stream().filter(img -> img != null).findFirst()
                            .ifPresent(img -> dto.setPrimaryImageUrl(img.getImageUrl()));
                }
            }
            if (listing.getAddress() != null) { // Map address DTO
                dto.setAddress(modelMapper.map(listing.getAddress(), AddressResponseDto.class));
            }
            if (listing.getPropertyType() != null) { // Map property type DTO
                dto.setPropertyType(modelMapper.map(listing.getPropertyType(), PropertyTypeResponseDto.class));
            }
            if (listing.getRealtor() != null) { // Map realtor DTO
                dto.setRealtor(modelMapper.map(listing.getRealtor(), UserSummaryResponseDto.class));
            }
            dto.setFavorite(true); // Vì đây là danh sách yêu thích
            return dto;
        });
    }

    @Override
    public boolean isListingFavoritedByUser(Long listingId, Long userId) {
        User user = userRepository.findById(userId)
                .orElse(null); // Hoặc orElseThrow nếu muốn chặt chẽ
        if (user == null) return false;
        return favoriteRepository.existsByUserAndListing_ListingId(user, listingId);
    }
}