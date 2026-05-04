package products.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProductsDTO {

    @NotBlank(message = "Mã sản phẩm không được để trống")
    @Size(min = 3, max = 20, message = "Mã sản phẩm phải từ 3 đến 20 ký tự")
    private String maSP;

    @NotBlank(message = "Tên model không được để trống")
    @Size(min = 2, max = 100, message = "Tên model phải từ 2 đến 100 ký tự")
    private String tenModel;

    @NotBlank(message = "Hãng sản xuất không được để trống")
    private String hangSanXuat;

    @Positive(message = "Giá bán phải lớn hơn 0")
    @NotNull(message = "Giá bán không được để trống")
    private double giaBan;

    @Min(value = 0, message = "Số lượng không được âm")
    private int soLuong;

    // Danh sách đường dẫn ảnh (dùng để hiển thị trên form và list)
    private List<String> imageUrls = new ArrayList<>();

    // Ảnh chính (để hiển thị dễ dàng)
    private String mainImageUrl;

    // Constructor rỗng
    public ProductsDTO() {
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

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getMainImageUrl() {
        return mainImageUrl;
    }

    public void setMainImageUrl(String mainImageUrl) {
        this.mainImageUrl = mainImageUrl;
    }

}