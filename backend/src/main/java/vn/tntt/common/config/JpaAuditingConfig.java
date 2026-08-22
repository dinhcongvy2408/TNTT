package vn.tntt.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

/**
 * Bật cơ chế auditing của Spring Data JPA.
 *
 * <p>Nhờ nó mà các field {@code @CreatedDate} / {@code @LastModifiedBy} trong
 * {@code BaseEntity} tự được điền mỗi lần save — ta không phải nhớ set tay ở
 * từng service, và cũng không thể quên.
 *
 * <p>{@code auditorProvider} trả về "ai đang thao tác". Ở Sprint 0 chưa có
 * đăng nhập nên luôn trả rỗng. Sprint 1 sẽ đổi thân hàm này để đọc user id
 * từ {@code SecurityContextHolder} — chỉ sửa đúng một chỗ này.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        // Sprint 1 thay bằng:
        //   SecurityContextHolder.getContext().getAuthentication() -> lấy id user
        return Optional::empty;
    }
}
