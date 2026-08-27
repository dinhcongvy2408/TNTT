package vn.tntt.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.tntt.security.filter.JwtAuthenticationFilter;

/**
 * Cấu hình Spring Security — Sprint 1 đã khoá thật.
 *
 * <p>Trước Sprint 1, filter chain này để {@code permitAll} toàn bộ. Giờ mặc
 * định là {@code authenticated()}, và danh sách công khai được liệt kê tường
 * minh bên dưới.
 *
 * <p><b>Nguyên tắc: mặc định phải là TỪ CHỐI.</b> {@code anyRequest()
 * .authenticated()} đặt cuối cùng nghĩa là mỗi endpoint mới thêm vào hệ thống
 * sẽ tự động được bảo vệ. Nếu làm ngược lại — mặc định mở, liệt kê những cái
 * cần khoá — thì mỗi lần quên là một lỗ hổng im lặng.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * BCrypt với cost 10.
     *
     * <p>Cost quyết định số vòng lặp: mỗi +1 là gấp đôi thời gian băm. 10 là
     * mặc định của Spring, khoảng 50-100ms trên máy chủ thường — đủ chậm để
     * dò từ điển trở nên vô vọng, đủ nhanh để 150 huynh trưởng đăng nhập cùng
     * lúc sáng Chủ Nhật không làm nghẽn server.
     *
     * <p>Phải khớp với hash trong migration V4 (cũng {@code $2a$10$}).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Giao CORS lại cho WebConfig. Phải khai ở đây vì filter chain
                // của Security chạy TRƯỚC Spring MVC.
                .cors(Customizer.withDefaults())

                // API stateless, xác thực bằng header Authorization — header
                // không được trình duyệt tự đính kèm nên không có lỗ CSRF để
                // chống. Refresh token tuy nằm trong cookie nhưng đã có
                // SameSite=Lax và chỉ dùng ở /api/v1/auth.
                .csrf(csrf -> csrf.disable())

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        // Preflight KHÔNG kèm header Authorization, nên bắt xác
                        // thực ở đây là mọi lời gọi từ frontend chết ngay bước đầu.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Đăng nhập và làm mới token: chưa có token thì làm sao
                        // yêu cầu token. logout cũng công khai — xem javadoc
                        // của AuthController.dangXuat.
                        .requestMatchers("/api/v1/auth/login",
                                         "/api/v1/auth/refresh",
                                         "/api/v1/auth/logout").permitAll()

                        // Máy giám sát gọi /actuator/health, nó không có tài khoản.
                        // /api/v1/health cũng công khai CÓ CHỦ ĐÍCH: nó là trang
                        // chẩn đoán, và thứ ta cần nhìn thấy nhất — "database
                        // DOWN" — lại đúng lúc không đăng nhập được. Nó không trả
                        // dữ liệu cá nhân nào, chỉ tên ứng dụng và trạng thái.
                        .requestMatchers("/actuator/health", "/api/v1/health").permitAll()

                        // Swagger đã bị tắt hẳn ở profile prod
                        // (springdoc.api-docs.enabled=false), nên mở ở đây chỉ
                        // có tác dụng tại môi trường dev.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                         "/v3/api-docs/**").permitAll()

                        // MẶC ĐỊNH TỪ CHỐI. Endpoint mới tự động được bảo vệ.
                        .anyRequest().authenticated()
                )

                // Đặt TRƯỚC UsernamePasswordAuthenticationFilter: tới lúc
                // Spring Security kiểm tra quyền thì danh tính đã sẵn sàng.
                .addFilterBefore(jwtAuthenticationFilter,
                                 UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::traLoi401)
                        .accessDeniedHandler((req, res, e) -> traLoi403(res)));

        return http.build();
    }

    /**
     * 401 khi chưa đăng nhập hoặc token hỏng.
     *
     * <p>Viết JSON bằng tay vì hai handler này chạy ở tầng FILTER, nơi
     * {@code GlobalExceptionHandler} chưa với tới ({@code @RestControllerAdvice}
     * chỉ bắt thứ ném ra từ controller). Vẫn phải giữ đúng vỏ
     * {@code ApiResponse} để frontend xử lý mọi lỗi theo một cách duy nhất.
     */
    private void traLoi401(jakarta.servlet.http.HttpServletRequest request,
                           jakarta.servlet.http.HttpServletResponse response,
                           org.springframework.security.core.AuthenticationException ex)
            throws java.io.IOException {
        vietJson(response, 401,
                """
                {"success":false,\
                "message":"Bạn chưa đăng nhập hoặc phiên đã hết hạn",\
                "errorCode":"CHUA_XAC_THUC"}""");
    }

    /** 403 khi đã đăng nhập nhưng không đủ quyền, phát hiện ở tầng filter. */
    private void traLoi403(jakarta.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        vietJson(response, 403,
                """
                {"success":false,\
                "message":"Bạn không có quyền thực hiện thao tác này",\
                "errorCode":"ACCESS_DENIED"}""");
    }

    private void vietJson(jakarta.servlet.http.HttpServletResponse response,
                          int maTrangThai, String body) throws java.io.IOException {
        response.setStatus(maTrangThai);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }
}
