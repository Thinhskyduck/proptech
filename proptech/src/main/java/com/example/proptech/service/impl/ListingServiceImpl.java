package com.example.proptech.service.impl;

import java.time.temporal.ChronoUnit;
import com.example.proptech.dto.request.ListingCreateRequestDto;
import com.example.proptech.dto.request.ListingUpdateRequestDto;
import com.example.proptech.dto.response.*;
import com.example.proptech.entity.*;
import com.example.proptech.enums.ListingStatus;
import com.example.proptech.enums.ListingType; // Đảm bảo import đúng
import com.example.proptech.enums.RoleType;
import com.example.proptech.enums.TransactionType; // Cho UserTransaction
import com.example.proptech.exception.BadRequestException;
import com.example.proptech.exception.InsufficientBalanceException;
import com.example.proptech.exception.ResourceNotFoundException;
import com.example.proptech.repository.*;
import com.example.proptech.service.FileStorageService;
import com.example.proptech.service.ListingService;
import com.example.proptech.service.WalletService; // Sẽ cần WalletService để trừ tiền
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification; // Cho tìm kiếm động
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate; // Cho Specification
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ListingServiceImpl implements ListingService {

    private static final Logger logger = LoggerFactory.getLogger(ListingServiceImpl.class);

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final AddressRepository addressRepository;
    private final PropertyTypeRepository propertyTypeRepository;
    private final PropertyFeatureRepository propertyFeatureRepository; // Không cần inject nếu PropertyFeature được quản lý qua Listing
    private final ListingImageRepository listingImageRepository;
    private final FileStorageService fileStorageService;
    private final WalletService walletService; // Inject WalletService
    private final ModelMapper modelMapper; // Đảm bảo đã có bean ModelMapperConfig

    // Phí đăng tin cố định (ví dụ)
    private static final BigDecimal DEFAULT_POSTING_FEE = new BigDecimal("50000.00"); // 50k VNĐ

    @Autowired
    private UserTransactionRepository userTransactionRepository;

    @Autowired
    public ListingServiceImpl(UserRepository userRepository,
                              ListingRepository listingRepository,
                              AddressRepository addressRepository,
                              PropertyTypeRepository propertyTypeRepository,
                              PropertyFeatureRepository propertyFeatureRepository,
                              ListingImageRepository listingImageRepository,
                              FileStorageService fileStorageService,
                              WalletService walletService, // Thêm WalletService vào constructor
                              ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.addressRepository = addressRepository;
        this.propertyTypeRepository = propertyTypeRepository;
        this.propertyFeatureRepository = propertyFeatureRepository;
        this.listingImageRepository = listingImageRepository;
        this.fileStorageService = fileStorageService;
        this.walletService = walletService;
        this.modelMapper = modelMapper;
    }


    @Override
    @Transactional
    public ListingDetailResponseDto createListing(ListingCreateRequestDto createRequest, String realtorPhone) {
        User realtor = userRepository.findByPhone(realtorPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Realtor not found with phone: " + realtorPhone));

        if (realtor.getRole() != RoleType.REALTOR) {
            throw new AccessDeniedException("Only realtors can create listings.");
        }

        BigDecimal postingFee = DEFAULT_POSTING_FEE;

        // 1. Chỉ trừ tiền từ ví Realtor
        try {
            walletService.deductUserBalance(realtor.getUserId(), postingFee);
        } catch (InsufficientBalanceException e) {
            throw e;
        }

        // 2. Tạo và lưu Address
        Address address = modelMapper.map(createRequest.getAddress(), Address.class);

        // 3. Lấy PropertyType
        PropertyType propertyType = propertyTypeRepository.findById(createRequest.getPropertyTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("PropertyType not found with id: " + createRequest.getPropertyTypeId()));

        // 4. Tạo Listing
        Listing listing = new Listing();
        // ... (set các thuộc tính cho listing như cũ) ...
        listing.setRealtor(realtor);
        listing.setPostingFeeAmount(postingFee);
        listing.setPostingFeePaid(true);
        listing.setStatus(ListingStatus.PAID_PENDING_APPROVAL);

        PropertyFeature features = modelMapper.map(createRequest.getFeatures(), PropertyFeature.class);
        listing.setFeatures(features);

        Listing savedListing = listingRepository.save(listing); // Lưu listing để có ID

        // 5. Tạo UserTransaction SAU KHI có listingId
        UserTransaction transaction = new UserTransaction();
        transaction.setUser(realtor);
        transaction.setTransactionType(TransactionType.POSTING_FEE);
        transaction.setAmount(postingFee.negate()); // Số tiền trừ
        transaction.setDescription("Posting fee for listing: " + savedListing.getTitle() + " (ID: " + savedListing.getListingId() + ")");
        transaction.setRelatedListingId(savedListing.getListingId()); // GÁN LISTING ID Ở ĐÂY
        userTransactionRepository.save(transaction);

        return modelMapper.map(savedListing, ListingDetailResponseDto.class);
    }


    @Override
    @Transactional
    public ListingDetailResponseDto updateListing(Long listingId, ListingUpdateRequestDto updateRequest, String realtorPhone) {
        User realtor = userRepository.findByPhone(realtorPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Realtor not found"));
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (!listing.getRealtor().getUserId().equals(realtor.getUserId())) {
            throw new AccessDeniedException("You are not authorized to update this listing.");
        }
        if (listing.getStatus() == ListingStatus.SOLD || listing.getStatus() == ListingStatus.RENTED || listing.getStatus() == ListingStatus.EXPIRED) {
            throw new BadRequestException("Cannot update a listing that is already sold, rented, or expired.");
        }


        // Update các trường cơ bản
        if (StringUtils.hasText(updateRequest.getTitle())) listing.setTitle(updateRequest.getTitle());
        if (updateRequest.getDescription() != null) listing.setDescription(updateRequest.getDescription()); // Cho phép description là empty string

        // Update Address
        if (updateRequest.getAddress() != null) {
            Address addressToUpdate = listing.getAddress(); // Lấy address hiện tại
            modelMapper.map(updateRequest.getAddress(), addressToUpdate); // Map các trường mới vào address hiện tại
            // addressRepository.save(addressToUpdate); // Không cần nếu Address là @ManyToOne và được quản lý bởi Listing
        }

        // Update PropertyType
        if (updateRequest.getPropertyTypeId() != null) {
            PropertyType propertyType = propertyTypeRepository.findById(updateRequest.getPropertyTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("PropertyType", "id", updateRequest.getPropertyTypeId()));
            listing.setPropertyType(propertyType);
        }

        if (updateRequest.getListingType() != null) listing.setListingType(updateRequest.getListingType());

        // Update giá (cẩn thận với việc set null)
        if (listing.getListingType() == ListingType.SALE) {
            if(updateRequest.getSalePrice() != null) listing.setSalePrice(updateRequest.getSalePrice());
            listing.setRentalPrice(null); // Đảm bảo rental price là null cho tin bán
        } else if (listing.getListingType() == ListingType.RENTAL) {
            if(updateRequest.getRentalPrice() != null) listing.setRentalPrice(updateRequest.getRentalPrice());
            listing.setSalePrice(null); // Đảm bảo sale price là null cho tin thuê
        }


        // Update Features
        if (updateRequest.getFeatures() != null) {
            PropertyFeature featuresToUpdate = listing.getFeatures();
            if (featuresToUpdate == null) { // Nếu listing chưa có features (trường hợp hiếm)
                featuresToUpdate = new PropertyFeature();
                listing.setFeatures(featuresToUpdate);
            }
            modelMapper.map(updateRequest.getFeatures(), featuresToUpdate);
        }

        // Sau khi cập nhật, có thể cần chuyển status về PENDING_APPROVAL nếu admin yêu cầu duyệt lại sau khi sửa
        // listing.setStatus(ListingStatus.PENDING_APPROVAL); // Hoặc PAID_PENDING_APPROVAL

        Listing updatedListing = listingRepository.save(listing);
        return modelMapper.map(updatedListing, ListingDetailResponseDto.class);
    }

    @Override
    @Transactional
    public void deleteListing(Long listingId, String realtorPhone) {
        User realtor = userRepository.findByPhone(realtorPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Realtor not found"));
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (!listing.getRealtor().getUserId().equals(realtor.getUserId())) {
            throw new AccessDeniedException("You are not authorized to delete this listing.");
        }
        // TODO: Cân nhắc xem có nên hoàn tiền đăng bài không, hoặc các quy tắc khác trước khi xóa.
        // Hiện tại chỉ xóa.

        // Xóa các ảnh liên quan trên file system trước
        listing.getImages().forEach(image -> {
            // Giả sử imageUrl lưu dạng "subFolder/filename.ext"
            String[] pathParts = image.getImageUrl().split("/");
            if (pathParts.length >= 2) {
                String subFolder = pathParts[0];
                String filename = pathParts[1];
                for(int i=2; i<pathParts.length; ++i) filename+="/"+pathParts[i]; // Nếu subFolder có nhiều cấp
                fileStorageService.delete(subFolder, filename);
            } else if (pathParts.length == 1 && pathParts[0].contains(".")) { // Không có subfolder, chỉ có filename
                fileStorageService.delete("", pathParts[0]); // Subfolder rỗng
            }
        });


        listingRepository.delete(listing);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ListingSummaryResponseDto> getMyListings(String realtorPhone, Pageable pageable) {
        User realtor = userRepository.findByPhone(realtorPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Realtor not found"));
        Page<Listing> listingsPage = listingRepository.findByRealtor(realtor, pageable);
        return listingsPage.map(listing -> {
            ListingSummaryResponseDto dto = modelMapper.map(listing, ListingSummaryResponseDto.class);
            // Map giá và các trường tóm tắt khác
            dto.setPrice(listing.getListingType() == ListingType.SALE ? listing.getSalePrice() : listing.getRentalPrice());
            if (listing.getFeatures() != null) {
                dto.setArea(listing.getFeatures().getArea());
                dto.setBedrooms(listing.getFeatures().getBedrooms());
            }
            if (listing.getImages() != null && !listing.getImages().isEmpty()) {
                listing.getImages().stream().filter(ListingImage::isPrimary).findFirst()
                        .ifPresent(img -> dto.setPrimaryImageUrl(img.getImageUrl()));
                if(dto.getPrimaryImageUrl() == null) { // Nếu không có ảnh primary, lấy ảnh đầu tiên
                    listing.getImages().stream().findFirst()
                            .ifPresent(img -> dto.setPrimaryImageUrl(img.getImageUrl()));
                }
            }
            dto.setRealtor(modelMapper.map(listing.getRealtor(), UserSummaryResponseDto.class));
            return dto;
        });
    }

    @Override
    @Transactional
    public List<ListingImageResponseDto> uploadListingImages(Long listingId, String realtorPhone, MultipartFile[] files) {
        User realtor = userRepository.findByPhone(realtorPhone)
                .orElseThrow(() -> new ResourceNotFoundException("Realtor not found"));
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (!listing.getRealtor().getUserId().equals(realtor.getUserId())) {
            throw new AccessDeniedException("You are not authorized to upload images for this listing.");
        }

        List<ListingImage> savedImages = new ArrayList<>();
        String subFolder = "listings/" + listingId; // Tạo thư mục con cho mỗi listing

        for (MultipartFile file : files) {
            String storedFilenamePath = fileStorageService.store(file, subFolder); // Trả về "subFolder/filename.ext"
            ListingImage listingImage = new ListingImage();
            listingImage.setListing(listing);
            listingImage.setImageUrl(storedFilenamePath); // Lưu đường dẫn tương đối
            // Set ảnh đầu tiên làm primary nếu chưa có ảnh primary nào
            if (listing.getImages().stream().noneMatch(ListingImage::isPrimary) && savedImages.isEmpty()) {
                listingImage.setPrimary(true);
            }
            savedImages.add(listingImageRepository.save(listingImage));
        }
        // Không cần gọi listingRepository.save(listing) nếu quan hệ được quản lý đúng
        return savedImages.stream()
                .map(img -> modelMapper.map(img, ListingImageResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteListingImage(Long listingId, Long imageId, String realtorPhone) {
        User realtor = userRepository.findByPhone(realtorPhone).orElseThrow(() -> new ResourceNotFoundException("Realtor not found"));
        Listing listing = listingRepository.findById(listingId).orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));
        if (!listing.getRealtor().getUserId().equals(realtor.getUserId())) {
            throw new AccessDeniedException("You are not authorized to delete images for this listing.");
        }

        ListingImage image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        if (!image.getListing().getListingId().equals(listingId)) {
            throw new BadRequestException("Image does not belong to the specified listing.");
        }

        String imageUrl = image.getImageUrl(); // "subFolder/filename.ext"
        String[] pathParts = imageUrl.split("/");
        if (pathParts.length >= 2) {
            String subFolder = pathParts[0];
            String filename = pathParts[1];
            for(int i=2; i<pathParts.length; ++i) filename+="/"+pathParts[i];
            fileStorageService.delete(subFolder, filename);
        } else if (pathParts.length == 1 && pathParts[0].contains(".")){
            fileStorageService.delete("", pathParts[0]);
        }


        boolean wasPrimary = image.isPrimary();
        listingImageRepository.delete(image);
        listing.getImages().remove(image); // Cập nhật danh sách trong entity Listing

        // Nếu ảnh bị xóa là ảnh primary và vẫn còn ảnh khác, chọn ảnh đầu tiên làm primary mới
        if (wasPrimary && !listing.getImages().isEmpty()) {
            ListingImage newPrimary = listing.getImages().get(0);
            newPrimary.setPrimary(true);
            listingImageRepository.save(newPrimary);
        }
    }

    @Override
    @Transactional
    public void setPrimaryImage(Long listingId, Long imageId, String realtorPhone) {
        User realtor = userRepository.findByPhone(realtorPhone).orElseThrow(() -> new ResourceNotFoundException("Realtor not found"));
        Listing listing = listingRepository.findById(listingId).orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));
        if (!listing.getRealtor().getUserId().equals(realtor.getUserId())) {
            throw new AccessDeniedException("You are not authorized to modify this listing.");
        }
        ListingImage newPrimaryImage = listing.getImages().stream()
                .filter(img -> img.getImageId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId + " for this listing."));

        listing.getImages().forEach(img -> {
            img.setPrimary(img.getImageId().equals(imageId));
            listingImageRepository.save(img); // Lưu thay đổi của từng ảnh
        });
    }


    // --- Public Methods ---
    @Override
    @Transactional(readOnly = true)
    public Page<ListingSummaryResponseDto> getPublicListings(Pageable pageable, String city, String district, Long propertyTypeId, String listingTypeStr) {
        Specification<Listing> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), ListingStatus.APPROVED)); // Chỉ lấy tin APPROVED

            if (StringUtils.hasText(city)) {
                predicates.add(criteriaBuilder.equal(root.get("address").get("city"), city));
            }
            if (StringUtils.hasText(district)) {
                predicates.add(criteriaBuilder.equal(root.get("address").get("district"), district));
            }
            if (propertyTypeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("propertyType").get("typeId"), propertyTypeId));
            }
            if (StringUtils.hasText(listingTypeStr)) {
                try {
                    ListingType listingType = ListingType.valueOf(listingTypeStr.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("listingType"), listingType));
                } catch (IllegalArgumentException e) {
                    // Bỏ qua nếu listingType không hợp lệ hoặc log lỗi
                    logger.warn("Invalid listingType provided for search: " + listingTypeStr);
                }
            }
            // Thêm các điều kiện lọc khác: giá, diện tích, số phòng ngủ (từ features)
            // Ví dụ: root.get("features").get("bedrooms")

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Listing> listingsPage = listingRepository.findAll(spec, pageable);
        return listingsPage.map(listing -> {
            ListingSummaryResponseDto dto = modelMapper.map(listing, ListingSummaryResponseDto.class);
            dto.setAddress(modelMapper.map(listing.getAddress(), AddressResponseDto.class));
            dto.setPropertyType(modelMapper.map(listing.getPropertyType(), PropertyTypeResponseDto.class));
            dto.setPrice(listing.getListingType() == ListingType.SALE ? listing.getSalePrice() : listing.getRentalPrice());
            if (listing.getFeatures() != null) {
                dto.setArea(listing.getFeatures().getArea());
                dto.setBedrooms(listing.getFeatures().getBedrooms());
            }
            if (listing.getImages() != null && !listing.getImages().isEmpty()) {
                listing.getImages().stream().filter(ListingImage::isPrimary).findFirst()
                        .ifPresent(img -> dto.setPrimaryImageUrl(img.getImageUrl()));
                if(dto.getPrimaryImageUrl() == null) {
                    listing.getImages().stream().findFirst()
                            .ifPresent(img -> dto.setPrimaryImageUrl(img.getImageUrl()));
                }
            }
            dto.setRealtor(modelMapper.map(listing.getRealtor(), UserSummaryResponseDto.class));
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ListingDetailResponseDto getPublicListingDetails(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (listing.getStatus() != ListingStatus.APPROVED) {
            // Hoặc throw ResourceNotFoundException nếu không muốn lộ thông tin tin chưa duyệt
            throw new AccessDeniedException("This listing is not currently available for public view.");
        }
        // TODO: Tăng view_count nếu cần
        return modelMapper.map(listing, ListingDetailResponseDto.class);
    }


    // --- Admin Methods ---
    @Override
    @Transactional(readOnly = true)
    public Page<ListingSummaryResponseDto> getListingsByStatus(String statusStr, Pageable pageable) {
        ListingStatus status;
        try {
            status = ListingStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid listing status provided: " + statusStr);
        }

        Page<Listing> listingsPage = listingRepository.findByStatus(status, pageable);
        return listingsPage.map(listing -> { // Copy logic map từ getMyListings hoặc getPublicListings
            ListingSummaryResponseDto dto = modelMapper.map(listing, ListingSummaryResponseDto.class);
            dto.setAddress(modelMapper.map(listing.getAddress(), AddressResponseDto.class));
            dto.setPropertyType(modelMapper.map(listing.getPropertyType(), PropertyTypeResponseDto.class));
            dto.setPrice(listing.getListingType() == ListingType.SALE ? listing.getSalePrice() : listing.getRentalPrice());
            if (listing.getFeatures() != null) {
                dto.setArea(listing.getFeatures().getArea());
                dto.setBedrooms(listing.getFeatures().getBedrooms());
            }
            if (listing.getImages() != null && !listing.getImages().isEmpty()) {
                listing.getImages().stream().filter(ListingImage::isPrimary).findFirst()
                        .ifPresent(img -> dto.setPrimaryImageUrl(img.getImageUrl()));
                if(dto.getPrimaryImageUrl() == null) {
                    listing.getImages().stream().findFirst()
                            .ifPresent(img -> dto.setPrimaryImageUrl(img.getImageUrl()));
                }
            }
            dto.setRealtor(modelMapper.map(listing.getRealtor(), UserSummaryResponseDto.class));
            return dto;
        });
    }

    @Override
    @Transactional
    public ListingDetailResponseDto approveListing(Long listingId, Long adminId) {
        // Kiểm tra adminId có quyền admin không (nếu cần)
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin User not found with id: " + adminId));
        if(admin.getRole() != RoleType.ADMIN){
            throw new AccessDeniedException("User is not an Admin.");
        }

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (listing.getStatus() != ListingStatus.PAID_PENDING_APPROVAL && listing.getStatus() != ListingStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Listing is not in a state that can be approved. Current status: " + listing.getStatus());
        }

        listing.setStatus(ListingStatus.APPROVED);
        // Set expires_at, ví dụ 30 ngày kể từ ngày duyệt
        listing.setExpiresAt(Timestamp.from(Instant.now().plus(30, ChronoUnit.DAYS)));
        // TODO: Gửi thông báo cho Realtor
        Listing savedListing = listingRepository.save(listing);
        return modelMapper.map(savedListing, ListingDetailResponseDto.class);
    }

    @Override
    @Transactional
    public ListingDetailResponseDto rejectListing(Long listingId, String reason, Long adminId) {
        // ... (logic kiểm tra admin và listing) ...
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin User not found with id: " + adminId));
        if(admin.getRole() != RoleType.ADMIN){
            throw new AccessDeniedException("User is not an Admin.");
        }

        // Đảm bảo dòng này tồn tại và đúng:
        Listing listing = listingRepository.findById(listingId) // << KIỂM TRA DÒNG NÀY
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        // Các dòng code tiếp theo sẽ sử dụng biến 'listing' này
        if (listing.getStatus() != ListingStatus.PAID_PENDING_APPROVAL && listing.getStatus() != ListingStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Listing is not in a state that can be rejected. Current status: " + listing.getStatus());
        }

        listing.setStatus(ListingStatus.REJECTED);
        // TODO: Lưu lý do từ chối
        // KHÔNG TỰ ĐỘNG REFUND NỮA
        // if (listing.isPostingFeePaid()) {
        //     // walletService.refundFunds(listing.getRealtor().getUserId(), listing.getPostingFeeAmount(), "Refund for rejected listing: " + listing.getTitle(), listingId);
        //     // listing.setPostingFeePaid(false);
        // }
        Listing savedListing = listingRepository.save(listing);
        return modelMapper.map(savedListing, ListingDetailResponseDto.class);
    }
}