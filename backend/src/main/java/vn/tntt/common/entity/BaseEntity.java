package vn.tntt.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lớp cha cho mọi entity, gom 5 cột lặp lại ở tất cả bảng
 * (theo docs/03-du-lieu.md mục 9).
 *
 * <p>{@code @MappedSuperclass} nghĩa là: Hibernate KHÔNG tạo bảng cho lớp này,
 * mà "dán" các cột của nó vào bảng của từng entity con. Khác với
 * {@code @Inheritance} — cái đó mới sinh bảng riêng.
 *
 * <p>{@code @EntityListeners(AuditingEntityListener.class)} là thứ khiến
 * {@code @CreatedDate} / {@code @LastModifiedBy} tự điền. Nó chỉ chạy khi
 * {@code @EnableJpaAuditing} được bật — xem {@code JpaAuditingConfig}.
 *
 * <p><b>Lưu ý về id:</b> tạm dùng {@code @GeneratedValue} (Hibernate sinh UUID
 * v4, khớp với {@code gen_random_uuid()} trong migration). docs/03 khuyến nghị
 * UUID v7 để index bớt phân mảnh — sẽ đổi sang generator riêng ở Sprint 4 khi
 * bảng {@code thieu_nhi} bắt đầu có vài nghìn dòng, lúc đó mới đo được khác biệt.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "ngay_tao", updatable = false, nullable = false)
    private OffsetDateTime ngayTao;

    @LastModifiedDate
    @Column(name = "ngay_cap_nhat", nullable = false)
    private OffsetDateTime ngayCapNhat;

    /** Id người tạo. Null với dữ liệu do migration seed sẵn. */
    @CreatedBy
    @Column(name = "nguoi_tao_id", updatable = false)
    private UUID nguoiTaoId;

    @LastModifiedBy
    @Column(name = "nguoi_cap_nhat_id")
    private UUID nguoiCapNhatId;

    /**
     * So sánh entity theo id chứ không theo tham chiếu.
     *
     * <p>Cần thiết vì Hibernate hay trả về đối tượng proxy: cùng một bản ghi
     * nhưng lấy ở hai chỗ khác nhau có thể là hai instance khác nhau. Nếu để
     * equals mặc định thì {@code Set<LopHoc>} sẽ chứa trùng.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    /**
     * Hằng số, KHÔNG dùng {@code Objects.hash(id)}.
     *
     * <p>Lý do: entity mới chưa có id (null), sau khi save mới được gán id.
     * Nếu hashCode phụ thuộc id thì đối tượng sẽ "biến mất" khỏi HashSet
     * ngay sau khi lưu. Đây là bẫy kinh điển khi dùng JPA.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
