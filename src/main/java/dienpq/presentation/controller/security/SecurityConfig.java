package dienpq.presentation.controller.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import dienpq.domain.port.external.PasswordServicePort;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Kích hoạt @PreAuthorize phòng thủ chiều sâu tại tầng Controller/Service
@RequiredArgsConstructor
public class SecurityConfig {
        private final AuthHandlerService authHandler;
        private final PasswordServicePort passwordServicePort;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                // Tương thích với cơ chế CSRF mới của Spring Security 6+ cho giao diện
                // Thymeleaf/JS
                CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                requestHandler.setCsrfRequestAttributeName("_csrf");

                http
                                .authorizeHttpRequests(auth -> auth
                                                // 1. Tài nguyên tĩnh và trang công khai công ty (Truy cập tự do)
                                                .requestMatchers("/", "/home", "/css/**", "/js/**", "/fonts/**",
                                                                "/images/**", "/login")
                                                .permitAll()
                                                .requestMatchers("/h2-console/**").permitAll() // Chỉ bật ở môi trường
                                                                                               // Dev, nên tắt khi lên
                                                                                               // Production

                                                // 2. Các chức năng cấu hình cá nhân (Yêu cầu phải đăng nhập)
                                                .requestMatchers("/users/change_password", "/users/update_password")
                                                .authenticated()
                                                .requestMatchers("/api/v1/users/check-old-password").authenticated()

                                                // 3. Phân quyền xem sản phẩm (Đăng nhập là xem được)
                                                .requestMatchers("/products/list_products").authenticated()

                                                // 4. Phân quyền nghiệp vụ Sản phẩm (Cụ thể đặt trước khái quát)
                                                // Nhân sự phòng HANHCHINH hoặc ADMIN mới được Thêm/Sửa/Xóa sản phẩm
                                                .requestMatchers("/products/**").hasAnyRole("HANHCHINH", "ADMIN")

                                                // 5. Phân quyền hệ thống Quản trị tài khoản (CHỈ ADMIN được vào)
                                                .requestMatchers("/users/**").hasRole("ADMIN")
                                                .requestMatchers("/administration", "/dashboard/**").hasRole("ADMIN")

                                                // Chốt chặn an toàn cuối cùng cho các liên kết phát sinh
                                                .anyRequest().authenticated())

                                // SỬA ĐỔI CSRF: Sử dụng Cookie bảo mật, chặn đứng tấn công Session-Cookie Cross
                                // Site
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(requestHandler)
                                                .ignoringRequestMatchers("/h2-console/**") // Bỏ qua cho H2-Console nội
                                                                                           // bộ
                                )

                                // Bảo mật Header chống tấn công Clickjacking vào giao diện
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                                // QUẢN LÝ SESSION: Phòng chống tấn công Session Fixation (OWASP Top 10)
                                .sessionManagement(session -> session
                                                .sessionFixation(fixation -> fixation.newSession()) // Tạo Session ID
                                                                                                    // hoàn toàn mới
                                                                                                    // ngay khi login
                                                                                                    // thành công
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                                .maximumSessions(1) // Khóa tài khoản: Chỉ cho phép đăng nhập trên 1
                                                                    // thiết bị/trình duyệt tại một thời điểm
                                                .maxSessionsPreventsLogin(false) // Lượt đăng nhập mới sẽ đẩy phiên cũ
                                                                                 // ra ngoài, tránh khóa tài khoản vô
                                                                                 // tình khi người dùng quên đăng xuất
                                                                                 // trên thiết bị khác
                                )

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .successHandler(authHandler) // Điều hướng động thông minh và ghi log
                                                                             // thành công
                                                .failureHandler(authHandler) // BỔ SUNG: Ghi log thất bại, ngăn chặn tấn
                                                                             // công Brute-Force dò mật khẩu
                                                .permitAll())

                                // Vô hiệu hóa bộ nhớ đệm điều hướng cũ nhằm tối ưu hóa bộ lọc luồng của
                                // AuthHandler
                                .requestCache(cache -> cache.disable())

                                .logout(logout -> logout
                                                .logoutSuccessHandler(authHandler) // Xử lý ghi log kiểm toán đăng xuất
                                                                                   // an toàn
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll());

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return passwordServicePort.getTargetEncoder();
        }
}