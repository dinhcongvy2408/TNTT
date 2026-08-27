package vn.tntt.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình Spring Security.
 *
 * <p><b>ĐỌC KỸ TRẠNG THÁI HIỆN TẠI:</b> filter chain này để
 * {@code permitAll} cho MỌI đường dẫn — nghĩa là chưa khoá gì cả. Nó chưa
 * phải hàng rào bảo vệ. Nó có mặt sớm hơn Sprint 1 một nhịp vì hai lý do:
 *
 * <ol>
 *   <li>{@code @EnableMethodSecurity} bật {@code @PreAuthorize}. CLAUDE.md
 *       mục 5 yêu cầu "mọi endpoint đều phải khai báo quyền rõ ràng bằng
 *       {@code @PreAuthorize}" — không có annotation này thì
 *       {@code @PreAuthorize} bị bỏ qua HOÀN TOÀN trong im lặng, và ta sẽ
 *       tưởng đã phân quyền trong khi endpoint đang mở toang.</li>
 *   <li>Có lớp {@code AccessDeniedException} trong classpath thì handler ở
 *       {@code GlobalExceptionHandler} mới biên dịch được. Thiếu nó, lỗi
 *       "không có quyền" trả 500 thay vì 403 — xem docs/99 mục E2.</li>
 * </ol>
 *
 * <p><b>Việc của Sprint 1:</b> thay {@code .anyRequest().permitAll()} bằng
 * danh sách cụ thể, và chèn {@code JwtAuthenticationFilter} vào trước
 * {@code UsernamePasswordAuthenticationFilter}.
 */
@Configuration
@EnableWebSecurity
// Bật @PreAuthorize / @PostAuthorize. KHÔNG bỏ annotation này: thiếu nó thì
// mọi @PreAuthorize trở thành chú thích trang trí, không ai kiểm tra gì cả.
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ---------------------------------------------------------
                // CORS: giao lại cho cấu hình MVC ở common/config/WebConfig.
                //
                // Phải khai báo ở đây, nếu không filter chain của Security
                // chạy TRƯỚC Spring MVC và chặn request preflight OPTIONS
                // trước khi MVC kịp trả header CORS.
                // ---------------------------------------------------------
                .cors(Customizer.withDefaults())

                // ---------------------------------------------------------
                // Tắt CSRF.
                //
                // CSRF token sinh ra để chống việc trình duyệt TỰ ĐỘNG đính
                // kèm cookie phiên vào request do website khác kích hoạt.
                // API của ta stateless, xác thực bằng header Authorization —
                // header đó không được đính kèm tự động, nên không có lỗ hổng
                // để chống.
                //
                // Sprint 1 dùng refresh token trong cookie HttpOnly: cookie ấy
                // CHỈ dùng ở đúng một endpoint /api/v1/auth/refresh, và phải
                // đặt SameSite để chặn. Không phải toàn bộ API quay lại dùng
                // session.
                // ---------------------------------------------------------
                .csrf(csrf -> csrf.disable())

                // Không tạo HttpSession. Mỗi request tự mang theo danh tính
                // của nó — điều kiện để sau này chạy nhiều instance backend
                // mà không cần chia sẻ session.
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Tắt form đăng nhập và HTTP Basic mặc định của Spring Boot.
                // Ta trả JSON, không chuyển hướng người dùng sang trang
                // /login do Spring tự sinh — frontend là SPA, nó tự lo giao
                // diện đăng nhập.
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        // Preflight phải luôn qua, kể cả sau khi Sprint 1 khoá
                        // API: trình duyệt gửi OPTIONS mà KHÔNG kèm header
                        // Authorization, nên nếu bắt xác thực thì mọi lời gọi
                        // từ frontend đều chết ngay ở bước preflight.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // TẠM THỜI mở hết. Sprint 1 thay dòng này bằng:
                        //   .requestMatchers("/api/v1/auth/**").permitAll()
                        //   .requestMatchers("/actuator/health").permitAll()
                        //   .anyRequest().authenticated()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
