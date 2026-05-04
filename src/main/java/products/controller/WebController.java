package products.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import products.dto.ProductsDTO;
import products.entity.ProductImage;
import products.entity.Products;
import products.service.ProductsService;

@Controller
public class WebController {

    private final ProductsService productsService;

    @Autowired
    public WebController(ProductsService productsService) {
        this.productsService = productsService;
    }

    @GetMapping({ "/", "/home" })
    public String home() {
        return "home"; // Trỏ đến file: templates/home.html
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // Trỏ đến file: templates/login.html
    }

    @GetMapping("/administration")
    public String administration() {
        return "administration"; // Trỏ đến file: templates/administration.html
    }

    // List_products: Hiển thị danh sách điện thoại phân trang
    @GetMapping("/products/list_products")
    public String list_products(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<Products> productPage;

        if (keyword != null && !keyword.trim().isEmpty()) {
            productPage = productsService.searchProductsPaginated(keyword.trim(), page, size);
        } else {
            productPage = productsService.getProductsPaginated(page, size);
        }

        // Tính tổng giá trị tồn kho
        double tongGiaTriTonKho = productPage.getContent().stream()
                .mapToDouble(p -> p.getGiaBan() * p.getSoLuong())
                .sum();

        model.addAttribute("productPage", productPage);
        model.addAttribute("listProducts", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("countProducts", productPage.getTotalElements());
        model.addAttribute("tongGiaTriTonKho", tongGiaTriTonKho);
        model.addAttribute("keyword", keyword);

        return "products/list_products"; // Trỏ đến file: templates/products/list_products.html
    }

    // Hiển thị form thêm điện thoại mới
    @GetMapping("/products/new_product")
    public String showNewProductForm(Model model) {
        model.addAttribute("product", new ProductsDTO()); // Truyền đối tượng rỗng để binding form
        return "products/new_product"; // Trỏ đến file: templates/products/new_product.html
    }

    // === PHẦN LƯU DỮ LIỆU ===
    @PostMapping("/products/save_product")
    public String saveProduct(@Valid @ModelAttribute("product") ProductsDTO dto,
            BindingResult bindingResult,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "mainImageIndex", defaultValue = "0") int mainImageIndex,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "products/new_product";
        }

        // Lưu sản phẩm
        Products product = new Products();
        product.setMaSP(dto.getMaSP());
        product.setTenModel(dto.getTenModel());
        product.setHangSanXuat(dto.getHangSanXuat());
        product.setGiaBan(dto.getGiaBan());
        product.setSoLuong(dto.getSoLuong());

        Products savedProduct = productsService.saveProducts(product);

        // Upload ảnh và set ảnh chính
        if (images != null && images.length > 0) {
            for (int i = 0; i < images.length; i++) {
                MultipartFile file = images[i];
                if (!file.isEmpty()) {
                    try {
                        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                        Path uploadDir = Paths.get("src/main/resources/static/images/products");
                        Files.createDirectories(uploadDir);
                        Path filePath = uploadDir.resolve(fileName);
                        file.transferTo(filePath);

                        ProductImage newImage = new ProductImage();
                        newImage.setImageUrl("/images/products/" + fileName);
                        newImage.setMain(i == mainImageIndex); // Set ảnh chính
                        newImage.setProducts(savedProduct);
                        productsService.saveImage(newImage);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        redirectAttributes.addFlashAttribute("success", "✅ Thêm sản phẩm thành công!");
        return "redirect:/products/list_products";
    }

    // Hiển thị form sửa điện thoại
    @GetMapping("/products/edit_product/{maSP}")
    public String showEditProductForm(@PathVariable("maSP") String maSP, Model model) {
        Products proEntity = productsService.getProductsById(maSP)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy product với maSP: " + maSP));

        // Chuyển Entity sang DTO để hiển thị trên form
        ProductsDTO dto = new ProductsDTO();
        dto.setMaSP(proEntity.getMaSP());
        dto.setTenModel(proEntity.getTenModel());
        dto.setHangSanXuat(proEntity.getHangSanXuat());
        dto.setGiaBan(proEntity.getGiaBan());
        dto.setSoLuong(proEntity.getSoLuong());

        model.addAttribute("product", dto);
        // Truyền thêm danh sách ảnh vào model
        model.addAttribute("images", proEntity.getImages()); // ← Thêm dòng này

        return "products/edit_product"; // sẽ render file templates/products/edit_product.html
    }

    // Xử lý cập nhật điện thoại
    @PostMapping("/products/update_product/{maSP}")
    public String updateProduct(@PathVariable String maSP,
            @Valid @ModelAttribute("product") ProductsDTO dto,
            BindingResult bindingResult,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
            @RequestParam(value = "mainImageId", required = false) String mainImageIdStr, // String để nhận cả ID cũ và
                                                                                          // new_
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "products/edit_product";
        }

        Products product = productsService.getProductsById(maSP)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với mã: " + maSP));

        // Cập nhật thông tin
        product.setTenModel(dto.getTenModel());
        product.setHangSanXuat(dto.getHangSanXuat());
        product.setGiaBan(dto.getGiaBan());
        product.setSoLuong(dto.getSoLuong());

        // Xóa ảnh được tick
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            for (Long imageId : deleteImageIds) {
                productsService.deleteImageById(imageId);
            }
        }

        // Lưu sản phẩm
        Products savedProduct = productsService.saveProducts(product);

        // Xử lý ảnh chính
        if (mainImageIdStr != null && !mainImageIdStr.isEmpty()) {
            if (mainImageIdStr.startsWith("new_")) {
                // Ảnh chính là ảnh mới → sẽ xử lý sau khi upload
            } else {
                try {
                    Long mainId = Long.parseLong(mainImageIdStr);
                    productsService.setMainImage(maSP, mainId);
                } catch (Exception ignored) {
                }
            }
        }

        // Upload ảnh mới
        if (images != null && images.length > 0) {
            int newImageIndex = 0;
            for (MultipartFile file : images) {
                if (!file.isEmpty()) {
                    try {
                        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                        Path uploadDir = Paths.get("src/main/resources/static/images/products");
                        Files.createDirectories(uploadDir);
                        Path filePath = uploadDir.resolve(fileName);
                        file.transferTo(filePath);

                        ProductImage newImage = new ProductImage();
                        newImage.setImageUrl("/images/products/" + fileName);
                        newImage.setMain(false);
                        newImage.setProducts(savedProduct);
                        ProductImage savedImage = productsService.saveImage(newImage);

                        // Kiểm tra xem ảnh này có phải là ảnh chính không
                        if (mainImageIdStr != null && mainImageIdStr.equals("new_" + newImageIndex)) {
                            productsService.setMainImage(maSP, savedImage.getId());
                        }

                        newImageIndex++;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        redirectAttributes.addFlashAttribute("success", "✅ Cập nhật thành công!");
        return "redirect:/products/list_products";
    }

    // Xóa điện thoại
    @GetMapping("/products/delete/{maSP}")
    public String deleteProduct(@PathVariable("maSP") String maSP, RedirectAttributes redirectAttributes) {
        productsService.deleteProducts(maSP);
        redirectAttributes.addFlashAttribute("success", "✅ Xóa thành công!");
        return "redirect:/products/list_products";
    }
}