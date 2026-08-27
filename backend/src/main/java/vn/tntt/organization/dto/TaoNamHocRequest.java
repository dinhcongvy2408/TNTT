package vn.tntt.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Dữ liệu gửi lên khi tạo năm học mới.
 *
 * <p>Vì sao có DTO riêng thay vì nhận thẳng entity {@code NamHoc}?
 * Nếu controller nhận entity, người gọi API có thể gửi kèm
 * {@code "trangThai": "DANG_HOAT_DONG"} hay {@code "id": "..."} và ghi đè
 * những thứ chỉ hệ thống được quyết. DTO chỉ có 3 field, nên client chỉ gửi
 * được đúng 3 thứ đó — phần còn lại do service đặt.
 *
 * @param tenNamHoc   dạng "2026-2027"
 * @param ngayBatDau  ngày khai giảng
 * @param ngayKetThuc ngày bế giảng, phải sau ngày bắt đầu
 */
public record TaoNamHocRequest(

        @NotBlank(message = "Tên năm học không được để trống")
        // Ép đúng dạng "2026-2027". Không có ràng buộc này thì sẽ có người
        // gõ "2026 - 2027", "26-27", "Năm 2026" — và bảng dữ liệu vận hành
        // nhiều năm sau sẽ không sắp xếp hay lọc được nữa.
        @Pattern(regexp = "[0-9]{4}-[0-9]{4}",
                 message = "Tên năm học phải có dạng 2026-2027")
        String tenNamHoc,

        @NotNull(message = "Ngày bắt đầu không được để trống")
        LocalDate ngayBatDau,

        @NotNull(message = "Ngày kết thúc không được để trống")
        LocalDate ngayKetThuc
) {
}
