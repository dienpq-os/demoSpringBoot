package products.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import products.entity.Products;
import products.service.ProductsService;

import java.util.List;

@RestController
@RequestMapping("/api/product")
@CrossOrigin(origins = "*")
public class ApiController {

    @Autowired
    private ProductsService productsService;

    // 1. Lấy danh sách tất cả sản phẩm
    // TỐI ƯU: Trả về trực tiếp, nhờ @BatchSize trong Entity nên ảnh sẽ được load
    // hiệu quả
    @GetMapping
    public List<Products> listProducts() {
        return productsService.getAllProducts();
    }

    // 2. Lấy một sản phẩm theo ID
    @GetMapping("/{maSP}")
    public ResponseEntity<Products> listProductById(@PathVariable String maSP) {
        return productsService.getProductsById(maSP)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Thêm mới sản phẩm
    @PostMapping
    public ResponseEntity<Products> addProduct(@Valid @RequestBody Products products) {
        // TỐI ƯU: Kiểm tra trùng mã trước khi lưu để trả về lỗi 400 thay vì 500
        if (productsService.existsProductsById(products.getMaSP())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(productsService.saveProducts(products));
    }

    // 4. Cập nhật sản phẩm
    @PutMapping("/{maSP}")
    public ResponseEntity<Products> updateProduct(@PathVariable String maSP,
            @Valid @RequestBody Products products) {
        if (!productsService.existsProductsById(maSP)) {
            return ResponseEntity.notFound().build();
        }
        products.setMaSP(maSP);
        return ResponseEntity.ok(productsService.saveProducts(products));
    }

    // 5. Xóa sản phẩm
    @DeleteMapping("/{maSP}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String maSP) {
        if (!productsService.existsProductsById(maSP)) {
            return ResponseEntity.notFound().build();
        }
        productsService.deleteProducts(maSP);
        return ResponseEntity.noContent().build();
    }
}