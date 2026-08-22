package vn.tntt.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình tầng web: CORS.
 *
 * <p><b>CORS là gì và vì sao cần?</b> Frontend chạy ở
 * {@code http://localhost:5173}, backend ở {@code http://localhost:8080} —
 * khác cổng nghĩa là khác "origin". Trình duyệt mặc định CHẶN JavaScript ở
 * origin này gọi sang origin khác. Backend phải chủ động trả header
 * {@code Access-Control-Allow-Origin} thì trình duyệt mới cho qua.
 *
 * <p><b>Vì sao dùng danh sách cụ thể chứ không phải {@code *}?</b> Ta bật
 * {@code allowCredentials(true)} để gửi được refresh-token cookie ở Sprint 1.
 * Chuẩn CORS cấm kết hợp {@code *} với credentials, và kể cả cho phép thì
 * cũng không nên: {@code *} nghĩa là bất kỳ website nào cũng gọi được API
 * kèm cookie của người dùng đang đăng nhập.
 *
 * <p><b>Lưu ý cho Sprint 8:</b> khi frontend lên Vercel còn backend ở VPS,
 * hai bên khác site thật sự → cookie refresh token bắt buộc phải có
 * {@code SameSite=None; Secure}, và cả hai phải chạy HTTPS.
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(appProperties.corsAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                // Cache kết quả preflight OPTIONS 1 tiếng, đỡ một round-trip
                // cho mỗi request — đáng kể với mạng 3G ở nhà thờ.
                .maxAge(3600);
    }
}
