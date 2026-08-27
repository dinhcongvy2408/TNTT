package vn.tntt.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.tntt.common.response.ApiResponse;
import vn.tntt.security.dto.DangNhapRequest;
import vn.tntt.security.dto.DangNhapResponse;
import vn.tntt.security.dto.DoiMatKhauRequest;
import vn.tntt.security.dto.ThongTinToiResponse;
import vn.tntt.security.service.AuthService;
import vn.tntt.security.service.NguoiDungDangDangNhap;

import java.time.Duration;

/**
 * API xác thực — docs/04 mục "Auth".
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Đăng nhập, làm mới token, đổi mật khẩu")
public class AuthController {

    /** Tên cookie chứa refresh token. */
    public static final String COOKIE_REFRESH = "refresh_token";

    private final AuthService authService;

    // -----------------------------------------------------------------

    @PostMapping("/login")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Đăng nhập bằng email hoặc số điện thoại")
    public ResponseEntity<ApiResponse<DangNhapResponse>> dangNhap(
            @Valid @RequestBody DangNhapRequest request,
            HttpServletRequest httpRequest) {

        var ketQua = authService.dangNhap(request, layIp(httpRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, taoCookie(ketQua.refreshTokenTho()).toString())
                .body(ApiResponse.ok(ketQua.phanHoi(), "Đăng nhập thành công"));
    }

    @PostMapping("/refresh")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Làm mới access token bằng refresh token trong cookie")
    public ResponseEntity<ApiResponse<DangNhapResponse>> lamMoi(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken,
            HttpServletRequest httpRequest) {

        var ketQua = authService.lamMoi(refreshToken, layIp(httpRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, taoCookie(ketQua.refreshTokenTho()).toString())
                .body(ApiResponse.ok(ketQua.phanHoi()));
    }

    @PostMapping("/logout")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Thu hồi refresh token và xoá cookie")
    public ResponseEntity<ApiResponse<Void>> dangXuat(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshToken) {

        authService.dangXuat(refreshToken);
        // permitAll chứ không phải authenticated: access token có thể đã hết
        // hạn khi người dùng bấm đăng xuất. Bắt đăng nhập lại để đăng xuất là
        // vô lý, và tệ hơn, refresh token sẽ không bao giờ được thu hồi.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieXoa().toString())
                .body(ApiResponse.ok("Đã đăng xuất"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Thông tin tài khoản đang đăng nhập")
    public ApiResponse<ThongTinToiResponse> toi(
            @AuthenticationPrincipal NguoiDungDangDangNhap nguoiDung) {
        return ApiResponse.ok(authService.thongTinToi(nguoiDung.id()));
    }

    @PostMapping("/doi-mat-khau")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đổi mật khẩu, thu hồi mọi phiên đăng nhập khác")
    public ResponseEntity<ApiResponse<Void>> doiMatKhau(
            @AuthenticationPrincipal NguoiDungDangDangNhap nguoiDung,
            @Valid @RequestBody DoiMatKhauRequest request) {

        authService.doiMatKhau(nguoiDung.id(), request);
        // Xoá luôn cookie: mọi refresh token vừa bị thu hồi, giữ lại cookie
        // chỉ khiến lần /refresh kế tiếp thất bại một cách khó hiểu.
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieXoa().toString())
                .body(ApiResponse.ok("Đã đổi mật khẩu. Vui lòng đăng nhập lại."));
    }

    // -----------------------------------------------------------------
    // Cookie
    // -----------------------------------------------------------------

    /**
     * Cookie chứa refresh token.
     *
     * <p>Bốn thuộc tính, mỗi cái chặn một kiểu tấn công:
     * <ul>
     *   <li><b>httpOnly</b> — JavaScript không đọc được. Đây là lý do chính
     *       để dùng cookie thay vì localStorage: một đoạn script lạ lọt vào
     *       trang (XSS) đọc sạch localStorage, nhưng không chạm được vào
     *       cookie HttpOnly.</li>
     *   <li><b>secure</b> — chỉ gửi qua HTTPS. Ở dev là {@code false} vì
     *       localhost chạy HTTP; Sprint 8 phải bật lên.</li>
     *   <li><b>sameSite=Lax</b> — trình duyệt không gửi cookie này khi request
     *       xuất phát từ website khác, tức là chặn CSRF. Sprint 8, nếu
     *       frontend ở Vercel còn backend ở VPS thì phải đổi sang
     *       {@code None} + {@code Secure} — xem docs/99 mục D1.</li>
     *   <li><b>path</b> — chỉ gửi tới đúng nhóm endpoint auth. Mọi request
     *       khác không mang theo cookie này, nên nó không lộ ra ở những nơi
     *       không cần.</li>
     * </ul>
     */
    private ResponseCookie taoCookie(String token) {
        return ResponseCookie.from(COOKIE_REFRESH, token)
                .httpOnly(true)
                .secure(false)          // SPRINT 8: bật true khi có HTTPS
                .sameSite("Lax")        // SPRINT 8: "None" nếu khác site
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();
    }

    /** Cookie rỗng, maxAge = 0 — cách chuẩn để bảo trình duyệt xoá cookie. */
    private ResponseCookie cookieXoa() {
        return ResponseCookie.from(COOKIE_REFRESH, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
    }

    /**
     * IP của người gọi, để ghi vào bảng refresh_token phục vụ truy vết.
     *
     * <p>Đọc {@code X-Forwarded-For} trước vì Sprint 8 sẽ có Nginx đứng trước:
     * khi đó {@code getRemoteAddr()} luôn trả về IP của chính Nginx.
     */
    private String layIp(HttpServletRequest request) {
        String chuyenTiep = request.getHeader("X-Forwarded-For");
        if (chuyenTiep != null && !chuyenTiep.isBlank()) {
            // Header này là danh sách "client, proxy1, proxy2" — phần tử đầu
            // là client thật.
            return chuyenTiep.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
