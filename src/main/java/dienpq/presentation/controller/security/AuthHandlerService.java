package dienpq.presentation.controller.security;

import java.io.IOException;
import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import dienpq.domain.port.external.UserLoggerPort;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthHandlerService
        implements AuthenticationSuccessHandler, AuthenticationFailureHandler, LogoutSuccessHandler {

    private final UserLoggerPort userLoggerPort;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication auth) throws IOException {
        String username = auth.getName();
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // Ghi log bảo mật có đầy đủ thông tin IP và Thiết bị phục vụ điều tra
        userLoggerPort.saveLog(username,
                String.format("📥 Đăng nhập thành công. [IP: %s] [Device: %s]", ipAddress, userAgent));

        // Chiến lược điều hướng thông minh: Trả người dùng về trang họ đang cố truy cập
        // trước đó nếu có
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            log.debug("Chuyển hướng người dùng về trang yêu cầu trước đó: {}", targetUrl);
            response.sendRedirect(targetUrl);
        } else {
            // Nếu không có yêu cầu cũ nào bị chặn trước đó, mặc định đẩy về trang chủ hoặc
            // dashboard
            response.sendRedirect("/");
        }
    }

    // BỔ SUNG: Chốt chặn giám sát các hành vi dò quét mật khẩu (Chống Brute-Force)
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        // Lấy tên tài khoản mà người dùng vừa cố nhập để đăng nhập
        String username = request.getParameter("username");
        if (username == null || username.isBlank()) {
            username = "unknown_user";
        }

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String failureReason = exception.getMessage(); // Lý do thất bại (Sai pass, tài khoản bị khóa,...)

        // Ghi nhận log cảnh báo nghiêm trọng cấp độ WARN
        log.warn("CẢNH BÁO BẢO MẬT: Đăng nhập thất bại cho tài khoản '{}' từ IP: {}. Lý do: {}", username, ipAddress,
                failureReason);
        userLoggerPort.saveLog(username, String.format("❌ Đăng nhập THẤT BẠI. Lý do: %s. [IP: %s] [Device: %s]",
                failureReason, ipAddress, userAgent));

        // Điều hướng ngược lại trang login kèm thông báo lỗi cụ thể
        response.sendRedirect("/login?error=true");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication auth) throws IOException {
        String username = "unknown";

        if (auth != null) {
            username = auth.getName();
        } else {
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null) {
                username = userPrincipal.getName();
            }
        }

        String ipAddress = getClientIp(request);
        userLoggerPort.saveLog(username, String.format("📤 Đăng xuất khỏi hệ thống. [IP: %s]", ipAddress));

        response.sendRedirect("/login?logout");
    }

    /**
     * Tiện ích bóc tách địa chỉ IP thật của Client (Hỗ trợ khi ứng dụng chạy sau
     * Proxy/Load Balancer như Nginx)
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        // Trường hợp chuỗi X-Forwarded-For chứa nhiều IP qua nhiều proxy, lấy IP đầu
        // tiên
        return xfHeader.split(",")[0].trim();
    }
}