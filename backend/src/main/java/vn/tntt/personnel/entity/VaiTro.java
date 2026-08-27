package vn.tntt.personnel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.common.entity.BaseEntity;

/**
 * Vai trò: ADMIN, KHOI_TRUONG, HUYNH_TRUONG, KY_LUAT.
 *
 * <p>Dữ liệu gốc, đã seed ở migration V2. Ma trận phân quyền đầy đủ ở
 * docs/02 mục "Ma trận phân quyền".
 *
 * <p><b>Lưu ý về tiền tố {@code ROLE_}.</b> Spring Security quy ước
 * {@code hasRole('ADMIN')} sẽ đi tìm quyền tên {@code ROLE_ADMIN}. Ta lưu
 * trong DB là {@code ADMIN} (không tiền tố) cho ban điều hành đọc DB dễ hiểu,
 * rồi ghép tiền tố ở {@code TnttUserDetails}. Nhầm chỗ này là mọi
 * {@code @PreAuthorize} đều từ chối mà không hiểu vì sao.
 */
@Entity
@Table(name = "vai_tro")
@Getter
@Setter
@NoArgsConstructor
public class VaiTro extends BaseEntity {

    @Column(name = "ma", nullable = false, length = 30, unique = true)
    private String ma;

    @Column(name = "ten_hien_thi", nullable = false, length = 80)
    private String tenHienThi;

    @Column(name = "mo_ta", columnDefinition = "text")
    private String moTa;
}
