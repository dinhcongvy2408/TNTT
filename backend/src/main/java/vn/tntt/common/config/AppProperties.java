package vn.tntt.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Gom mọi cấu hình riêng của ứng dụng (tiền tố {@code app.*}) vào một chỗ,
 * có kiểu rõ ràng.
 *
 * <p>Vì sao không rải {@code @Value} khắp nơi? Vì {@code @Value} chỉ báo lỗi
 * lúc CHẠY, khi request đầu tiên chạm tới. Còn
 * {@code @ConfigurationProperties + @Validated} báo lỗi ngay lúc KHỞI ĐỘNG —
 * thiếu biến môi trường thì app không lên, ta biết ngay thay vì để nó gãy
 * giữa buổi Chủ Nhật.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(

        @NotEmpty(message = "Phải khai báo ít nhất một origin cho CORS")
        List<String> corsAllowedOrigins,

        // @Valid là BẮT BUỘC ở đây. Bean Validation KHÔNG tự đi vào object
        // lồng nhau — thiếu nó thì mọi ràng buộc bên trong record Jwt chỉ là
        // trang trí, không bao giờ chạy. Đây là lỗi im lặng: không có thông
        // báo nào cho biết ràng buộc đã bị bỏ qua.
        @Valid
        Jwt jwt
) {

    public record Jwt(
            /**
             * Khoá ký token. Đọc từ biến môi trường, KHÔNG hardcode.
             *
             * <p>Tối thiểu 64 ký tự vì thuật toán HMAC-SHA256 đòi khoá ít
             * nhất 256 bit. Khoá ngắn hơn thì thư viện jjwt từ chối làm việc —
             * nhưng nó từ chối lúc có người đăng nhập, còn ràng buộc ở đây
             * chặn ngay lúc khởi động, khi ta còn đang nhìn màn hình.
             *
             * <p>Sinh khoá: {@code openssl rand -base64 64}
             */
            @NotBlank(message = "Thiếu app.jwt.secret (biến môi trường APP_JWT_SECRET)")
            @Size(min = 64, message = "app.jwt.secret phải dài ít nhất 64 ký tự")
            String secret,

            @Min(1) int accessTtlMinutes,

            @Min(1) int refreshTtlDays
    ) {
    }
}
