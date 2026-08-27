package vn.tntt.security.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param dinhDanh email hoặc số điện thoại — người dùng chỉ gõ vào một ô
 * @param matKhau  mật khẩu dạng thô, chỉ tồn tại trong bộ nhớ đúng một lần
 */
public record DangNhapRequest(
        @NotBlank(message = "Nhập email hoặc số điện thoại")
        String dinhDanh,

        @NotBlank(message = "Nhập mật khẩu")
        String matKhau
) {
}
