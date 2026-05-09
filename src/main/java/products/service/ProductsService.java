package products.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import products.dto.ProductsDTO;
import products.entity.ProductImage;
import products.entity.Products;
import products.repository.ProductImageRepository;
import products.repository.ProductsRepository;

@Service
@Transactional(readOnly = true)
public class ProductsService {

    @Autowired
    private ProductsRepository productsRepository;
    @Autowired
    private ProductImageRepository productImageRepository;

    // Thêm vào trong lớp ProductsService
    public List<Products> getAllProducts() {
        return productsRepository.findAll();
    }

    // --- NGHIỆP VỤ DANH SÁCH & PHÂN TRANG ---
    public Page<Products> getProductsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maSP").ascending());
        return productsRepository.findAll(pageable);
    }

    public Page<Products> searchProductsPaginated(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maSP").ascending());
        if (keyword == null || keyword.trim().isEmpty()) {
            return getProductsPaginated(page, size);
        }
        String k = keyword.trim();
        return productsRepository
                .findByMaSPContainingIgnoreCaseOrTenModelContainingIgnoreCaseOrHangSanXuatContainingIgnoreCase(
                        k, k, k, pageable);
    }

    // --- NGHIỆP VỤ THỐNG KÊ DASHBOARD (TỐI ƯU TUYỆT ĐỐI) ---

    public double calculateTotalInventoryValue() {
        Double total = productsRepository.sumTotalInventoryValue();
        return (total != null) ? total : 0.0;
    }

    public Map<String, Long> getProductsCountByBrand() {
        List<Object[]> results = productsRepository.countProductsByBrand();
        Map<String, Long> stats = new HashMap<>();
        for (Object[] result : results) {
            String brand = (String) result[0];
            Long count = (Long) result[1];
            if (brand != null && !brand.isBlank()) {
                stats.put(brand, count);
            }
        }
        return stats;
    }

    public long countTotalProducts() {
        return productsRepository.count();
    }

    public long countLowStockProducts(int threshold) {
        return productsRepository.countLowStock(threshold);
    }

    public long countOutOfStockProducts() {
        return productsRepository.countOutOfStock();
    }

    // Lấy danh sách sản phẩm tồn kho thấp
    public List<Products> getLowStockProducts(int threshold) {
        return productsRepository.findBySoLuongLessThan(threshold);
    }

    // --- CÁC THAO TÁC CƠ BẢN ---
    @Transactional
    public Products saveProducts(Products products) {
        return productsRepository.save(products);
    }

    // 1. Hàm lấy sản phẩm theo ID (trả về Optional để Controller xử lý)
    public java.util.Optional<Products> getProductsById(String maSP) {
        return productsRepository.findById(maSP);
    }

    // 2. Hàm kiểm tra mã sản phẩm đã tồn tại chưa
    public boolean existsProductsById(String maSP) {
        return productsRepository.existsById(maSP);
    }

    // Thêm vào trong lớp ProductsService
    @Transactional
    public void saveImage(ProductImage image) {
        productImageRepository.save(image);
    }

    // Xóa sản phẩm và ảnh liên quan
    @Transactional
    public void deleteProducts(String maSP) {
        // 1. Tìm sản phẩm để lấy danh sách ảnh trước khi xóa khỏi DB
        Products product = productsRepository.findById(maSP)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm mã: " + maSP));

        // 2. Lưu lại danh sách đường dẫn ảnh
        List<String> imageUrls = product.getImages().stream()
                .map(ProductImage::getImageUrl)
                .toList();

        // 3. Xóa sản phẩm trong Database
        // (Hibernate sẽ tự động xóa các bản ghi trong bảng product_image nhờ
        // CascadeType.ALL)
        productsRepository.delete(product);

        // 4. Xóa tệp vật lý trên ổ cứng
        for (String url : imageUrls) {
            deletePhysicalFile(url);
        }
    }

    // Xóa sản phẩm và ảnh liên quan
    @Transactional
    public void updateFullProduct(String maSP, ProductsDTO dto, MultipartFile[] newImages,
            List<Long> deleteImageIds, Long mainImageId) throws IOException {

        Products product = productsRepository.findById(maSP)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // 1. Cập nhật thông tin cơ bản từ DTO
        product.setTenModel(dto.getTenModel());
        product.setHangSanXuat(dto.getHangSanXuat());
        product.setGiaBan(dto.getGiaBan());
        product.setSoLuong(dto.getSoLuong());

        // 2. Xử lý XÓA ảnh cũ (Xóa cả DB và File vật lý)
        if (deleteImageIds != null) {
            for (Long id : deleteImageIds) {
                productImageRepository.findById(id).ifPresent(img -> {
                    deletePhysicalFile(img.getImageUrl()); // Hàm xóa file đã viết ở bước trước
                    product.removeImage(img); // Xóa khỏi list trong Entity (orphanRemoval sẽ tự xóa DB)
                });
            }
        }

        // 3. Xử lý THÊM ảnh mới
        if (newImages != null) {
            for (MultipartFile file : newImages) {
                if (!file.isEmpty()) {
                    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    // Logic lưu file vật lý...
                    ProductImage newImg = new ProductImage(fileName, false);
                    product.addImage(newImg);
                }
            }
        }

        // 4. Cập nhật lại ẢNH CHÍNH (isMain)
        if (mainImageId != null) {
            product.getImages().forEach(img -> img.setMain(img.getId().equals(mainImageId)));
        }

        productsRepository.save(product);
    }

    // Hàm hỗ trợ xóa file vật lý
    private void deletePhysicalFile(String imageUrl) {
        try {
            // Chuyển đổi URL (vd: /images/products/abc.jpg) thành đường dẫn thực tế
            // Lưu ý: Đường dẫn này phải khớp với nơi bạn đã lưu lúc upload
            Path filePath = Paths.get("src/main/resources/static" + imageUrl);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println(">>> Đã xóa file vật lý: " + filePath.toString());
            }
        } catch (Exception e) {
            System.err.println(">>> Không thể xóa file: " + imageUrl + " - Lỗi: " + e.getMessage());
        }
    }

    // Cập nhật product với DTO và xử lý ảnh
    @Transactional
    public void updateFullProduct(String maSP, ProductsDTO dto, MultipartFile[] images,
            List<Long> deleteImageIds, String mainImageId) throws IOException {

        // 1. Lấy sản phẩm hiện tại từ DB
        Products product = productsRepository.findById(maSP)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // 2. Cập nhật thông tin cơ bản
        product.setTenModel(dto.getTenModel());
        product.setHangSanXuat(dto.getHangSanXuat());
        product.setGiaBan(dto.getGiaBan());
        product.setSoLuong(dto.getSoLuong());

        // 3. Xử lý xóa ảnh cũ (nếu có chọn checkbox xóa)
        if (deleteImageIds != null) {
            // Tạo một bản sao danh sách để tránh lỗi ConcurrentModificationException khi
            // xóa
            List<ProductImage> toDelete = product.getImages().stream()
                    .filter(img -> deleteImageIds.contains(img.getId()))
                    .collect(Collectors.toList());

            for (ProductImage img : toDelete) {
                deletePhysicalFile(img.getImageUrl()); // Xóa file vật lý
                product.removeImage(img); // Xóa liên kết (orphanRemoval sẽ tự xóa trong DB)
            }
        }

        // 4. Xử lý thêm ảnh mới
        if (images != null && images.length > 0) {
            for (int i = 0; i < images.length; i++) {
                MultipartFile file = images[i];
                if (!file.isEmpty()) {
                    String fileName = savePhysicalFile(file); // Lưu file vào /static/images/products

                    ProductImage newImg = new ProductImage();
                    newImg.setImageUrl("/images/products/" + fileName);

                    // Kiểm tra xem người dùng có chọn ảnh mới này làm ảnh chính không
                    // (Dựa trên value="new_0", "new_1" từ Javascript của bạn)
                    if (mainImageId != null && mainImageId.equals("new_" + i)) {
                        newImg.setMain(true);
                        // Tắt ảnh chính của tất cả các ảnh khác
                        product.getImages().forEach(img -> img.setMain(false));
                    } else {
                        newImg.setMain(false);
                    }

                    product.addImage(newImg); // Lệnh này giúp ảnh mới được INSERT vào DB
                }
            }
        }

        // 5. Cập nhật ảnh chính nếu người dùng chọn một trong các ảnh cũ
        if (mainImageId != null && !mainImageId.startsWith("new_")) {
            Long idLong = Long.parseLong(mainImageId);
            product.getImages().forEach(img -> img.setMain(img.getId().equals(idLong)));
        }

        productsRepository.save(product);
    }

    private String savePhysicalFile(MultipartFile file) throws IOException {
        // 1. Xác định đường dẫn thư mục lưu trữ
        // Lưu ý: "src/main/resources/static" chỉ dùng khi chạy trong môi trường Dev
        // (IDE)
        String uploadRoot = "src/main/resources/static/images/products";
        Path uploadPath = Paths.get(uploadRoot);

        // 2. Tạo thư mục nếu chưa tồn tại
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 3. Tạo tên file duy nhất để tránh trùng lặp (dùng timestamp + tên gốc)
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        // 4. Copy dữ liệu từ file upload vào đường dẫn mục tiêu
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }
}