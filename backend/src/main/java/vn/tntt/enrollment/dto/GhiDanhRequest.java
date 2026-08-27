package vn.tntt.enrollment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Ghi danh một em vào một lớp.
 *
 * <p>KHÔNG có {@code namHocId}: service lấy nó từ lớp. Cho client gửi lên là
 * mở đường cho việc hai cột lệch nhau, thứ mà khoá ngoại ghép ở DB sẽ chặn
 * bằng một thông báo khó hiểu.
 */
public record GhiDanhRequest(
        @NotNull(message = "Phải chọn thiếu nhi") UUID thieuNhiId,
        @NotNull(message = "Phải chọn lớp") UUID lopId
) {
}
