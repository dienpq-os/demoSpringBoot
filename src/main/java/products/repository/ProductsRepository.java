package products.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import products.entity.Products;

@Repository
public interface ProductsRepository extends JpaRepository<Products, String> {

    // JpaRepository đã cung cấp sẵn rất nhiều hàm hữu ích:
    // findAll(), findById(), save(), deleteById(), existsById(), ...
    // Tìm kiếm theo từ khóa (Mã SP, Tên Model, hoặc Hãng sản xuất) - Không phân
    // biệt hoa thường
    List<Products> findByMaSPContainingIgnoreCaseOrTenModelContainingIgnoreCaseOrHangSanXuatContainingIgnoreCase(
            String maSP, String tenModel, String hangSanXuat);

    Page<Products> findAll(Pageable pageable);

    Page<Products> findByMaSPContainingIgnoreCaseOrTenModelContainingIgnoreCaseOrHangSanXuatContainingIgnoreCase(
            String maSP, String tenModel, String hangSanXuat, Pageable pageable);
}