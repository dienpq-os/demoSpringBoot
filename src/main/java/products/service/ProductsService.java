package products.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import products.entity.ProductImage;
import products.entity.Products;
import products.repository.ProductImageRepository;
import products.repository.ProductsRepository;

@Service
public class ProductsService {

    private final ProductsRepository productsRepository;
    private final ProductImageRepository productImageRepository;

    @Autowired
    public ProductsService(ProductsRepository productsRepository,
            ProductImageRepository productImageRepository) {
        this.productsRepository = productsRepository;
        this.productImageRepository = productImageRepository;
    }

    // Lấy tất cả sản phẩm
    public List<Products> getAllProducts() {
        return productsRepository.findAll();
    }

    // Tìm kiếm sản phẩm theo từ khóa
    public List<Products> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProducts();
        }
        return productsRepository
                .findByMaSPContainingIgnoreCaseOrTenModelContainingIgnoreCaseOrHangSanXuatContainingIgnoreCase(
                        keyword.trim(), keyword.trim(), keyword.trim());
    }

    // Lấy theo MaSP sản phẩm
    public Optional<Products> getProductsById(String maSP) {
        return productsRepository.findById(maSP);
    }

    // Lưu / Cập nhật sản phẩm
    public Products saveProducts(Products products) {
        return productsRepository.save(products);
    }

    // Xóa sản phẩm và tất cả ảnh liên quan
    @Transactional
    public void deleteProducts(String maSP) {
        // Xóa tất cả ảnh trước
        productImageRepository.deleteByProductsMaSP(maSP);
        // Sau đó xóa sản phẩm
        productsRepository.deleteById(maSP);
    }

    // Kiểm tra tồn tại
    public boolean existsProductsById(String maSP) {
        return productsRepository.existsById(maSP);
    }

    // ==================== PHẦN QUẢN LÝ ẢNH ====================

    // Lấy tất cả ảnh của một sản phẩm
    public List<ProductImage> getImagesByProductMaSP(String maSP) {
        return productImageRepository.findByProductsMaSP(maSP);
    }

    // Xóa một ảnh cụ thể theo ID
    @Transactional
    public void deleteImageById(Long imageId) {
        productImageRepository.deleteById(imageId);
    }

    // Xóa tất cả ảnh của một sản phẩm (dùng khi xóa sản phẩm)
    @Transactional
    public void deleteAllImagesByProductMaSP(String maSP) {
        productImageRepository.deleteByProductsMaSP(maSP);
    }

    // Lưu một ảnh mới
    @Transactional
    public ProductImage saveImage(ProductImage image) {
        return productImageRepository.save(image);
    }

    // Đánh dấu ảnh làm ảnh chính
    @Transactional
    public void setMainImage(String maSP, Long imageId) {
        if (imageId == null)
            return;

        // Bỏ trạng thái ảnh chính của tất cả ảnh cũ
        List<ProductImage> allImages = productImageRepository.findByProductsMaSP(maSP);
        for (ProductImage img : allImages) {
            img.setMain(false);
        }
        if (!allImages.isEmpty()) {
            productImageRepository.saveAll(allImages);
        }

        // Đánh dấu ảnh được chọn là ảnh chính
        productImageRepository.findById(imageId).ifPresent(img -> {
            img.setMain(true);
            productImageRepository.save(img);
        });
    }

    // Lấy ảnh chính của sản phẩm
    public Optional<ProductImage> getMainImage(String maSP) {
        return productImageRepository.findByProductsMaSP(maSP)
                .stream()
                .filter(ProductImage::isMain)
                .findFirst();
    }

    // ====================PHẦN PHÂN TRANG VÀ TÌM KIẾM ====================
    // Phân trang tất cả
    public Page<Products> getProductsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maSP").ascending());
        return productsRepository.findAll(pageable);
    }

    // Phân trang + tìm kiếm
    public Page<Products> searchProductsPaginated(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maSP").ascending());
        if (keyword == null || keyword.trim().isEmpty()) {
            return getProductsPaginated(page, size);
        }
        return productsRepository
                .findByMaSPContainingIgnoreCaseOrTenModelContainingIgnoreCaseOrHangSanXuatContainingIgnoreCase(
                        keyword.trim(), keyword.trim(), keyword.trim(), pageable);
    }
}