package dienpq.infrastructure.adapter.external;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import dienpq.domain.port.external.PasswordServicePort;

import java.security.SecureRandom;

@Component
public class BCryptPasswordAdapter implements PasswordServicePort {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public boolean matches(String oldPassword, String password) {
        return passwordEncoder.matches(oldPassword, password);
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // Tạo mật khẩu ngẫu nhiên bảo mật cao
    @Override
    public String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%^&*_-";
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return password.toString();
    }

    // Trả về chính instance mã hóa bảo mật bên trong
    @Override
    public PasswordEncoder getTargetEncoder() {
        return this.passwordEncoder;
    }
}
