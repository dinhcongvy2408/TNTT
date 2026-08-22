package vn.tntt.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Cấu hình Swagger UI (springdoc-openapi).
 *
 * <p>{@code @Profile("dev")} — chỉ nạp bean này ở môi trường dev.
 * docs/04-api.md yêu cầu TẮT ở production, và có lý do: trang Swagger phơi
 * bày toàn bộ danh sách endpoint, tên field, cấu trúc dữ liệu. Với hệ thống
 * chứa hồ sơ trẻ em thì đó là bản đồ tấn công miễn phí cho người lạ.
 *
 * <p>Việc CHẶN đường dẫn {@code /swagger-ui/**} ở prod nằm ở application-prod.yml
 * ({@code springdoc.api-docs.enabled: false}) — bean này chỉ lo phần mô tả.
 */
@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI tnttOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API Quản lý Xứ đoàn Thiếu Nhi Thánh Thể")
                .version("v1")
                .description("""
                        Tài liệu API tự sinh. Xem đặc tả nghiệp vụ ở docs/04-api.md.

                        Quy ước response:
                        { "success": true, "data": ..., "message": null }
                        """)
                .contact(new Contact().name("Đinh Công Vỹ"))
                .license(new License().name("Phi lợi nhuận — nội bộ giáo xứ")));
    }
}
