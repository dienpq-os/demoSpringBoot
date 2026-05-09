package products.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "products") // QUAN TRỌNG: Loại bỏ products khỏi toString để tránh lỗi StackOverflow
@EqualsAndHashCode(exclude = "products") // QUAN TRỌNG: Tránh lỗi vòng lặp khi so sánh đối tượng
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_masp", referencedColumnName = "maSP")
    private Products products;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "is_main", nullable = false)
    @Builder.Default
    private boolean isMain = false;

    // Constructor tùy chỉnh cho việc tạo nhanh (giống bản gốc của bạn)
    public ProductImage(String imageUrl, boolean isMain) {
        this.imageUrl = imageUrl;
        this.isMain = isMain;
    }
}
