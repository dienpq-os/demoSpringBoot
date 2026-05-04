package products.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
public class Products {

    @Id
    @NotBlank(message = "Mã sản phẩm không được để trống")
    @Size(min = 3, max = 20, message = "Mã sản phẩm phải từ 3 đến 20 ký tự")
    private String maSP;

    @NotBlank(message = "Tên model không được để trống")
    @Size(min = 2, max = 100, message = "Tên model phải từ 2 đến 100 ký tự")
    private String tenModel;

    @NotBlank(message = "Hãng sản xuất không được để trống")
    private String hangSanXuat;

    @Positive(message = "Giá bán phải lớn hơn 0")
    private double giaBan;

    @Min(value = 0, message = "Số lượng không được âm")
    private int soLuong;

    // Quan hệ One-to-Many với bảng product_image
    @OneToMany(mappedBy = "products", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // Constructor rỗng (bắt buộc cho JPA)
    public Products() {
    }

    public Products(String maSP, String tenModel, String hangSanXuat, double giaBan, int soLuong, String imageUrl) {
        this.maSP = maSP;
        this.tenModel = tenModel;
        this.hangSanXuat = hangSanXuat;
        this.giaBan = giaBan;
        this.soLuong = soLuong;
    }

    // Helper method để thêm ảnh vào sản phẩm
    public void addImage(ProductImage image) {
        images.add(image);
        image.setProducts(this);
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProducts(null);
    }

    // Getter và Setter
    public String getMaSP() {
        return maSP;
    }

    public void setMaSP(String maSP) {
        this.maSP = maSP;
    }

    public String getTenModel() {
        return tenModel;
    }

    public void setTenModel(String tenModel) {
        this.tenModel = tenModel;
    }

    public String getHangSanXuat() {
        return hangSanXuat;
    }

    public void setHangSanXuat(String hangSanXuat) {
        this.hangSanXuat = hangSanXuat;
    }

    public double getGiaBan() {
        return giaBan;
    }

    public void setGiaBan(double giaBan) {
        this.giaBan = giaBan;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

}