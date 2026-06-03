package dienpq.presentation.controller;

import java.io.IOException;
import java.util.List;

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

import dienpq.application.dto.ProductDTO;
import dienpq.application.service.ProductAppService;
import dienpq.domain.model.DomainFile;
import dienpq.domain.model.PagedResult;
import dienpq.domain.model.Product;
import dienpq.presentation.mapper.ProductWebMapper;
import dienpq.presentation.dto.ProductRequest;
import dienpq.presentation.dto.ProductResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.security.Principal;
import java.util.Arrays;

@Controller
@RequiredArgsConstructor
public class ProductWebController {

    private final ProductWebMapper productMapper;
    private final ProductAppService productService;

    // DANH SÁCH SẢN PHẨM (ĐÃ CHUẨN HÓA DTO)
    @GetMapping("/products/list_products")
    public String listProducts(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        PagedResult<Product> productPage = productService.listPagedResult(keyword, page, size);

        // Ánh xạ toàn bộ danh sách Domain Product sang
        // ProductResponse trước khi đẩy ra Thymeleaf
        List<ProductResponse> responseContent = productMapper.toResponseList(productPage.getContent());

        model.addAttribute("listProducts", responseContent); // View chỉ làm việc với Response DTO
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("countProducts", productPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("tongGiaTriTonKho", productService.getInventoryValue());

        return "products/list_products";
    }

    // Hiển thị Form thêm mới sản phẩm
    @GetMapping("/products/new_product")
    public String showNewProductForm(Model model) {
        model.addAttribute("product", new ProductRequest());
        return "products/new_product";
    }

    // LƯU SẢN PHẨM MỚI
    @PostMapping("/products/save_product")
    public String saveProduct(@Valid @ModelAttribute("product") ProductRequest request,
            BindingResult result,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "mainImageIndex", defaultValue = "0") int mainImageIndex,
            RedirectAttributes ra, Principal principal, Model model) {

        if (result.hasErrors()) {
            return "products/new_product";
        }

        String username = (principal != null) ? principal.getName() : "anonymous";

        try {
            List<DomainFile> domainFiles = toDomainFiles(images);
            ProductDTO productDTO = productMapper.toDTO(request);

            productService.save(productDTO, domainFiles, mainImageIndex, username);
            ra.addFlashAttribute("success", "✅ Thêm sản phẩm kèm album ảnh thành công!");
        } catch (IllegalArgumentException e) {
            result.rejectValue("maSP", "error.product", e.getMessage());
            // Giữ lại dữ liệu cũ trên Form cho người dùng
            model.addAttribute("product", request);
            return "products/new_product";
        } catch (Exception e) {
            model.addAttribute("error", "❌ Lỗi hệ thống khi lưu: " + e.getMessage());

            // Giữ lại dữ liệu cũ trên Form
            model.addAttribute("product", request);
            return "products/new_product";
        }

        return "redirect:/products/list_products";
    }

    // FORM CẬP NHẬT SẢN PHẨM
    @GetMapping("/products/edit_product/{maSP}")
    public String showEditForm(@PathVariable String maSP, Model model) {
        try {
            Product product = productService.getProductById(maSP);
            ProductRequest requestForm = productMapper.toRequest(product);

            model.addAttribute("product", requestForm);
            model.addAttribute("albumImages", product.getImages());
            return "products/edit_product";
        } catch (IllegalArgumentException e) {
            return "redirect:/products/list_products";
        }
    }

    @PostMapping("/products/update_product/{maSP}")
    public String updateProduct(@PathVariable String maSP,
            @Valid @ModelAttribute("product") ProductRequest request,
            BindingResult result,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
            @RequestParam(value = "mainImageId", required = false) String mainImageId,
            RedirectAttributes ra, Model model, Principal principal) {

        if (result.hasErrors()) {
            Product product = productService.getProductById(maSP);
            model.addAttribute("product", request);
            model.addAttribute("albumImages", product.getImages());
            return "products/edit_product";
        }

        String username = (principal != null) ? principal.getName() : "anonymous";

        try {
            List<DomainFile> domainFiles = toDomainFiles(images);
            ProductDTO appDto = productMapper.toDTO(request);

            productService.update(maSP, appDto, domainFiles, deleteImageIds, mainImageId, username);
            ra.addFlashAttribute("success", "✅ Cập nhật sản phẩm và album ảnh thành công!");
        } catch (Exception e) {
            model.addAttribute("error", "❌ Lỗi hệ thống khi cập nhật: " + e.getMessage());

            Product product = productService.getProductById(maSP);
            model.addAttribute("product", request); // Giữ lại dữ liệu form người dùng vừa sửa lỗi
            model.addAttribute("albumImages", product.getImages());
            return "products/edit_product";
        }
        return "redirect:/products/list_products";
    }

    // XÓA SẢN PHẨM (ĐÃ ĐỒNG BỘ USERNAME VÀ CHUẨN MÃ PATH)
    @GetMapping("/products/delete/{maSP}")
    public String deleteProduct(@PathVariable("maSP") String maSP, RedirectAttributes ra, Principal principal) {
        String username = (principal != null) ? principal.getName() : "anonymous";
        try {
            // Truyền username để tích hợp đồng bộ với hệ thống Audit Log nghiệp vụ
            productService.delete(maSP, username);
            ra.addFlashAttribute("success", "✅ Đã xóa sản phẩm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        return "redirect:/products/list_products";
    }

    // Hàm tiện ích nội bộ xử lý mảng Byte cô lập an toàn bộ nhớ
    private List<DomainFile> toDomainFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return List.of();
        }

        return Arrays.stream(files)
                .filter(file -> file != null && !file.isEmpty())
                .map(file -> {
                    try {
                        return new DomainFile(
                                "/images/products",
                                file.getOriginalFilename(),
                                file.getSize(),
                                file.getBytes());
                    } catch (IOException e) {
                        throw new IllegalArgumentException(
                                "Không thể xử lý tệp tin tải lên: " + file.getOriginalFilename(), e);
                    }
                })
                .toList(); // Thu gom trực tiếp về List bất biến (Immutable List)
    }
}