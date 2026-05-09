package products.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import products.entity.Products;

import java.util.List;

@Repository
public interface ProductsRepository extends JpaRepository<Products, String> {

        // --- Tối ưu hiển thị danh sách (Fix N+1 Query) ---
        @EntityGraph(attributePaths = { "images" })
        Page<Products> findAll(Pageable pageable);

        @EntityGraph(attributePaths = { "images" })
        Page<Products> findByMaSPContainingIgnoreCaseOrTenModelContainingIgnoreCaseOrHangSanXuatContainingIgnoreCase(
                        String maSP, String tenModel, String hangSanXuat, Pageable pageable);

        // --- Tối ưu thống kê Dashboard (Tính toán tại DB) ---

        // 1. Tính tổng giá trị tồn kho: DB thực hiện nhân và cộng rồi trả về 1 số duy
        // nhất
        @Query("SELECT SUM(p.giaBan * p.soLuong) FROM Products p")
        Double sumTotalInventoryValue();

        // 2. Thống kê theo hãng: DB thực hiện Group By và trả về danh sách rút gọn
        @Query("SELECT p.hangSanXuat, COUNT(p) FROM Products p GROUP BY p.hangSanXuat")
        List<Object[]> countProductsByBrand();

        // 3. Đếm sản phẩm sắp hết hàng
        @Query("SELECT COUNT(p) FROM Products p WHERE p.soLuong < :threshold")
        long countLowStock(@Param("threshold") int threshold);

        // 4. Đếm sản phẩm hết hàng (soLuong = 0)
        @Query("SELECT COUNT(p) FROM Products p WHERE p.soLuong = 0")
        long countOutOfStock();

        // 5. Lấy danh sách sản phẩm tồn kho thấp (vẫn dùng EntityGraph để load ảnh
        // nhanh)
        @EntityGraph(attributePaths = { "images" })
        List<Products> findBySoLuongLessThan(int threshold);
}