package products.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import products.entity.Users;

public interface UserRepository extends JpaRepository<Users, Integer> {
    // Tìm kiếm bằng Email hoặc Username cho tính năng đăng nhập linh hoạt
    Optional<Users> findByEmailOrUsername(String email, String username);

    // Tìm kiếm bằng Email hỗ trợ lấy thông tin User khi đổi mật khẩu
    Optional<Users> findByEmail(String email);

    // Kiểm tra tồn tại để tránh trùng lặp khi thêm mới
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

}