package products.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import products.dto.UsersDTO;
import products.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import products.entity.Users;
import lombok.*;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        // Cho phép đăng nhập bằng cả email và username
        return userRepository.findByEmailOrUsername(input, input)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + input));
    }

    public List<UsersDTO> getAllUsersDTO() {
        return userRepository.findAll().stream()
                .map(user -> new UsersDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole(), user.getImageUrl()))

                .collect(Collectors.toList());
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public void saveUser(Users user) {
        userRepository.save(user);
    }

    public Optional<Users> findById(Integer id) {
        return userRepository.findById(id);
    }

    public Optional<Users> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<Users> findByEmailOrUsername(String email, String username) {
        return userRepository.findByEmailOrUsername(email, username);
    }

}