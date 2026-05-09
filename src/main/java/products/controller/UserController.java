package products.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import products.service.UserService;
import products.dto.UsersDTO;
import products.entity.Users;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

@Controller
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    @GetMapping("/users/list_users")
    public String listUsers(Model model) {
        model.addAttribute("listUsers", userService.getAllUsersDTO());
        return "users/list_users"; // Tên file HTML
    }

    // THÊM USER MỚI
    // Hiển thị form thêm user mới
    @GetMapping("/users/new_user")
    public String showNewUserForm(Model model) {
        model.addAttribute("user", new UsersDTO()); // Truyền đối tượng rỗng để binding form
        return "users/new_user"; // Trỏ đến file: templates/users/new_userhtml
    }

    // lưu user mới sau khi submit form
    @PostMapping("/users/save")
    public String saveUser(@Valid @ModelAttribute("user") UsersDTO userDTO,
            BindingResult result,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes ra,
            Model model) {

        // 1. Kiểm tra lỗi Validation (nếu có dùng @Valid trong DTO)
        if (result.hasErrors()) {
            return "users/new_user";
        }

        // 2. Kiểm tra trùng lặp Email hoặc Username trước khi xử lý
        if (userService.existsByEmail(userDTO.getEmail())) {
            ra.addFlashAttribute("error", "Lỗi: Email này đã được sử dụng!");
            return "redirect:/users/new_user";
        }
        if (userService.existsByUsername(userDTO.getUsername())) {
            ra.addFlashAttribute("error", "Lỗi: Username này đã được sử dụng!");
            return "redirect:/users/new_user";
        }

        try {
            // 3. Chuyển đổi DTO sang Entity
            Users user = new Users();
            user.setUsername(userDTO.getUsername());
            user.setEmail(userDTO.getEmail());
            user.setRole(userDTO.getRole());

            // 4. Mật khẩu ngầm định: Lấy giá trị Role làm mật khẩu và mã hóa
            // Ví dụ: Chọn role ADMIN thì mật khẩu đăng nhập sẽ là "ADMIN"
            String defaultPassword = userDTO.getRole().toUpperCase();
            user.setPassword(passwordEncoder.encode(defaultPassword));

            // 5. Xử lý lưu ảnh đại diện (Avatar)
            if (avatar != null && !avatar.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + avatar.getOriginalFilename();
                Path uploadPath = Paths.get("src/main/resources/static/images/users");

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Files.copy(avatar.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                user.setImageUrl("/images/users/" + fileName);
            } else {
                // Thiết lập ảnh mặc định nếu không upload
                user.setImageUrl("/images/default-avatar.png");
            }

            // 6. Lưu vào Database thông qua Service, ghi ra log
            userService.saveUser(user);
            log.debug("Tài khoản {} đã tạo mới User {} thành công", "auth.getName()", user.getEmail());
            ra.addFlashAttribute("success", "✅ Thêm người dùng thành công! Mật khẩu mặc định là: " + defaultPassword);

        } catch (IOException e) {
            ra.addFlashAttribute("error", "❌ Lỗi khi lưu ảnh: " + e.getMessage());
            return "redirect:/users/new_user";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Lỗi hệ thống: " + e.getMessage());
            return "redirect:/users/new_user";
        }

        return "redirect:/users/list_users";
    }

    // CẬP NHẬT USER
    // 1. Hiển thị form cập nhật thông tin user (bao gồm cả ảnh thẻ)
    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable("id") Integer id, Model model) {
        Users user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng ID: " + id));

        // Chuyển sang DTO để hiển thị trên form
        UsersDTO dto = new UsersDTO(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getImageUrl());
        dto.setImageUrl(user.getImageUrl());

        model.addAttribute("user", dto);
        return "users/edit_user"; // Trỏ đến file templates/users/edit_user.html
    }

    // 2. Xử lý cập nhật
    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable("id") Integer id,
            @ModelAttribute("user") UsersDTO userDTO,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes ra) throws IOException {

        Users existingUser = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        // Cập nhật các thông tin cơ bản
        existingUser.setUsername(userDTO.getUsername());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setRole(userDTO.getRole());

        // Xử lý ảnh mới nếu người dùng thay đổi ảnh thẻ
        if (avatar != null && !avatar.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + avatar.getOriginalFilename();
            Path uploadPath = Paths.get("src/main/resources/static/images/users");
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            Files.copy(avatar.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            existingUser.setImageUrl("/images/users/" + fileName);
        }

        userService.saveUser(existingUser); // Lưu đè lên bản ghi cũ
        ra.addFlashAttribute("success", "✅ Cập nhật thông tin thành công!");
        return "redirect:/users/list_users";
    }

    // === PHẦN ĐỔI MẬT KHẨU ===
    @GetMapping("/users/change_password")
    public String showChangePasswordForm(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        // Tìm theo cả Email hoặc Username
        return userService.findByEmailOrUsername(principal.getName(), principal.getName()).map(user -> {
            model.addAttribute("email", user.getEmail());
            model.addAttribute("avatar", user.getImageUrl());
            return "users/change_password";
        }).orElseGet(() -> {
            System.out.println(">>> Không tìm thấy User với danh tính: " + principal.getName());
            return "redirect:/login";
        });
    }

    @PostMapping("/users/update_password")
    public String updatePassword(@RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Principal principal,
            RedirectAttributes ra) {

        // 1. Lấy thông tin User linh hoạt (Email hoặc Username)
        Users user = userService.findByEmailOrUsername(principal.getName(), principal.getName())
                .orElseThrow(
                        () -> new RuntimeException("Hệ thống không nhận diện được tài khoản: " + principal.getName()));

        // 2. Kiểm tra mật khẩu cũ (Khớp mã hóa BCrypt)
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Mật khẩu cũ không chính xác!");
            return "redirect:/users/change_password";
        }

        // 3. Kiểm tra mật khẩu mới và xác nhận
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu mới và xác nhận không khớp!");
            return "redirect:/users/change_password";
        }

        // 4. Mã hóa, Lưu và Đăng xuất
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.saveUser(user);

        ra.addFlashAttribute("success", "✅ Đổi mật khẩu thành công. Hãy đăng nhập lại với mật khẩu mới!");
        return "redirect:/administration";
    }

}
