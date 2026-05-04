package products.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import products.entity.Products;
import products.service.ProductsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/product")
@CrossOrigin(origins = "*") // Cho phép frontend gọi API (nếu cần sau này)
public class ApiController {

    @Autowired
    private ProductsService productsService;

    // 1. Lấy danh sách tất cả điện thoại
    @GetMapping
    public List<Products> listProducts() {
        return productsService.getAllProducts();
    }

    // 2. Lấy một điện thoại theo ID
    @GetMapping("/{maSP}")
    public ResponseEntity<Products> listProductById(@PathVariable String maSP) {
        return productsService.getProductsById(maSP).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. Thêm điện thoại mới
    @PostMapping
    public ResponseEntity<Products> addProduct(@Valid @RequestBody Products products) {
        Products saved = productsService.saveProducts(products);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 4. Cập nhật điện thoại
    @PutMapping("/{maSP}")
    public ResponseEntity<?> updateProduct(@PathVariable String maSP,
            @Valid @RequestBody Products products) {
        if (!productsService.existsProductsById(maSP)) {
            return ResponseEntity.notFound().build();
        }
        products.setMaSP(maSP); // Đảm bảo giữ nguyên maSP
        return ResponseEntity.ok(productsService.saveProducts(products));
    }

    // 5. Xóa điện thoại
    @DeleteMapping("/{maSP}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String maSP) {
        if (!productsService.existsProductsById(maSP)) {
            return ResponseEntity.notFound().build();
        }
        productsService.deleteProducts(maSP);
        return ResponseEntity.noContent().build();
    }
}