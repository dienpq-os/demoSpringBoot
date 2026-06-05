package dienpq.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

import dienpq.application.dto.UserDTO;
import dienpq.application.service.UserAppService;
import dienpq.domain.model.DomainFile;
import dienpq.presentation.dto.UserRequest;
import dienpq.presentation.dto.UserResponse;
import dienpq.presentation.mapper.UserWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserWebController {

    private final UserWebMapper webMapper;
    private final UserAppService userService;

    // Cấu hình các bộ lọc an toàn cho ảnh Avatar
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // Giới hạn Avatar tối đa 2MB chống DoS

    // DANH SÁCH TÀI KHOẢN (Chỉ dành cho ADMIN)
    @GetMapping("/users/list_users")
    @PreAuthorize("hasRole('ADMIN')")
    public String listUsers(Model model) {
        List<UserResponse> displayList = webMapper.toResponseList(userService.getAllUsers());
        model.addAttribute("listUsers", displayList);
        return "users/list_users";
    }

    // THÊM USER MỚI (Chỉ dành cho ADMIN)
    @GetMapping("/users/new_user")
    @PreAuthorize("hasRole('ADMIN')")
    public String showNewUserForm(Model model) {
        model.addAttribute("user", new UserRequest());
        return "users/new_user";
    }

    @PostMapping("/users/save")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveUser(@Valid @ModelAttribute("user") UserRequest request,
            BindingResult result,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes ra, Model model, Principal principal) {

        if (principal == null)
            return "redirect:/login";
        if (result.hasErrors())
            return "users/new_user";

        try {
            UserDTO userDTO = webMapper.toDTO(request);
            String username = principal.getName();

            userService.create(username, userDTO, toSecureDomainFile(avatar, "/images/users"));
            ra.addFlashAttribute("success", "✅ Thêm người dùng thành công!");
            return "redirect:/users/list_users";
        } catch (IllegalArgumentException e) {
            result.rejectValue("username", "error.user", e.getMessage());
            model.addAttribute("user", request);
            return "users/new_user";
        } catch (Exception e) {
            // Bảo mật: Ẩn lỗi hệ thống chi tiết
            model.addAttribute("error", "❌ Lỗi hệ thống: Không thể lưu tài khoản. Vui lòng kiểm tra lại dữ liệu.");
            model.addAttribute("user", request);
            return "users/new_user";
        }
    }

    // CẬP NHẬT USER (Chỉ dành cho ADMIN)
    @GetMapping("/users/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String showEditUserForm(@PathVariable("id") Integer id, Model model) {
        try {
            var userDomain = userService.getUserById(id);
            UserRequest requestForm = webMapper.toRequest(userDomain);

            model.addAttribute("user", requestForm);
            return "users/edit_user";
        } catch (IllegalArgumentException e) {
            return "redirect:/users/list_users";
        }
    }

    @PostMapping("/users/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUser(@PathVariable("id") Integer id,
            @Valid @ModelAttribute("user") UserRequest request,
            BindingResult result,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes ra, Model model, Principal principal) {

        if (principal == null)
            return "redirect:/login";
        if (result.hasErrors())
            return "users/edit_user";

        String username = principal.getName();
        try {
            UserDTO userDTO = webMapper.toDTO(request);
            userService.update(username, userDTO, toSecureDomainFile(avatar, "/images/users"));
            ra.addFlashAttribute("success", "✅ Cập nhật thông tin thành công!");
            return "redirect:/users/list_users";
        } catch (Exception e) {
            model.addAttribute("error", "❌ Lỗi hệ thống: Không thể cập nhật thông tin tài khoản.");
            model.addAttribute("user", request);
            return "users/edit_user";
        }
    }

    // ĐỔI MẬT KHẨU USER (Bất kỳ ai đã đăng nhập đều dùng được cho CHÍNH HỌ)
    @GetMapping("/users/change_password")
    @PreAuthorize("isAuthenticated()")
    public String showChangePasswordForm(Model model, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        try {
            var userDomain = userService.getUserByIdentity(principal.getName());
            UserRequest requestForm = webMapper.toRequest(userDomain);

            model.addAttribute("email", requestForm.getEmail());
            model.addAttribute("avatar", requestForm.getImageUrl());
            return "users/change_password";
        } catch (Exception e) {
            return "redirect:/login";
        }
    }

    @PostMapping("/users/update_password")
    @PreAuthorize("isAuthenticated()")
    public String updatePassword(@RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Principal principal, Model model, RedirectAttributes ra) {

        if (principal == null)
            return "redirect:/login";

        try {
            // Tầng nghiệp vụ (Service) bắt buộc phải kiểm tra cơ chế đổi pass có đúng tên
            // của Principal này không
            userService.changePassword(principal.getName(), oldPassword, newPassword, confirmPassword);

            ra.addFlashAttribute("success", "✅ Đổi mật khẩu thành công. Hãy đăng nhập lại với mật khẩu mới!");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "❌ " + e.getMessage()); // Các lỗi logic nghiệp vụ thuần túy được hiển thị công
                                                                // khai
            reloadChangePasswordFormContext(principal.getName(), model);
            return "users/change_password";
        } catch (Exception e) {
            model.addAttribute("error", "❌ Hệ thống đổi mật khẩu gặp sự cố. Vui lòng thử lại.");
            reloadChangePasswordFormContext(principal.getName(), model);
            return "users/change_password";
        }
    }

    // SỬA ĐỔI: Chuyển sang POST để kích hoạt chống giả mạo tấn công CSRF (Chỉ dành
    // cho ADMIN)
    @PostMapping("/users/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable("id") Integer id, RedirectAttributes ra, Principal principal) {
        if (principal == null)
            return "redirect:/login";

        try {
            String username = principal.getName();
            userService.delete(username, id);
            ra.addFlashAttribute("success", "✅ Đã xóa người dùng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Không thể xóa người dùng do lỗi hệ thống hoặc phân quyền.");
        }
        return "redirect:/users/list_users";
    }

    // Hàm tiện ích làm sạch dữ liệu đầu vào và bọc tách Byte an toàn tuyệt đối
    // (Chống RCE / Path Traversal)
    private DomainFile toSecureDomainFile(MultipartFile f, String pathDir) {
        if (f == null || f.isEmpty()) {
            return null;
        }

        // 1. Kiểm tra kích thước file để chống tấn công DoS làm đầy ổ cứng/RAM
        if (f.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước tệp tin ảnh vượt quá giới hạn tối đa cho phép (2MB).");
        }

        // 2. Kiểm tra tính hợp lệ của Content-Type
        String contentType = f.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Định dạng tệp tải lên không hợp lệ. Chỉ chấp nhận tệp tin hình ảnh.");
        }

        // 3. Khử độc ký tự Path Traversal nguy hiểm
        String originalFilename = StringUtils.cleanPath(f.getOriginalFilename());
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Tên tệp tin không hợp lệ. Phát hiện hành vi thao túng đường dẫn.");
        }

        // 4. Kiểm tra phần mở rộng định dạng file
        String fileExtension = StringUtils.getFilenameExtension(originalFilename);
        if (fileExtension == null || !ALLOWED_EXTENSIONS.contains(fileExtension.toLowerCase())) {
            throw new IllegalArgumentException("Hệ thống không hỗ trợ phần mở rộng tệp này.");
        }

        // 5. Thay đổi tên file thành chuỗi ngẫu nhiên UUID để xóa bỏ hoàn toàn dấu vết
        // tên cũ từ Hacker
        String secureFilename = UUID.randomUUID().toString() + "." + fileExtension.toLowerCase();

        try {
            return new DomainFile(
                    pathDir,
                    secureFilename,
                    f.getSize(),
                    f.getBytes());
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể xử lý dữ liệu nhị phân của tệp tin.", e);
        }
    }

    private void reloadChangePasswordFormContext(String currentUsername, Model model) {
        try {
            var userDomain = userService.getUserByIdentity(currentUsername);
            UserRequest requestForm = webMapper.toRequest(userDomain);
            model.addAttribute("email", requestForm.getEmail());
            model.addAttribute("avatar", requestForm.getImageUrl());
        } catch (Exception ignored) {
        }
    }
}