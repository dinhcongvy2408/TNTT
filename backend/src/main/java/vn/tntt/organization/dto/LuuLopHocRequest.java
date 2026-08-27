package vn.tntt.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Dữ liệu tạo hoặc sửa một lớp học.
 *
 * <p>Vì sao MỘT record dùng cho cả POST lẫn PUT thay vì
 * {@code TaoLopHocRequest} và {@code SuaLopHocRequest} riêng? Vì cả hai nhận
 * đúng cùng bộ field. Tách đôi lúc này chỉ tạo ra hai chỗ phải sửa mỗi khi
 * thêm một field, và chắc chắn sẽ có ngày hai bên lệch nhau.
 *
 * <p>Sẽ tách khi nào chúng THẬT SỰ khác nhau — VD sau này tạo lớp cho phép
 * chép danh sách từ lớp năm trước, còn sửa thì không.
 *
 * @param namHocId chỉ dùng khi TẠO. Khi sửa, service bỏ qua field này: xem
 *                 {@code LopHocService.capNhat} để biết vì sao không cho
 *                 chuyển lớp sang năm học khác.
 */
public record LuuLopHocRequest(

        @NotBlank(message = "Tên lớp không được để trống")
        @Size(max = 50, message = "Tên lớp tối đa 50 ký tự")
        String tenLop,

        @NotNull(message = "Phải chọn ngành")
        UUID nganhId,

        @NotNull(message = "Phải chọn năm học")
        UUID namHocId,

        // Ấu 1, Ấu 2, Ấu 3... Giới hạn 10 là rộng rãi: ngành dài nhất trong
        // docs/02 chỉ trải 3 năm.
        @NotNull(message = "Phải nhập cấp độ")
        @Min(value = 1, message = "Cấp độ nhỏ nhất là 1")
        @Max(value = 10, message = "Cấp độ lớn nhất là 10")
        Short capDo,

        String ghiChu
) {
}
