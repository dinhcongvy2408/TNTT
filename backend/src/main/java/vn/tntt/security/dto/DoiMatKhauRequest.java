package vn.tntt.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoiMatKhauRequest(
        @NotBlank(message = "Nhập mật khẩu hiện tại")
        String matKhauCu,

        @NotBlank(message = "Nhập mật khẩu mới")
        // 8 ký tự là mức tối thiểu hợp lý cho người dùng thật. Đặt cao hơn
        // (12-16) ở một hệ thống mà 150 huynh trưởng phải tự nhớ mật khẩu sẽ
        // dẫn tới việc họ ghi ra giấy dán lên màn hình — mất an toàn hơn.
        @Size(min = 8, max = 72,
              message = "Mật khẩu mới phải từ 8 đến 72 ký tự")
        String matKhauMoi
) {
}
