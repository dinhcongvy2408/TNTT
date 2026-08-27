package vn.tntt.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dữ liệu tạo hoặc sửa hồ sơ thiếu nhi.
 *
 * <p>Không có {@code maThieuNhi}: mã do hệ thống sinh, client không được đặt.
 * Không có {@code daXoa}: xoá là một thao tác riêng, không phải một field.
 */
public record LuuThieuNhiRequest(

        @Size(max = 50) String tenThanh,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 120) String hoTen,

        @NotNull(message = "Ngày sinh không được để trống")
        // @Past ở đây thay cho ràng buộc CHECK mà PostgreSQL từ chối — xem
        // docs/99 mục A1. Đây là chốt chặn DUY NHẤT cho quy tắc này.
        @Past(message = "Ngày sinh phải ở quá khứ")
        LocalDate ngaySinh,

        @Pattern(regexp = "NAM|NU", message = "Giới tính chỉ nhận NAM hoặc NU")
        String gioiTinh,

        @Size(max = 120) String tenBo,
        @Size(max = 120) String tenMe,

        // Số điện thoại Việt Nam: 10 số, có thể viết +84 hoặc 0 ở đầu.
        // Không ép quá chặt vì có phụ huynh dùng số cố định hoặc số nước ngoài.
        @Pattern(regexp = "|[0-9+][0-9 .-]{7,19}",
                 message = "Số điện thoại không hợp lệ")
        String sdtPhuHuynh,

        String diaChi,
        @Size(max = 80) String giaoHo,
        String ghiChu
) {
}
