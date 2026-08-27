package vn.tntt.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Bật cơ chế auditing của Spring Data JPA.
 *
 * <p>Nhờ nó mà các field {@code @CreatedDate} / {@code @LastModifiedBy} trong
 * {@code BaseEntity} tự được điền mỗi lần save — ta không phải nhớ set tay ở
 * từng service, và cũng không thể quên.
 */
@Configuration
@EnableJpaAuditing(
        auditorAwareRef = "auditorProvider",
        dateTimeProviderRef = "auditDateTimeProvider")
public class JpaAuditingConfig {

    /**
     * "Ai đang thao tác".
     *
     * <p>Ở Sprint 0 chưa có đăng nhập nên luôn trả rỗng — khi đó
     * {@code nguoi_tao_id} để NULL, đúng như migration cho phép. Sprint 1 sẽ
     * đổi thân hàm này để đọc user id từ {@code SecurityContextHolder},
     * chỉ sửa đúng một chỗ này.
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return Optional::empty;
    }

    /**
     * "Bây giờ là mấy giờ" — dành cho {@code @CreatedDate} và
     * {@code @LastModifiedDate}.
     *
     * <p><b>Vì sao phải tự khai báo bean này?</b> Mặc định Spring Data dùng
     * {@code CurrentDateTimeProvider}, thứ trả về {@link java.time.LocalDateTime}.
     * Còn {@code BaseEntity} khai báo hai cột thời gian là
     * {@link OffsetDateTime} (để khớp kiểu {@code TIMESTAMPTZ} của PostgreSQL,
     * tức là có mang theo múi giờ). Spring Data KHÔNG có đường chuyển
     * {@code LocalDateTime → OffsetDateTime} — hợp lý, vì {@code LocalDateTime}
     * không biết nó thuộc múi giờ nào, đoán bừa là sai. Nó ném:
     *
     * <pre>
     * Cannot convert unsupported date type java.time.LocalDateTime
     * to java.time.OffsetDateTime
     * </pre>
     *
     * <p>Bean này cắt bỏ khâu chuyển đổi: nó trả thẳng đúng kiểu mà entity
     * cần, nên Spring Data dùng nguyên giá trị.
     *
     * <p><b>Bug này nằm im suốt Sprint 0</b> vì lúc đó chưa có entity nào được
     * lưu xuống DB. Nó chỉ lộ ra ở lần {@code save()} đầu tiên — bài học: một
     * đoạn cấu hình chưa từng chạy thì chưa được coi là đúng.
     */
    @Bean
    public DateTimeProvider auditDateTimeProvider() {
        // OffsetDateTime.now() lấy giờ và offset của máy chủ. Cột TIMESTAMPTZ
        // chỉ lưu MỐC thời gian tuyệt đối (PostgreSQL quy về UTC), nên offset
        // của máy chủ không làm sai lệch dữ liệu.
        return () -> Optional.of(OffsetDateTime.now());
    }
}
