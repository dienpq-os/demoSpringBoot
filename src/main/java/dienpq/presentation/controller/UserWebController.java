package dienpq.presentation.controller;

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

import dienpq.application.dto.UserDTO;
import dienpq.application.service.UserAppService;
import dienpq.domain.model.DomainFile;
import dienpq.presentation.dto.UserRequest;
import dienpq.presentation.dto.UserResponse;
import dienpq.presentation.mapper.UserWebMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserWebController {

    private final UserWebMapper webMapper;
    private final UserAppService userService;

    // DANH SÁCH TÀI KHOẢN (đã chuẩn hóa DTO)
    @GetMapping("/users/list_users")
    public String listUsers(Model model) {
        // Biến đổi mảng thực thể lõi sang UserResponse để bảo vệ thông tin mật
        List<UserResponse> displayList = webMapper.toResponseList(userService.getAllUsers());
        model.addAttribute("listUsers", displayList);
        return "users/list_users";
    }

    // THÊM USER MỚI
    @GetMapping("/users/new_user")
    public String showNewUserForm(Model model) {
        model.addAttribute("user", new UserRequest());
        return "users/new_user";
    }

    @PostMapping("/users/save")
    public String saveUser(@Valid @ModelAttribute("user") UserRequest request,
            BindingResult result,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes ra, Model model, Principal principal) {

        if (result.hasErrors()) {
            return "users/new_user";
        }

        try {
            UserDTO userDTO = webMapper.toDTO(request);
            String username = (principal != null) ? principal.getName() : "anonymous";

            userService.create(username, userDTO, toDomainFile(avatar));
            ra.addFlashAttribute("success", "✅ Thêm người dùng thành công!");
            return "redirect:/users/list_users";
        } catch (IllegalArgumentException e) {
            // Nếu trùng tên/email nghiệp vụ, bắt lỗi gán trực tiếp vào Form field
            result.rejectValue("username", "error.user", e.getMessage());
            model.addAttribute("user", request); // Giữ lại dữ liệu cũ người dùng đã nhập
            return "users/new_user";
        } catch (Exception e) {
            model.addAttribute("error", "❌ Lỗi hệ thống khi lưu: " + e.getMessage());
            model.addAttribute("user", request); // Giữ lại dữ liệu cũ
            return "users/new_user";
        }
    }

    // CẬP NHẬT USER
    @GetMapping("/users/edit/{id}")
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
    public String updateUser(@PathVariable("id") Integer id,
            @Valid @ModelAttribute("user") UserRequest request,
            BindingResult result,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes ra, Model model, Principal principal) {

        if (result.hasErrors()) {
            return "users/edit_user";
        }
        String username = (principal != null) ? principal.getName() : "anonymous";
        try {
            UserDTO userDTO = webMapper.toDTO(request);
            userService.update(username, userDTO, toDomainFile(avatar));
            ra.addFlashAttribute("success", "✅ Cập nhật thông tin thành công!");
            return "redirect:/users/list_users";
        } catch (Exception e) {
            model.addAttribute("error", "❌ Lỗi khi cập nhật: " + e.getMessage());
            model.addAttribute("user", request); // Giữ lại dữ liệu form đang sửa lỗi
            return "users/edit_user";
        }
    }

    // ĐỔI MẬT KHẨU USER
    @GetMapping("/users/change_password")
    public String showChangePasswordForm(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

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
    public String updatePassword(@RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Principal principal, Model model, RedirectAttributes ra) {

        if (principal == null) {
            return "redirect:/login";
        }

        try {
            // Bọc khối xử lý try-catch chặn đứng lỗi crash luồng,
            // khi nhập sai pass hoặc xác nhận không khớp
            userService.changePassword(principal.getName(), oldPassword, newPassword, confirmPassword);

            // CHUẨN SECURITY BEST PRACTICE: Sau khi đổi pass thành công, đá về trang
            // login kèm chỉ thị bắt buộc đăng nhập lại với mật khẩu mới nhằm đảm bảo
            // an toàn tuyệt đối Session
            ra.addFlashAttribute("success", "✅ Đổi mật khẩu thành công. Hãy đăng nhập lại với mật khẩu mới!");
            return "redirect:/login";
        } catch (Exception e) {
            // Nếu có lỗi xác thực (sai mật khẩu cũ...), quay lại form báo lỗi đỏ trực quan
            model.addAttribute("error", "❌ Đổi mật khẩu thất bại: " + e.getMessage());
            try {
                var userDomain = userService.getUserByIdentity(principal.getName());
                UserRequest requestForm = webMapper.toRequest(userDomain);
                model.addAttribute("email", requestForm.getEmail());
                model.addAttribute("avatar", requestForm.getImageUrl());
            } catch (Exception ignored) {
            }
            return "users/change_password";
        }
    }

    // CHỨC NĂNG XÓA USER
    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") Integer id, RedirectAttributes ra, Principal principal) {
        try {
            String username = (principal != null) ? principal.getName() : "anonymous";

            userService.delete(username, id);
            ra.addFlashAttribute("success", "✅ Đã xóa người dùng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "❌ Xóa người dùng không thành công! " + e.getMessage());
        }
        return "redirect:/users/list_users";
    }

    // Hàm cô lập luồng byte vật lý từ MultipartFile của Spring thành DomainFile của
    // ứng dụng, giúp tách biệt rõ ràng giữa tầng trình bày và tầng nghiệp vụ
    private DomainFile toDomainFile(MultipartFile f) {
        return toDomainFile(f, "/images/users");
    }

    private DomainFile toDomainFile(MultipartFile f, String pathDir) {
        if (f == null || f.isEmpty()) {
            return null;
        }
        try {
            return new DomainFile(
                    pathDir, // Truyền động từ UseCase hoặc Controller quyết định
                    f.getOriginalFilename(),
                    f.getSize(),
                    f.getBytes() // Lấy mảng byte trực tiếp cực kỳ an toàn
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Không thể xử lý tệp tin: " + f.getOriginalFilename(), e);
        }
    }

}