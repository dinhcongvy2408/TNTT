package vn.tntt.common.config;

import jakarta.validation.Valid;
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

        // @Valid là BẮT BUỘC ở đây. Bean Validation KHÔNG tự đi vào object
        // lồng nhau — thiếu nó thì mọi @Min bên trong record Jwt chỉ là
        // trang trí, không bao giờ chạy. Đây là lỗi im lặng: không có thông
        // báo nào cho biết ràng buộc đã bị bỏ qua.
        @Valid
        Jwt jwt
) {

    public record Jwt(
            /**
             * Khoá ký token. Đọc từ biến môi trường, KHÔNG hardcode.
             *
             * <p>Sprint 1 phải thêm {@code @NotBlank} và ràng buộc độ dài
             * tối thiểu 64 ký tự. Hiện application.yml để mặc định là chuỗi
             * rỗng — ký JWT bằng khoá rỗng sẽ nổ lúc CHẠY, đúng thứ mà cả
             * lớp này sinh ra để tránh.
             */
            String secret,

            @Min(1) int accessTtlMinutes,

            @Min(1) int refreshTtlDays
    ) {
    }
}
