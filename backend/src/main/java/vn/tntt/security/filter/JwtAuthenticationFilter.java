package vn.tntt.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.tntt.security.service.JwtService;
import vn.tntt.security.service.NguoiDungDangDangNhap;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Đọc {@code Authorization: Bearer ...} và dựng danh tính cho request.
 *
 * <p>Kế thừa {@link OncePerRequestFilter} chứ không phải {@code Filter} trần:
 * một request có thể đi qua chuỗi filter nhiều lần (forward, error dispatch),
 * và ta không muốn xác thực lại mỗi lần.
 *
 * <p>Filter này KHÔNG từ chối request thiếu token — nó chỉ đơn giản không đặt
 * danh tính nào. Việc từ chối là của {@code authorizeHttpRequests} trong
 * {@code SecurityConfig}. Tách bạch như vậy thì endpoint công khai
 * ({@code /auth/login}) vẫn đi qua đây bình thường.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TIEN_TO = "Bearer ";

    /**
     * Các đường dẫn vẫn dùng được khi tài khoản đang bị bắt đổi mật khẩu.
     *
     * <p>docs/02 bước 2: mật khẩu tạm phải đổi ở lần đăng nhập đầu. Nếu chỉ
     * dựa vào frontend chuyển hướng thì ai gọi thẳng API vẫn dùng được cả hệ
     * thống bằng mật khẩu tạm — mà mật khẩu tạm thì thường được nhắn qua Zalo
     * và nằm lại đó mãi mãi.
     */
    private static final Set<String> CHO_PHEP_KHI_CAN_DOI_MAT_KHAU = Set.of(
            "/api/v1/auth/me",
            "/api/v1/auth/doi-mat-khau",
            "/api/v1/auth/logout",
            "/api/v1/health");

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(TIEN_TO)) {
            filterChain.doFilter(request, response);
            return;
        }

        var claimsTuyChon = jwtService.docToken(header.substring(TIEN_TO.length()));
        if (claimsTuyChon.isEmpty()) {
            // Token hỏng hoặc hết hạn: đi tiếp mà không có danh tính. Endpoint
            // cần quyền sẽ tự trả 401 qua AuthenticationEntryPoint.
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = claimsTuyChon.get();

        if (canChanVìPhaiDoiMatKhau(claims, request)) {
            traLoi403DoiMatKhau(response);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(dungXacThuc(claims, request));
        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private UsernamePasswordAuthenticationToken dungXacThuc(Claims claims,
                                                            HttpServletRequest request) {
        List<String> maVaiTro = claims.get(JwtService.CLAIM_VAI_TRO, List.class);

        // Ghép tiền tố ROLE_ ở ĐÂY, không lưu tiền tố trong DB.
        // hasRole('ADMIN') của Spring Security đi tìm quyền tên ROLE_ADMIN.
        // Quên bước này là mọi @PreAuthorize đều từ chối mà không rõ lý do.
        var quyen = maVaiTro == null ? List.<SimpleGrantedAuthority>of()
                : maVaiTro.stream().map(ma -> new SimpleGrantedAuthority("ROLE_" + ma)).toList();

        var nguoiDung = new NguoiDungDangDangNhap(
                jwtService.layNguoiDungId(claims),
                claims.get(JwtService.CLAIM_HO_TEN, String.class),
                maVaiTro == null ? List.of() : maVaiTro,
                Boolean.TRUE.equals(claims.get(JwtService.CLAIM_CAN_DOI_MAT_KHAU, Boolean.class)));

        var xacThuc = new UsernamePasswordAuthenticationToken(nguoiDung, null, quyen);
        xacThuc.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return xacThuc;
    }

    private boolean canChanVìPhaiDoiMatKhau(Claims claims, HttpServletRequest request) {
        boolean phaiDoi = Boolean.TRUE.equals(
                claims.get(JwtService.CLAIM_CAN_DOI_MAT_KHAU, Boolean.class));
        return phaiDoi && !CHO_PHEP_KHI_CAN_DOI_MAT_KHAU.contains(request.getRequestURI());
    }

    /**
     * Trả JSON đúng vỏ {@code ApiResponse} bằng tay.
     *
     * <p>Ở tầng filter thì {@code GlobalExceptionHandler} chưa vào cuộc —
     * {@code @RestControllerAdvice} chỉ bắt được thứ ném ra từ controller.
     * Nên phải tự viết body, và phải giữ đúng định dạng để frontend xử lý
     * đồng nhất với mọi lỗi khác.
     */
    private void traLoi403DoiMatKhau(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,\
                "message":"Bạn phải đổi mật khẩu trước khi dùng hệ thống",\
                "errorCode":"CAN_DOI_MAT_KHAU"}""");
    }
}
