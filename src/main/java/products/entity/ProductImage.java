package products.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_image")
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
    private boolean isMain = false; // Ảnh chính

    // Constructors
    public ProductImage() {
    }

    public ProductImage(String imageUrl, boolean isMain) {
        this.imageUrl = imageUrl;
        this.isMain = isMain;
    }

    // Getter & Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Products getProducts() {
        return products;
    }

    public void setProducts(Products products) {
        this.products = products;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isMain() {
        return isMain;
    }

    public void setMain(boolean isMain) {
        this.isMain = isMain;
    }
}