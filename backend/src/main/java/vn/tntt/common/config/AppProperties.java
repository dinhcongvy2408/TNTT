package vn.tntt.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Gom mọi cấu hình riêng của ứng dụng (tiền tố {@code app.*} trong
 * application.yml) vào một chỗ, có kiểu rõ ràng.
 *
 * <p>Vì sao không rải {@code @Value("${app.cors...}")} khắp nơi? Vì
 * {@code @Value} chỉ báo lỗi lúc CHẠY, khi request đầu tiên chạm tới. Còn
 * {@code @ConfigurationProperties} + {@code @Validated} báo lỗi ngay lúc
 * KHỞI ĐỘNG — thiếu biến môi trường thì app không lên, ta biết ngay thay vì
 * để nó gãy giữa buổi Chủ Nhật.
 *
 * @param corsAllowedOrigins danh sách origin của frontend được phép gọi API
 * @param jwt                cấu hình token, Sprint 1 mới dùng
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(

        @NotEmpty(message = "Phải khai báo ít nhất một origin cho CORS")
        List<String> corsAllowedOrigins,

        Jwt jwt
) {

    public record Jwt(
            /** Khoá ký token. Đọc từ biến môi trường, KHÔNG hardcode. */
            String secret,

            @Min(1) int accessTtlMinutes,

            @Min(1) int refreshTtlDays
    ) {
    }
}
