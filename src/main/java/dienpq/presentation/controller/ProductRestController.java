package dienpq.presentation.controller;

import dienpq.application.dto.ProductDTO;
import dienpq.application.service.ProductAppService;
import dienpq.domain.model.DomainFile;
import dienpq.domain.model.Product;
import dienpq.presentation.dto.ProductRequest;
import dienpq.presentation.dto.ProductResponse;
import dienpq.presentation.mapper.ProductWebMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductRestController {
    private final ProductAppService productService;
    private final ProductWebMapper webMapper;

    // 1. API: LƯU SẢN PHẨM MỚI
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") @Valid ProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> files,
            @RequestParam(value = "mainImageIndex", defaultValue = "0") int mainImageIndex,
            Principal principal) {

        ProductDTO productDTO = webMapper.toDTO(request);
        List<DomainFile> domainFiles = toDomainFiles(files);
        String username = (principal != null) ? principal.getName() : "anonymous";

        // Thực thi nghiệp vụ lõi
        Product savedProduct = productService.save(productDTO, domainFiles, mainImageIndex, username);

        return new ResponseEntity<>(webMapper.toResponse(savedProduct), HttpStatus.CREATED);
    }

    // 2. API: CẬP NHẬT TOÀN DIỆN SẢN PHẨM
    @PutMapping(value = "/{maSP}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateProduct(
            @PathVariable String maSP,
            @RequestPart("product") @Valid ProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> files,
            @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
            @RequestParam(value = "mainImageId", required = false) String mainImageId,
            Principal principal) {

        ProductDTO appDto = webMapper.toDTO(request);
        List<DomainFile> domainFiles = toDomainFiles(files);
        String username = (principal != null) ? principal.getName() : "anonymous";

        // Thực thi nghiệp vụ lõi
        productService.update(maSP, appDto, domainFiles, deleteImageIds, mainImageId, username);

        return ResponseEntity.noContent().build();
    }

    // 3. API: XÓA SẢN PHẨM (ĐÃ ĐỒNG BỘ HOÀN TOÀN BIẾN KIỂM TOÁN)
    @DeleteMapping("/{maSP}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String maSP, Principal principal) {
        // SỬA: Lấy động tên tài khoản đang gọi API xóa sản phẩm để đẩy vào hệ thống Log
        // nghiệp vụ
        String username = (principal != null) ? principal.getName() : "anonymous";

        // Thực thi nghiệp vụ xóa kèm định danh
        productService.delete(maSP, username);

        return ResponseEntity.noContent().build();
    }

    // Hàm tiện ích nội bộ bóc tách byte an toàn
    private List<DomainFile> toDomainFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<DomainFile> domainFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                try {
                    // Đọc trực tiếp byte[] giúp giải phóng RAM ngay khi ra khỏi hàm
                    domainFiles.add(new DomainFile(
                            "/images/products",
                            file.getOriginalFilename(),
                            file.getSize(),
                            file.getBytes()));
                } catch (IOException e) {
                    throw new IllegalArgumentException("Không thể đọc tệp tin tải lên: " + file.getOriginalFilename(),
                            e);
                }
            }
        }
        return domainFiles;
    }
}