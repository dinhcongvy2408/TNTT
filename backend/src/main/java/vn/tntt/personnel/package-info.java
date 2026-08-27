/**
 * <b>Module: Nhân sự và phân quyền</b>
 *
 * <p>Triển khai ở <b>Sprint 3</b> — xem docs/05-lo-trinh.md.
 *
 * <p><b>Đã có sẵn từ Sprint 1:</b> entity {@code NguoiDung}, {@code VaiTro} và
 * repository của chúng — module {@code security} cần để đăng nhập. Chưa có
 * API quản trị người dùng, đó là việc của Sprint 3.
 *
 * <p>Lưu ý về {@code VaiTro.ma}: lưu trong DB là {@code ADMIN} (không tiền tố),
 * tiền tố {@code ROLE_} được ghép ở {@code JwtAuthenticationFilter} vì
 * {@code hasRole('ADMIN')} của Spring Security đi tìm quyền tên
 * {@code ROLE_ADMIN}. Nhầm chỗ này là mọi {@code @PreAuthorize} đều từ chối mà
 * không rõ lý do.
 *
 * <p>Bảng dữ liệu phụ trách: {@code nguoi_dung}, {@code vai_tro},
 * {@code nguoi_dung_vai_tro}, {@code phan_cong}
 */
package vn.tntt.personnel;
