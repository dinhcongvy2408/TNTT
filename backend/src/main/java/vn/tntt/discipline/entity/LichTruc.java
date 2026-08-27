package vn.tntt.discipline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.common.entity.BaseEntity;
import vn.tntt.organization.entity.NamHoc;

import java.time.LocalDate;

/**
 * Một ca trực: tổ nào, ngày nào, ca nào (docs/02 mục 6.1).
 *
 * <p>Ràng buộc {@code uq_lich_truc (ngay_truc, ca_truc, to_truc_id)} chặn việc
 * xếp trùng cùng một tổ vào cùng một ca. Nó KHÔNG chặn hai tổ khác nhau cùng
 * trực một ca — và đó là cố ý: ngày lễ lớn có thể cần hai tổ cùng đứng cổng.
 */
@Entity
@Table(name = "lich_truc")
@Getter
@Setter
@NoArgsConstructor
public class LichTruc extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_truc_id", nullable = false)
    private ToTruc toTruc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nam_hoc_id", nullable = false)
    private NamHoc namHoc;

    @Column(name = "ngay_truc", nullable = false)
    private LocalDate ngayTruc;

    /** "Thánh lễ thiếu nhi 7h30". */
    @Column(name = "ca_truc", nullable = false, length = 80)
    private String caTruc;

    @Column(name = "ghi_chu", columnDefinition = "text")
    private String ghiChu;
}
