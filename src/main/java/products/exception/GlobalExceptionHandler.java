package products.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý lỗi khi không tìm thấy sản phẩm (IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "⚠️ " + ex.getMessage());
        return "redirect:/products/list_products";
    }

    // Xử lý lỗi tổng quát (tất cả các exception khác)
    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, RedirectAttributes redirectAttributes) {
        // In lỗi ra console để dễ debug
        ex.printStackTrace();

        redirectAttributes.addFlashAttribute("error",
                "❌ Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau!");

        return "redirect:/products/list_products";
    }
}