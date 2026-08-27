/**
 * <b>Module: Xác thực và phân quyền (JWT, filter chain, RBAC)</b>
 *
 * <p>Triển khai ở <b>Sprint 1</b> — xem docs/05-lo-trinh.md.
 *
 * <p><b>Đã có sẵn từ cuối Sprint 0:</b> {@code config/SecurityConfig} — filter
 * chain đang để {@code permitAll} TOÀN BỘ, chưa khoá endpoint nào. Nó có mặt
 * sớm để bật {@code @EnableMethodSecurity} (thiếu nó thì {@code @PreAuthorize}
 * bị bỏ qua trong im lặng) và để handler 403 ở {@code GlobalExceptionHandler}
 * biên dịch được. Xem docs/99 mục E2.
 *
 * <p>Bảng dữ liệu phụ trách: không có bảng riêng
 *
 * <p>Cấu trúc thư mục con theo quy ước ở CLAUDE.md mục 4:
 * {@code controller/ service/ repository/ entity/ dto/ mapper/}
 */
package vn.tntt.security;
