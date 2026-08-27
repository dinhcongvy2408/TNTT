/**
 * <b>Module: Xác thực và phân quyền (JWT, filter chain, RBAC)</b>
 *
 * <p>Hoàn tất ở <b>Sprint 1</b> — xem docs/05-lo-trinh.md và docs/99 mục G.
 *
 * <p><b>Bốn mảnh ghép:</b>
 * <ul>
 *   <li>{@code config/SecurityConfig} — filter chain. Mặc định là
 *       {@code anyRequest().authenticated()}: endpoint mới tự động được bảo vệ.</li>
 *   <li>{@code filter/JwtAuthenticationFilter} — đọc Bearer token, dựng danh
 *       tính, và chặn tài khoản đang bị bắt đổi mật khẩu.</li>
 *   <li>{@code service/JwtService} — sinh và kiểm token. Đọc javadoc lớp này
 *       trước tiên: nó giải thích vì sao access token và refresh token có hai
 *       thiết kế khác hẳn nhau.</li>
 *   <li>{@code service/AuthService} — đăng nhập, làm mới (có xoay vòng token),
 *       đăng xuất, đổi mật khẩu.</li>
 * </ul>
 *
 * <p>Bảng dữ liệu phụ trách: {@code refresh_token} (migration V5)
 *
 * <p><b>KHÔNG có {@code UserDetailsService} — có chủ đích.</b> Đó là bộ đôi
 * phục vụ form login theo session. Hệ thống này xác thực bằng JWT, nên thêm nó
 * chỉ là tầng gián tiếp không ai gọi tới. docs/99 mục G4.
 *
 * <p><b>Nợ Sprint 8:</b> cookie refresh token đang {@code secure(false)} vì dev
 * chạy HTTP. Phải bật {@code true} khi có HTTPS, và cân nhắc
 * {@code SameSite=None} nếu frontend với backend khác site — docs/99 mục D1.
 */
package vn.tntt.security;
