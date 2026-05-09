package products.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Xử lý lỗi sai tham số hoặc không tìm thấy dữ liệu
    // (IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex,
            RedirectAttributes ra,
            HttpServletRequest request) {

        ra.addFlashAttribute("error", "⚠️ " + ex.getMessage());

        // Lấy đường dẫn mà người dùng vừa truy cập trước khi lỗi
        String referer = request.getHeader("Referer");

        // Nếu lỗi đến từ trang sản phẩm thì quay về danh sách sản phẩm
        if (referer != null && referer.contains("/products")) {
            return "redirect:/products/list_products";
        }

        // Nếu lỗi đến từ trang người dùng thì quay về trang quản trị
        return "redirect:/administration";
    }

    // 2. Xử lý lỗi bảo mật/quyền truy cập (Nếu bạn có dùng AccessDeniedException)
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public String handleAccessDenied(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "🚫 Bạn không có quyền thực hiện thao tác này!");
        return "redirect:/administration";
    }

    // 3. Xử lý tất cả các lỗi hệ thống khác
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, RedirectAttributes ra) {
        // In lỗi chi tiết ra console để lập trình viên kiểm tra
        System.err.println("!!! HỆ THỐNG GẶP LỖI NGHIÊM TRỌNG: " + ex.getMessage());
        ex.printStackTrace();

        ra.addFlashAttribute("error", "❌ Đã xảy ra lỗi hệ thống: " + ex.getMessage());

        // Mặc định đẩy về trang quản trị cho an toàn
        return "redirect:/administration";
    }
}