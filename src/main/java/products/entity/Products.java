package products.entity;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "products")
@Getter // Tự động tạo tất cả Getter
@Setter // Tự động tạo tất cả Setter
@NoArgsConstructor // Tự động tạo Constructor rỗng (bắt buộc cho JPA)
@AllArgsConstructor // Tự động tạo Constructor đầy đủ tham số
@Builder // Hỗ trợ tạo đối tượng theo kiểu chuỗi (Fluent API)
public class Products {

    @Id
    @NotBlank(message = "Mã sản phẩm không được để trống")
    @Size(min = 3, max = 20, message = "Mã sản phẩm phải từ 3 đến 20 ký tự")
    @Column(length = 20)
    private String maSP;

    @NotBlank(message = "Tên model không được để trống")
    @Size(min = 2, max = 100, message = "Tên model phải từ 2 đến 100 ký tự")
    @Column(length = 100)
    private String tenModel;

    @NotBlank(message = "Hãng sản xuất không được để trống")
    @Column(length = 50)
    private String hangSanXuat;

    @Positive(message = "Giá bán phải lớn hơn 0")
    private double giaBan;

    @Min(value = 0, message = "Số lượng không được âm")
    private int soLuong;

    @BatchSize(size = 10) // Tối ưu: Lấy ảnh theo lô để tránh load chậm (N+1 Query)
    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude // Tối ưu: Tránh vòng lặp vô hạn khi in Log (nếu dùng @Data)
    private List<ProductImage> images = new ArrayList<>();

    // Helper methods: Giữ lại vì Lombok không tự tạo logic quan hệ cha-con
    public void addImage(ProductImage image) {
        if (image != null) {
            images.add(image);
            image.setProducts(this);
        }
    }

    public void removeImage(ProductImage image) {
        if (image != null) {
            images.remove(image);
            image.setProducts(null);
        }
    }
}