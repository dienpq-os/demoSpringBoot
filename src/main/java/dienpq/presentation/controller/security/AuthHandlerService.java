package dienpq.presentation.controller.security;

import java.io.IOException;
import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import dienpq.domain.port.external.UserLoggerPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthHandlerService implements AuthenticationSuccessHandler, LogoutSuccessHandler {

    private final UserLoggerPort userLoggerPort;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication auth) throws IOException {
        String username = auth.getName();
        userLoggerPort.saveLog(username, "📥 Đăng nhập hệ thống thành công.");

        // Điều hướng sau khi đăng nhập về trang chủ
        response.sendRedirect("/");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication auth) throws IOException {
        String username = "unknown";

        if (auth != null) {
            username = auth.getName();
        } else {
            // Giải pháp an toàn: Nếu auth bị null, cố gắng tìm username từ Request
            // Principal của Servlet
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null) {
                username = userPrincipal.getName();
            }
        }

        // Luôn luôn ghi log: Nếu có tên thì ghi tên, nếu không sẽ ghi nhận là "unknown"
        // vào file user_system.log
        userLoggerPort.saveLog(username, "📤 Đăng xuất khỏi hệ thống.");

        // Điều hướng về trang login kèm tham số báo đã logout
        response.sendRedirect("/login?logout");
    }
}