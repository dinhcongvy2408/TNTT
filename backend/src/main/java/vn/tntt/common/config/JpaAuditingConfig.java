package vn.tntt.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.tntt.security.service.NguoiDungDangDangNhap;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Bật cơ chế auditing của Spring Data JPA.
 *
 * <p>Nhờ nó mà {@code @CreatedDate} / {@code @LastModifiedBy} trong
 * {@code BaseEntity} tự được điền mỗi lần save — không service nào phải nhớ
 * set tay, và cũng không thể quên.
 */
@Configuration
@EnableJpaAuditing(
        auditorAwareRef = "auditorProvider",
        dateTimeProviderRef = "auditDateTimeProvider")
public class JpaAuditingConfig {

    /**
     * "Ai đang thao tác" — điền vào {@code nguoi_tao_id} và
     * {@code nguoi_cap_nhat_id}.
     *
     * <p>Sprint 1 đã nối vào Spring Security: đọc principal do
     * {@code JwtAuthenticationFilter} đặt vào {@code SecurityContextHolder}.
     *
     * <p><b>Vì sao vẫn phải trả được rỗng?</b> Có những đường ghi DB không đi
     * qua một request nào của người dùng: migration của Flyway, tác vụ chạy
     * định kỳ ở Sprint 8, và cả test. Lúc đó {@code SecurityContextHolder}
     * trống, và {@code nguoi_tao_id} để NULL — migration V1 đã cho phép cột
     * này NULL đúng vì lý do đó.
     *
     * <p>CLAUDE.md mục 6 yêu cầu ghi nhật ký mọi thao tác trên hồ sơ thiếu
     * nhi. Hai cột này là một nửa của yêu cầu đó; nửa còn lại là bảng
     * {@code nhat_ky_he_thong}, làm ở Sprint 4.
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            var xacThuc = SecurityContextHolder.getContext().getAuthentication();
            if (xacThuc == null || !xacThuc.isAuthenticated()) {
                return Optional.empty();
            }
            // Người dùng ẩn danh cũng có Authentication, nhưng principal của
            // nó là chuỗi "anonymousUser" chứ không phải record của ta.
            if (xacThuc.getPrincipal() instanceof NguoiDungDangDangNhap nguoiDung) {
                return Optional.of(nguoiDung.id());
            }
            return Optional.empty();
        };
    }

    /**
     * "Bây giờ là mấy giờ" — cho {@code @CreatedDate} và
     * {@code @LastModifiedDate}.
     *
     * <p><b>Vì sao phải tự khai báo?</b> Mặc định Spring Data dùng
     * {@code CurrentDateTimeProvider}, trả về {@link java.time.LocalDateTime}.
     * Còn {@code BaseEntity} khai hai cột thời gian là {@link OffsetDateTime}
     * (khớp {@code TIMESTAMPTZ} của PostgreSQL). Spring Data KHÔNG có đường
     * chuyển {@code LocalDateTime → OffsetDateTime} — hợp lý, vì
     * {@code LocalDateTime} không biết nó thuộc múi giờ nào.
     *
     * <p>Bug này nằm im suốt Sprint 0 vì chưa entity nào được lưu; nó chỉ lộ
     * ở lần {@code save()} đầu tiên, giữa Sprint 2. Bài học: một đoạn cấu
     * hình chưa từng chạy thì chưa được coi là đúng. Xem docs/99 mục F2.
     */
    @Bean
    public DateTimeProvider auditDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
