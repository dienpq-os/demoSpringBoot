package dienpq.presentation.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SecurityWebController {

    // TRANG CHỦ: Tiêm thẳng Authentication vào tham số,
    // Spring MVC sẽ tự nạp đối tượng bảo mật hiện tại
    @GetMapping({ "/", "/home" })
    public String home(Authentication auth) {
        if (checkIsAuthenticated(auth)) {
            return "redirect:/administration";
        }
        return "home";
    }

    // TRANG ĐĂNG NHẬP
    @GetMapping("/login")
    public String login(Authentication auth) {
        if (checkIsAuthenticated(auth)) {
            return "redirect:/administration";
        }
        return "login";
    }

    // KHU VỰC QUẢN TRỊ NỘI BỘ
    @GetMapping("/administration")
    public String administration(Model model, Authentication auth) {
        // Nếu có sự cố trễ luồng khiến auth bị null,
        // chủ động đá về trang login an toàn
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Hệ Thống Quản Trị");
        model.addAttribute("username", auth.getName());

        // CHUẨN KIẾN TRÚC: Trích xuất quyền đơn giản ngay tại giao diện hiển thị
        String firstRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .findFirst()
                .orElse("ROLE_USER");

        model.addAttribute("role", firstRole);
        return "administration";
    }

    // Hàm trợ giúp nhận đối tượng động qua tham số, không gọi Static
    // SecurityContextHolder. Giúp dễ dàng truyền một đối tượng
    // Mock Authentication vào khi viết Unit Test
    private boolean checkIsAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}