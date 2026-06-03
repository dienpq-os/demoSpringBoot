package dienpq.presentation.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.ui.Model;
import java.util.NoSuchElementException;

import dienpq.presentation.controller.ProductWebController;
import dienpq.presentation.controller.SecurityWebController;
import dienpq.presentation.controller.UserWebController;

@Slf4j
// ĐÃ TỐI ƯU: Chỉ định danh các Web Controller được áp dụng để tránh tranh chấp
// với REST API
@ControllerAdvice(assignableTypes = { ProductWebController.class, SecurityWebController.class,
        UserWebController.class })
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("Static resource not found: {}", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, RedirectAttributes ra,
            HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        ra.addFlashAttribute("error", "⚠️ " + ex.getMessage());
        return getRefererUrl(request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNoSuchElement(NoSuchElementException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("error", "⚠️ " + ex.getMessage());
        return "redirect:/administration";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundError(Model model) {
        model.addAttribute("message", "Trang web bạn yêu cầu không tồn tại trên hệ thống!");
        return "error/404";
    }

    // ĐÃ TỐI ƯU: Chốt chặn tối thượng lỗi 500 trả về trang lỗi tĩnh,
    // không redirect về trang administration để chống vòng lặp vô hạn (Infinite
    // Loop)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex, Model model) {
        log.error("Lỗi hệ thống nghiêm trọng", ex);
        model.addAttribute("message", "Hệ thống gặp sự cố ngoài ý muốn. Vui lòng liên hệ Admin!");
        return "error/500"; // Trỏ đến file template error/500.html tĩnh an toàn
    }

    private String getRefererUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        // Phòng ngừa nếu referer trỏ ngược về chính nó vô hạn thì chặn lại
        if (referer != null && !referer.contains("/error")) {
            return "redirect:" + referer;
        }
        return "redirect:/administration";
    }
}