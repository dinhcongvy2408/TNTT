package vn.tntt.enrollment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.common.entity.BaseEntity;
import vn.tntt.organization.entity.LopHoc;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.student.entity.ThieuNhi;

import java.time.LocalDate;

/**
 * Một em học ở một lớp trong một năm học.
 *
 * <p>Đây là bảng bản lề của cả hệ thống: điểm danh và điểm số đều treo vào
 * {@code ghi_danh} chứ không treo thẳng vào {@code thieu_nhi}. Nhờ vậy một em
 * học 5 năm thì có 5 dòng ghi danh, mỗi dòng mang dữ liệu riêng của năm đó.
 *
 * <p><b>Vì sao có cả {@code namHoc} khi đã có {@code lopHoc}?</b> Cột
 * {@code nam_hoc_id} là dữ liệu LẶP có chủ đích — xem docs/99 mục B1. Không có
 * nó thì quy tắc "một em chỉ có một lớp đang học mỗi năm" không ép được bằng
 * ràng buộc DB, vì partial unique index đòi mọi cột phải cùng một bảng. Khoá
 * ngoại GHÉP {@code (lop_id, nam_hoc_id)} khoá chặt nguy cơ hai cột lệch nhau.
 */
@Entity
@Table(name = "ghi_danh")
@Getter
@Setter
@NoArgsConstructor
public class GhiDanh extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thieu_nhi_id", nullable = false)
    private ThieuNhi thieuNhi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lop_id", nullable = false)
    private LopHoc lopHoc;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nam_hoc_id", nullable = false)
    private NamHoc namHoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiGhiDanh trangThai = TrangThaiGhiDanh.DANG_HOC;

    @Column(name = "ngay_ghi_danh", nullable = false)
    private LocalDate ngayGhiDanh = LocalDate.now();

    public boolean dangHoc() {
        return trangThai == TrangThaiGhiDanh.DANG_HOC;
    }
}
