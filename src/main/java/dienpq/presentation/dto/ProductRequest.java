package dienpq.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Mã sản phẩm không được để trống")
    private String maSP;

    @NotBlank(message = "Tên model không được để trống")
    private String tenModel;

    @NotBlank(message = "Hãng sản xuất không được để trống")
    private String hangSanXuat;

    @NotNull(message = "Giá bán không được để trống")
    @Min(value = 0, message = "Giá bán không được nhỏ hơn 0")
    private BigDecimal giaBan;

    @Min(value = 0, message = "Số lượng không được nhỏ hơn 0")
    private int soLuong;
}