package vn.tntt.security.service;

import java.util.List;
import java.util.UUID;

/**
 * Danh tính của người đang gọi request, dựng từ access token.
 *
 * <p>Đây là {@code principal} nằm trong {@code SecurityContextHolder}. Nó cố
 * ý KHÔNG phải entity {@code NguoiDung}: entity gắn với session Hibernate và
 * chứa cả {@code matKhauHash}, trong khi tầng web chỉ cần biết "ai" và
 * "quyền gì". Giữ hai thứ tách nhau thì không có đường nào để hash mật khẩu
 * lọt ra ngoài qua một lần serialize vô ý.
 *
 * @param id             id người dùng, lấy từ subject của token
 * @param hoTen          để hiển thị trên giao diện
 * @param maVaiTro       ADMIN, HUYNH_TRUONG... KHÔNG có tiền tố ROLE_
 * @param canDoiMatKhau  đang bị bắt đổi mật khẩu
 */
public record NguoiDungDangDangNhap(
        UUID id,
        String hoTen,
        List<String> maVaiTro,
        boolean canDoiMatKhau
) {
}
