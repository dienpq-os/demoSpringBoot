package products.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                // 1. Cho phép tài nguyên tĩnh & trang chủ
                                                .requestMatchers("/", "/home", "/css/**", "/js/**", "/images/**",
                                                                "/login")
                                                .permitAll()
                                                // 2. Mở quyền Đổi mật khẩu (Dùng gạch dưới _ để khớp với HTML của bạn)
                                                // DÒNG NÀY PHẢI ĐỨNG TRƯỚC /users/**
                                                .requestMatchers("/users/change_password", "/users/update_password")
                                                .authenticated()
                                                // 3. Phân quyền Quản trị User (Chỉ Admin mới được vào các mục khác của
                                                // /users)
                                                .requestMatchers("/users/**").hasRole("ADMIN")
                                                // 4. Các quyền khác
                                                .requestMatchers("/products/**").hasAnyRole("HANHCHINH", "ADMIN")
                                                .requestMatchers("/administration", "/dashboard/**",
                                                                "/products/list_products")
                                                .authenticated()
                                                .anyRequest().authenticated())
                                // Tắt CSRF cho H2 và API (nếu cần test Postman)
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/product/**"))
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .defaultSuccessUrl("/administration", true)
                                                .permitAll())
                                .requestCache(cache -> cache.disable()) // Vô hiệu hóa bộ nhớ đệm điều hướng
                                .logout(logout -> logout
                                                .logoutUrl("/logout") // Đường dẫn kích hoạt logout
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true) // Xóa session
                                                .deleteCookies("JSESSIONID") // Xóa cookie
                                                .permitAll());

                return http.build();
        }

        @Bean
        public ObjectMapper objectMapper() {
                return new ObjectMapper();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

}