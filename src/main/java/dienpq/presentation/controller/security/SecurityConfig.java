package dienpq.presentation.controller.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import dienpq.domain.port.external.PasswordServicePort;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
        private final AuthHandlerService authHandler;
        private final PasswordServicePort passwordServicePort;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                // 1. Cho phép tài nguyên tĩnh & trang chủ công ty truy cập tự do
                                                .requestMatchers("/", "/home", "/css/**", "/js/**", "/fonts/**",
                                                                "/images/**", "/login")
                                                .permitAll()

                                                // 2. Đưa các trang xem chung lên TRƯỚC các bộ lọc chặn cứng theo Role
                                                // Ai đăng nhập cũng được xem danh sách sản phẩm
                                                .requestMatchers("/products/list_products")
                                                .authenticated()

                                                // 3. Các chức năng cấu hình chung của User (đổi mật khẩu, kiểm tra pass
                                                // cũ)
                                                .requestMatchers("/users/change_password", "/users/update_password",
                                                                "/api/v1/users/check-old-password")
                                                .authenticated()

                                                // 4. Phân quyền nghiệp vụ
                                                // (Nhân sự phòng HANHCHINH hoặc ADMIN mới được Thêm/Sửa/Xóa sản phẩm)
                                                .requestMatchers("/products/**").hasAnyRole("HANHCHINH", "ADMIN")

                                                // 5. Phân quyền Quản trị hệ thống tài khoản (CHỈ ADMIN được vào)
                                                .requestMatchers("/users/**").hasRole("ADMIN")

                                                // 6. Toàn bộ các trang quản trị nội bộ còn lại bắt buộc phải đăng nhập
                                                .requestMatchers("/administration", "/dashboard/**")
                                                .authenticated()

                                                // Chốt chặn cuối cùng cho các link phát sinh
                                                .anyRequest().authenticated())

                                // Bỏ qua bộ lọc kiểm soát CSRF cho môi trường Test (H2)
                                // và các API RESTful không trạng thái (/api/v1/**)
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/v1/**"))
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                // Điều hướng động và ghi log đăng nhập tập trung
                                                .successHandler(authHandler)
                                                .permitAll())
                                // Vô hiệu hóa bộ nhớ đệm điều hướng cũ
                                .requestCache(cache -> cache.disable())

                                .logout(logout -> logout
                                                // Xử lý xóa log/phiên làm việc an toàn, chống NPE
                                                .logoutSuccessHandler(authHandler)
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