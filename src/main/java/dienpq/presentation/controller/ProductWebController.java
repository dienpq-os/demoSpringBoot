package dienpq.presentation.controller;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
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

@Controller
@RequiredArgsConstructor
public class ProductWebController {
    private final ProductWebMapper productMapper;
    private final ProductAppService productService;

    // Cấu hình các bộ lọc tệp tin ảnh an toàn
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final int MAX_FILE_COUNT = 5;

    // DANH SÁCH SẢN PHẨM (Ai đăng nhập cũng được xem)
    @GetMapping("/products/list_products")
    @PreAuthorize("isAuthenticated()")
    public String listProducts(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        PagedResult<Product> productPage = productService.listPagedResult(keyword, page, size);
        List<ProductResponse> responseContent = productMapper.toResponseList(productPage.getContent());

        model.addAttribute("listProducts", responseContent);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("countProducts", productPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("tongGiaTriTonKho", productService.getInventoryValue());
        return "products/list_products";
    }

    // Hiển thị Form thêm mới sản phẩm
    @GetMapping("/products/new_product")
    @PreAuthorize("hasAnyRole('HANHCHINH', 'ADMIN')")
    public String showNewProductForm(Model model) {
        model.addAttribute("product", new ProductRequest());
        return "products/new_product";
    }

    // LƯU SẢN PHẨM MỚI
    @PostMapping("/products/save_product")
    @PreAuthorize("hasAnyRole('HANHCHINH', 'ADMIN')")
    public String saveProduct(@Valid @ModelAttribute("product") ProductRequest request,
            BindingResult result,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "mainImageIndex", defaultValue = "0") int mainImageIndex,
            RedirectAttributes ra, Principal principal, Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            return "products/new_product";
        }

        String username = principal.getName();
        try {
            List<DomainFile> domainFiles = toSecureDomainFiles(images);
            ProductDTO productDTO = productMapper.toDTO(request);
            productService.save(productDTO, domainFiles, mainImageIndex, username);
            ra.addFlashAttribute("success", "✅ Thêm sản phẩm kèm album ảnh thành công!");
        } catch (IllegalArgumentException e) {
            result.rejectValue("maSP", "error.product", e.getMessage());
            model.addAttribute("product", request);
            return "products/new_product";
        } catch (Exception e) {
            // Bảo mật: Ẩn thông tin lỗi chi tiết của hệ thống, chỉ log nội bộ (nếu có log)
            model.addAttribute("error", "❌ Đã có lỗi hệ thống xảy ra khi lưu sản phẩm. Vui lòng thử lại sau.");
            model.addAttribute("product", request);
            return "products/new_product";
        }
        return "redirect:/products/list_products";
    }

    // FORM CẬP NHẬT SẢN PHẨM
    @GetMapping("/products/edit_product/{maSP}")
    @PreAuthorize("hasAnyRole('HANHCHINH', 'ADMIN')")
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

    // CẬP NHẬT SẢN PHẨM
    @PostMapping("/products/update_product/{maSP}")
    @PreAuthorize("hasAnyRole('HANHCHINH', 'ADMIN')")
    public String updateProduct(@PathVariable String maSP,
            @Valid @ModelAttribute("product") ProductRequest request,
            BindingResult result,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
            @RequestParam(value = "mainImageId", required = false) String mainImageId,
            RedirectAttributes ra, Model model, Principal principal) {

        if (principal == null) {
            return "redirect:/login";
        }

        if (result.hasErrors()) {
            Product product = productService.getProductById(maSP);
            model.addAttribute("product", request);
            model.addAttribute("albumImages", product.getImages());
            return "products/edit_product";
        }

        String username = principal.getName();
        try {
            List<DomainFile> domainFiles = toSecureDomainFiles(images);
            ProductDTO appDto = productMapper.toDTO(request);
            productService.update(maSP, appDto, domainFiles, deleteImageIds, mainImageId, username);
            ra.addFlashAttribute("success", "✅ Cập nhật sản phẩm và album ảnh thành công!");
        } catch (Exception e) {
            model.addAttribute("error", "❌ Đã có lỗi hệ thống xảy ra khi cập nhật sản phẩm. Vui lòng thử lại.");
            Product product = productService.getProductById(maSP);
            model.addAttribute("product", request);
            model.addAttribute("albumImages", product.getImages());
            return "products/edit_product";
        }
        return "redirect:/products/list_products";
    }

    // SỬA ĐỔI: Chuyển đổi XÓA SẢN PHẨM sang POST nhằm kích hoạt bộ lọc phòng thủ
    // CSRF
    @PostMapping("/products/delete/{maSP}")
    @PreAuthorize("hasAnyRole('HANHCHINH', 'ADMIN')")
    public String deleteProduct(@PathVariable("maSP") String maSP, RedirectAttributes ra, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        try {
            productService.delete(maSP, username);
            ra.addFlashAttribute("success", "✅ Đã xóa sản phẩm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Không thể xóa sản phẩm do lỗi hệ thống bảo mật.");
        }
        return "redirect:/products/list_products";
    }

    // Tiện ích bóc tách xử lý file ảnh an toàn (Chống DoS, Path Traversal, Malware
    // Injection)
    private List<DomainFile> toSecureDomainFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return List.of();
        }

        if (files.length > MAX_FILE_COUNT) {
            throw new IllegalArgumentException(
                    "Vượt quá số lượng tệp ảnh cho phép tối đa (" + MAX_FILE_COUNT + " tệp).");
        }

        List<DomainFile> domainFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {

                String contentType = file.getContentType();
                if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
                    throw new IllegalArgumentException("Chỉ chấp nhận các tệp tin định dạng ảnh hợp lệ.");
                }

                String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
                if (originalFilename.contains("..")) {
                    throw new IllegalArgumentException("Phát hiện hành vi tấn công thao túng đường dẫn tệp tin.");
                }

                String fileExtension = StringUtils.getFilenameExtension(originalFilename);
                if (fileExtension == null || !ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
                    throw new IllegalArgumentException("Hệ thống không hỗ trợ phần mở rộng tệp này.");
                }

                // Đổi tên tệp ngẫu nhiên để chống ghi đè tệp tin tĩnh trên Server
                String secureFilename = UUID.randomUUID().toString() + "." + fileExtension.toLowerCase();

                try {
                    domainFiles.add(new DomainFile(
                            "/images/products",
                            secureFilename,
                            file.getSize(),
                            file.getBytes()));
                } catch (IOException e) {
                    throw new IllegalArgumentException("Không thể đọc dữ liệu tệp tải lên.", e);
                }
            }
        }
        return domainFiles;
    }
}