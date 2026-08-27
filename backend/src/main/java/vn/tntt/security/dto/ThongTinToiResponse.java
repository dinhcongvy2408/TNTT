package vn.tntt.security.dto;

import vn.tntt.personnel.entity.NguoiDung;
import vn.tntt.personnel.entity.VaiTro;

import java.util.List;
import java.util.UUID;

/**
 * Thông tin tài khoản đang đăng nhập — {@code GET /auth/me}.
 *
 * <p>Không có {@code matKhauHash}, không có ngày sinh. Chỉ đúng những gì
 * giao diện cần để hiển thị và để quyết định hiện nút nào.
 */
public record ThongTinToiResponse(
        UUID id,
        String hoTen,
        String email,
        List<String> vaiTro,
        boolean canDoiMatKhau
) {
    public static ThongTinToiResponse tu(NguoiDung nguoiDung) {
        return new ThongTinToiResponse(
                nguoiDung.getId(),
                nguoiDung.tenDayDu(),
                nguoiDung.getEmail(),
                nguoiDung.getVaiTro().stream().map(VaiTro::getMa).sorted().toList(),
                nguoiDung.isCanDoiMatKhau());
    }
}
