package dienpq.presentation.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.ui.Model;
import java.util.NoSuchElementException;
import java.net.URI;
import java.net.URISyntaxException;

import dienpq.presentation.controller.ProductWebController;
import dienpq.presentation.controller.SecurityWebController;
import dienpq.presentation.controller.UserWebController;

@Slf4j
@ControllerAdvice(assignableTypes = {
        ProductWebController.class,
        SecurityWebController.class,
        UserWebController.class
})
public class GlobalExceptionHandler {

    // CHỐT CHẶN 403: Xử lý lỗi truy cập trái phép (Broken Access Control)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model, HttpServletRequest request) {
        log.warn("CẢNH BÁO BẢO MẬT: Người dùng cố gắng truy cập trái phép vùng dữ liệu bí mật. URL: {}",
                request.getRequestURI());
        model.addAttribute("message", "Bạn không có quyền truy cập vào tài nguyên hoặc chức năng này!");
        return "error/403"; // Trỏ về trang error/403.html thiết kế riêng
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoResourceFound(NoResourceFoundException ex, Model model) {
        log.debug("Không tìm thấy tài nguyên tĩnh: {}", ex.getMessage());
        model.addAttribute("message", "Tài nguyên hệ thống yêu cầu không tồn tại.");
        return "error/404";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes ra,
            HttpServletRequest request) {
        log.warn("Lỗi dữ liệu đầu vào: {}", ex.getMessage());

        // Bảo mật: Chỉ hiển thị thông báo lỗi nếu đó là thông báo tường minh thân
        // thiện, tránh ném stacktrace
        String safeMessage = (ex.getMessage() != null && !ex.getMessage().contains("java."))
                ? ex.getMessage()
                : "Dữ liệu yêu cầu không hợp lệ.";

        ra.addFlashAttribute("error", "⚠️ " + safeMessage);
        return getSecureRefererUrl(request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNoSuchElement(NoSuchElementException ex, RedirectAttributes ra) {
        log.warn("Không tìm thấy thực thể yêu cầu: {}", ex.getMessage());
        ra.addFlashAttribute("error", "⚠️ Tài nguyên yêu cầu không tồn tại hoặc đã bị xóa.");
        return "redirect:/administration";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundError(Model model) {
        model.addAttribute("message", "Trang web bạn yêu cầu không tồn tại trên hệ thống!");
        return "error/404";
    }

    // CHỐT CHẶN TỐI THƯỢNG LỖI 500: Ẩn toàn bộ stack trace khỏi người dùng cuối
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        // Log chi tiết lỗi kèm StackTrace vào file hệ thống để lập trình viên điều tra
        // nội bộ
        log.error("LỖI HỆ THỐNG NGHIÊM TRỌNG ĐÃ BỊ CHẶN ĐỨNG:", ex);

        // Tuyệt đối không đưa ex.getMessage() ra ngoài màn hình view công cộng
        model.addAttribute("message",
                "Hệ thống gặp sự cố ngoài ý muốn. Vui lòng liên hệ bộ phận Quản trị viên để được hỗ trợ!");
        return "error/500";
    }

    /**
     * Thuật toán kiểm tra và làm sạch URL chống tấn công Open Redirect (OWASP Top
     * 10)
     */
    private String getSecureRefererUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "redirect:/administration";
        }

        try {
            URI refererUri = new URI(referer);
            String rawServerName = request.getServerName();

            // Bảo mật: Chỉ cho phép chuyển hướng nếu tên miền trùng khớp với domain hiện
            // tại của máy chủ
            if (rawServerName.equalsIgnoreCase(refererUri.getHost())) {
                String path = refererUri.getPath();
                // Phòng chống vòng lặp vô hạn vào trang lỗi
                if (path != null && !path.contains("/error")) {
                    return "redirect:" + referer;
                }
            }
        } catch (URISyntaxException e) {
            log.error("Phát hiện hành vi thao túng Referer Header độc hại: {}", referer);
        }

        return "redirect:/administration";
    }
}