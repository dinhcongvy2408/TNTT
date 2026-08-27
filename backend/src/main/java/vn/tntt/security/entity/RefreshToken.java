package vn.tntt.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.personnel.entity.NguoiDung;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một refresh token còn sống (hoặc đã bị thu hồi).
 *
 * <p>KHÔNG kế thừa {@code BaseEntity}: bảng này chỉ có {@code ngay_tao},
 * không có {@code ngay_cap_nhat} hay người tạo — nó không phải dữ liệu do
 * người dùng nhập, mà là dấu vết của một phiên đăng nhập.
 *
 * <p>Xem migration V5 để biết vì sao lưu bản băm chứ không lưu token gốc.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_dung_id", nullable = false)
    private NguoiDung nguoiDung;

    /** SHA-256 hex của token gốc. Token gốc chỉ tồn tại trong cookie. */
    @Column(name = "ma_bam", nullable = false, length = 64, unique = true)
    private String maBam;

    @Column(name = "het_han_luc", nullable = false)
    private OffsetDateTime hetHanLuc;

    /** null = còn hiệu lực. */
    @Column(name = "thu_hoi_luc")
    private OffsetDateTime thuHoiLuc;

    @Column(name = "dia_chi_ip", length = 45)
    private String diaChiIp;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private OffsetDateTime ngayTao = OffsetDateTime.now();

    /** Còn dùng được: chưa thu hồi VÀ chưa hết hạn. */
    public boolean conHieuLuc() {
        return thuHoiLuc == null && hetHanLuc.isAfter(OffsetDateTime.now());
    }
}
