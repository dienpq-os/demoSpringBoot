package products.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.io.InputStream;
import com.fasterxml.jackson.core.JsonProcessingException;

import products.dto.ProductsDTO;
import products.entity.ProductImage;
import products.entity.Products;
import products.service.ProductsService;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import products.entity.Products;
import products.service.ProductsService;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;

@Controller
public class WebController {

    private final ProductsService productsService;

    @Autowired
    public WebController(ProductsService productsService) {
        this.productsService = productsService;
    }

    // --- ĐIỀU HƯỚNG TRANG CHỦ & LOGIN ---
    @GetMapping({ "/", "/home" })
    public String home() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/administration"; // Dùng redirect để trình duyệt gửi request mới
        }
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/administration"; // Dùng redirect để trình duyệt gửi request mới
        }
        return "login";
    }

    @GetMapping("/administration")
    public String administration() {
        return "administration";
    }

    // --- DANH SÁCH SẢN PHẨM (TỐI ƯU HIỆU NĂNG) ---
    @GetMapping("/products/list_products")
    public String list_products(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<Products> productPage = productsService.searchProductsPaginated(keyword, page, size);

        model.addAttribute("productPage", productPage);
        model.addAttribute("listProducts", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("countProducts", productPage.getTotalElements());
        model.addAttribute("keyword", keyword);

        // TỐI ƯU: Gọi thẳng hàm tính tổng tiền bằng SQL (rất nhanh)
        model.addAttribute("tongGiaTriTonKho", productsService.calculateTotalInventoryValue());

        return "products/list_products";
    }

    // Hiển thị Form thêm mới sản phẩm
    @GetMapping("/products/new_product")
    public String showNewProductForm(Model model) {
        model.addAttribute("product", new ProductsDTO());
        return "products/new_product";
    }

    // --- LƯU SẢN PHẨM & XỬ LÝ ẢNH ---
    @PostMapping("/products/save_product")
    public String saveProduct(@Valid @ModelAttribute("product") ProductsDTO dto,
            BindingResult result,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "mainImageIndex", defaultValue = "0") int mainImageIndex,
            RedirectAttributes ra, Model model) {

        if (result.hasErrors())
            return "products/new_product";

        if (productsService.existsProductsById(dto.getMaSP())) {
            result.rejectValue("maSP", "error.product", "Mã sản phẩm này đã tồn tại!");
            return "products/new_product";
        }

        try {
            // 1. Chuyển đổi DTO sang Entity
            Products product = new Products();
            product.setMaSP(dto.getMaSP());
            product.setTenModel(dto.getTenModel());
            product.setHangSanXuat(dto.getHangSanXuat());
            product.setGiaBan(dto.getGiaBan());
            product.setSoLuong(dto.getSoLuong());

            Products savedProduct = productsService.saveProducts(product);

            // 2. Xử lý upload ảnh (Có thể đưa vào Service để Controller sạch hơn)
            if (images != null && images.length > 0) {
                Path uploadDir = Paths.get("src/main/resources/static/images/products");
                if (!Files.exists(uploadDir))
                    Files.createDirectories(uploadDir);

                for (int i = 0; i < images.length; i++) {
                    if (!images[i].isEmpty()) {
                        String fileName = System.currentTimeMillis() + "_" + images[i].getOriginalFilename();
                        Files.copy(images[i].getInputStream(), uploadDir.resolve(fileName),
                                StandardCopyOption.REPLACE_EXISTING);

                        ProductImage newImage = new ProductImage();
                        newImage.setImageUrl("/images/products/" + fileName);
                        newImage.setMain(i == mainImageIndex);
                        newImage.setProducts(savedProduct);
                        productsService.saveImage(newImage);
                    }
                }
            }
            ra.addFlashAttribute("mesage", "✅ Thêm sản phẩm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("mesage", "❌ Lỗi khi lưu: " + e.getMessage());
        }

        return "redirect:/products/list_products";
    }

    @GetMapping("/products/edit_product/{maSP}")
    public String showEditForm(@PathVariable String maSP, Model model) {
        Products product = productsService.getProductsById(maSP)
                .orElseThrow(() -> new IllegalArgumentException("Mã SP không tồn tại: " + maSP));

        // Chuyển từ Entity sang DTO để mapping vào Form
        ProductsDTO dto = new ProductsDTO();
        dto.setMaSP(product.getMaSP());
        dto.setTenModel(product.getTenModel());
        dto.setHangSanXuat(product.getHangSanXuat());
        dto.setGiaBan(product.getGiaBan());
        dto.setSoLuong(product.getSoLuong());

        model.addAttribute("product", dto);
        model.addAttribute("images", product.getImages()); // Dùng để hiển thị ảnh cũ
        return "products/edit_product";
    }

    // Xử lý cập nhật điện thoại
    @PostMapping("/products/update_product/{maSP}")
    public String updateProduct(@PathVariable String maSP,
            @Valid @ModelAttribute("product") ProductsDTO dto,
            BindingResult result,
            @RequestParam(value = "images", required = false) MultipartFile[] images, // Khớp name="images"
            @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds, // Khớp
                                                                                                 // name="deleteImageIds"
            @RequestParam(value = "mainImageId", required = false) String mainImageId, // Nhận String vì có cả ID cũ và
                                                                                       // "new_index"
            RedirectAttributes ra) {
        if (result.hasErrors())
            return "products/edit_product";

        try {
            productsService.updateFullProduct(maSP, dto, images, deleteImageIds, mainImageId);
            ra.addFlashAttribute("success", "✅ Cập nhật sản phẩm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/products/list_products";
    }

    // Xóa điện thoại
    @GetMapping("/products/delete/{maSP}")
    public String deleteProduct(@PathVariable("maSP") String maSP, RedirectAttributes ra) {
        try {
            // Gọi service xử lý toàn bộ: Xóa DB + Xóa file vật lý
            productsService.deleteProducts(maSP);

            ra.addFlashAttribute("success", "✅ Đã xóa sản phẩm và các ảnh liên quan thành công!");
        } catch (Exception e) {
            // Trường hợp không tìm thấy sản phẩm hoặc lỗi khóa ngoại
            ra.addFlashAttribute("error", "❌ Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
        // Quay về danh sách sản phẩm sau khi xóa
        return "redirect:/products/list_products";
    }

    // --- DASHBOARD (TỐI ƯU HIỆU NĂNG & XỬ LÝ JSON) ---
    @Autowired
    private ObjectMapper objectMapper; // Spring sẽ tự tiêm bean này vào, không cần 'new'

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // 1. Các chỉ số nhanh (Sử dụng các hàm Service đã tối ưu bằng SQL)
            model.addAttribute("totalProducts", productsService.countTotalProducts());
            model.addAttribute("totalInventoryValue", productsService.calculateTotalInventoryValue());
            model.addAttribute("lowStockCount", productsService.countLowStockProducts(10));
            model.addAttribute("outOfStockCount", productsService.countOutOfStockProducts());

            // 2. Danh sách sản phẩm sắp hết hàng (vẫn dùng EntityGraph để load nhanh)
            model.addAttribute("lowStockProducts", productsService.getLowStockProducts(10));

            // 3. Xử lý JSON cho biểu đồ (Dùng ObjectMapper đã được Autowired)
            Map<String, Long> brandStats = productsService.getProductsCountByBrand();
            model.addAttribute("brandStatsJson", objectMapper.writeValueAsString(brandStats));

        } catch (JsonProcessingException e) {
            // Log lỗi và gửi một bản đồ trống nếu lỗi JSON để tránh lỗi Whitelabel
            model.addAttribute("brandStatsJson", "{}");
            System.err.println("Lỗi parse JSON Dashboard: " + e.getMessage());
        }

        return "dashboard";
    }

    // --- XUẤT PDF BÁO CÁO SẢN PHẨM TỒN KHO THẤP ---
    @Autowired
    private ResourceLoader resourceLoader;

    @GetMapping("/dashboard/export-pdf")
    public void exportToPDF(HttpServletResponse response) {
        try {
            // 1. Cấu hình Response để trình duyệt hiểu đây là file PDF
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=Bao_cao_ton_kho.pdf");

            // 2. Lấy dữ liệu sản phẩm tồn kho thấp (< 10)
            List<Products> lowStockList = productsService.getLowStockProducts(10);

            // 3. Nạp Font tiếng Việt Arial (Dùng ResourceLoader để an toàn tuyệt đối)
            Resource resource = resourceLoader.getResource("classpath:static/fonts/arial.ttf");
            PdfFont vietnameseFont;

            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    byte[] fontBytes = is.readAllBytes();
                    vietnameseFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H);
                }
            } else {
                // Nếu lỗi không tìm thấy font, dùng font mặc định (sẽ mất dấu tiếng Việt nhưng
                // không hỏng file)
                System.err.println("!!! CẢNH BÁO: Không tìm thấy arial.ttf tại static/fonts/");
                vietnameseFont = PdfFontFactory.createFont();
            }

            // 4. Khởi tạo iText PDF
            PdfWriter writer = new PdfWriter(response.getOutputStream());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            document.setFont(vietnameseFont); // Áp dụng font tiếng Việt

            // 5. Tiêu đề báo cáo
            document.add(new Paragraph("BÁO CÁO SẢN PHẨM TỒN KHO THẤP")
                    .setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER));

            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            document.add(new Paragraph("Ngày xuất báo cáo: " + currentTime)
                    .setItalic().setTextAlignment(TextAlignment.RIGHT).setMarginBottom(20));

            // 6. Tạo bảng dữ liệu
            // Định nghĩa tỉ lệ độ rộng các cột: 15% - 40% - 25% - 20%
            float[] columnWidths = { 15, 40, 25, 20 };
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            // Header của bảng
            table.addHeaderCell(new Cell().add(new Paragraph("Mã SP").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Tên Model").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Hãng SX").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Số lượng").setBold()));

            // Đổ dữ liệu từ danh sách vào bảng
            if (lowStockList != null && !lowStockList.isEmpty()) {
                for (Products p : lowStockList) {
                    table.addCell(new Cell().add(new Paragraph(p.getMaSP())));
                    table.addCell(new Cell().add(new Paragraph(p.getTenModel())));
                    table.addCell(new Cell().add(new Paragraph(p.getHangSanXuat())));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(p.getSoLuong()))));
                }
            } else {
                table.addCell(new Cell(1, 4).add(new Paragraph("Không có sản phẩm nào sắp hết hàng.")
                        .setTextAlignment(TextAlignment.CENTER)));
            }

            // 7. Thêm bảng vào tài liệu và đóng luồng
            document.add(table);
            document.close();
            System.out.println(">>> Xuất báo cáo PDF thành công!");

        } catch (Exception e) {
            System.err.println("!!! LỖI NGHIÊM TRỌNG KHI XUẤT PDF: " + e.getMessage());
            e.printStackTrace();
            // Không nên để response trống nếu lỗi, nhưng vì đã ghi vào OutputStream nên chỉ
            // có thể log lỗi
        }
    }
}
