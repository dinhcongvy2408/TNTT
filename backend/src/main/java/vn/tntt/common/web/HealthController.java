package vn.tntt.common.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.tntt.common.response.ApiResponse;

import java.time.OffsetDateTime;

/**
 * Endpoint kiểm tra sức khoẻ hệ thống — mục tiêu của Sprint 0.
 *
 * <p>Vì sao tự viết mà không chỉ dùng {@code /actuator/health} có sẵn?
 * Hai cái phục vụ hai đối tượng khác nhau:
 * <ul>
 *   <li>{@code /actuator/health} — cho MÁY đọc (Docker healthcheck, Nginx,
 *       uptime monitor). Định dạng do Spring quy định, ta không đổi.</li>
 *   <li>{@code /api/v1/health} — cho NGƯỜI và cho frontend, trả đúng vỏ
 *       {@link ApiResponse} như mọi endpoint khác, để ta kiểm chứng luôn
 *       rằng chuỗi React → CORS → Controller → DB đã thông.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Kiểm tra trạng thái hệ thống")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name}")
    private String tenUngDung;

    @Value("${spring.profiles.active:default}")
    private String profile;

    /** Dữ liệu trả về. Record lồng trong controller vì không ai khác dùng tới. */
    public record HealthInfo(
            String ungDung,
            String profile,
            String trangThai,
            String database,
            OffsetDateTime thoiGianMayChu
    ) {
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Trạng thái backend và kết nối database")
    public ApiResponse<HealthInfo> health() {
        return ApiResponse.ok(new HealthInfo(
                tenUngDung,
                profile,
                "UP",
                kiemTraDatabase(),
                OffsetDateTime.now()
        ));
    }

    /**
     * Ping database bằng câu truy vấn rẻ nhất có thể.
     *
     * <p>Ta nuốt exception thay vì để nó nổ ra 500: mục đích của endpoint này
     * là BÁO CÁO tình trạng, nên khi DB chết nó vẫn phải trả lời được
     * "app sống, DB chết" — chứ không phải im lặng luôn.
     */
    private String kiemTraDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception ex) {
            log.error("Không kết nối được database", ex);
            return "DOWN";
        }
    }
}
