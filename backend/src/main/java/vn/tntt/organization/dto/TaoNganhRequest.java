package vn.tntt.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Tạo ngành mới.
 *
 * <p>Thực tế đây là thao tác rất hiếm — 5 ngành chuẩn đã seed ở V2. Endpoint
 * tồn tại vì docs/04 có khai báo, và vì một xứ đoàn khác dùng chung hệ thống
 * (mục tiêu mở rộng ở CLAUDE.md mục 1) có thể có cơ cấu khác.
 */
public record TaoNganhRequest(

        @NotBlank(message = "Tên ngành không được để trống")
        String tenNganh,

        @NotBlank(message = "Mã ngành không được để trống")
        // Mã chỉ gồm chữ HOA và gạch dưới, vì code so sánh bằng mã này.
        // Cho phép chữ thường hay dấu cách là mở đường cho 'Au_Nhi' và
        // 'AU_NHI' cùng tồn tại.
        @Pattern(regexp = "[A-Z][A-Z_]*",
                 message = "Mã ngành chỉ gồm chữ in hoa và dấu gạch dưới")
        String maNganh,

        @NotNull @Min(3) @Max(30) Short tuoiToiThieu,
        @NotNull @Min(3) @Max(30) Short tuoiToiDa,
        @NotNull @Min(1) Short thuTu
) {
}
