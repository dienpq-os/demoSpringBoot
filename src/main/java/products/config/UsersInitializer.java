package products.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import products.entity.Users;
import products.repository.UserRepository;

@Configuration
public class UsersInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String Email = "dienpq@gmail.com";
            String Username = "admin";
            if (userRepository.findByEmailOrUsername(Email, Username).isEmpty()) {
                Users admin = new Users();
                admin.setEmail(Email);
                admin.setUsername(Username);
                // Mật khẩu sẽ được mã hóa BCrypt trước khi lưu vào DB
                admin.setPassword(passwordEncoder.encode("admin123"));
                // Gán ROLE_ADMIN để khớp với SecurityConfig
                admin.setRole("ROLE_ADMIN");

                userRepository.save(admin);
                System.out.println(">>> Đã tạo tài khoản admin mặc định: " + Email + " / admin123");
            }
        };
    }
}
