package dienpq.presentation.controller;

import dienpq.application.dto.DashboardSummary;
import dienpq.domain.port.external.PDFServicePort;
import dienpq.application.service.DashboardAndReportService;
import dienpq.presentation.dto.ProductResponse; // Import DTO tầng Presentation
import dienpq.presentation.mapper.ProductWebMapper; // Import Mapper tầng Presentation
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.OutputStream;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardAndReportController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardAndReportController.class);

    private final DashboardAndReportService dashboardAndReportService;
    private final PDFServicePort pdfReportExporter;
    private final ProductWebMapper productWebMapper; // Tiêm Mapper để ánh xạ dữ liệu giao diện

    // --- DASHBOARD THỐNG KÊ ---
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Lấy dữ liệu tổng hợp từ lõi UseCase
        DashboardSummary summary = dashboardAndReportService.getDashboardSummary();

        // SỬA: Chuyển đổi List<Product> thành List<ProductResponse> để bảo vệ Domain
        // Model
        List<ProductResponse> lowStockResponses = productWebMapper.toResponseList(summary.getLowStockProducts());

        model.addAttribute("totalProducts", summary.getTotalProducts());
        model.addAttribute("totalInventoryValue", summary.getTotalInventoryValue());
        model.addAttribute("lowStockCount", summary.getLowStockCount());
        model.addAttribute("outOfStockCount", summary.getOutOfStockCount());
        model.addAttribute("lowStockProducts", lowStockResponses); // Đẩy dữ liệu sạch ra Thymeleaf
        model.addAttribute("brandStatsJson", summary.getBrandStatsJson());

        return "dashboard";
    }

    // --- XUẤT FILE BÁO CÁO PDF TIẾNG VIỆT ---
    @GetMapping("/dashboard/export-pdf")
    public void exportToPDF(HttpServletResponse response) {
        // 1. Cấu hình Header cho HTTP Response
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Bao_cao_ton_kho.pdf");

        // 2. Lấy dữ liệu thuần túy từ nghiệp vụ
        DashboardSummary summary = dashboardAndReportService.getDashboardSummary();

        // Chuyển đổi sang danh sách DTO an toàn cho tầng hiển thị PDF
        List<ProductResponse> lowStockList = productWebMapper.toResponseList(summary.getLowStockProducts());

        // 3. SỬA: Bọc khối try-with-resources để tự động đóng Stream và bắt lỗi I/O
        // mạng an toàn
        try (OutputStream out = response.getOutputStream()) {
            // Ra lệnh cho Adapter in dữ liệu dạng DTO ra luồng mạng
            pdfReportExporter.exportLowStockReport(lowStockList, out);
        } catch (Exception e) {
            // Ghi nhận lỗi hạ tầng mạng một cách chính quy vào system.log, không làm sập
            // luồng ứng dụng
            logger.error("Sự cố xảy ra khi khách hàng tải file PDF báo cáo tồn kho: {}", e.getMessage());
        }
    }
}