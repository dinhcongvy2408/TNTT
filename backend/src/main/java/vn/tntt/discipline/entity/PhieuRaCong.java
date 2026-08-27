package vn.tntt.discipline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.enrollment.entity.GhiDanh;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.personnel.entity.NguoiDung;
import vn.tntt.student.entity.ThieuNhi;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Phiếu xin cho một em ra cổng về sớm (docs/02 mục 6.2).
 *
 * <p><b>KHÔNG kế thừa {@code BaseEntity} — có chủ đích, xem docs/99 mục B3.</b>
 * Ở mọi bảng khác, {@code nguoi_tao_id} và {@code ngay_tao} là cột kỹ thuật do
 * hệ thống tự điền và không ai nhìn tới. Ở đây chúng là <b>dữ liệu nghiệp vụ</b>:
 * người trực cổng đọc trên màn hình để biết "ai xin cho em này về, lúc mấy
 * giờ". Nhập chung với cột kỹ thuật là mất đi sự phân biệt đó.
 *
 * <p>Vì thế các field ở đây được đặt tay trong service chứ không do JPA
 * Auditing điền.
 */
@Entity
@Table(name = "phieu_ra_cong")
@Getter
@Setter
@NoArgsConstructor
public class PhieuRaCong {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thieu_nhi_id", nullable = false)
    private ThieuNhi thieuNhi;

    /**
     * Lượt ghi danh, để biết em thuộc lớp nào.
     *
     * <p>Cho phép NULL vì có trường hợp em chưa được ghi danh vào lớp nào mà
     * vẫn có mặt ở nhà thờ (mới chuyển đến, đang chờ xếp lớp). Khi đó màn hình
     * trực cổng hiện "chưa có lớp" thay vì từ chối tạo phiếu — người trực cổng
     * cần biết em được về, chuyện xếp lớp là việc khác.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ghi_danh_id")
    private GhiDanh ghiDanh;

    /**
     * Năm học. Cột này là bổ sung so với schema gốc (docs/99 mục B4): topic
     * WebSocket là {@code /topic/phieu-ra-cong/{namHocId}}, không có cột này
     * thì server không biết đẩy bản tin vào topic nào.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nam_hoc_id", nullable = false)
    private NamHoc namHoc;

    /** Giáo lý viên xin cho em về. Dữ liệu nghiệp vụ, hiện trên màn hình trực. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_tao_id", nullable = false)
    private NguoiDung nguoiTao;

    /** Người trực cổng bấm xác nhận. NULL cho tới lúc em thật sự ra về. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_xac_nhan_id")
    private NguoiDung nguoiXacNhan;

    @Column(name = "ly_do", nullable = false, columnDefinition = "text")
    private String lyDo;

    @Column(name = "thoi_gian_tao", nullable = false)
    private OffsetDateTime thoiGianTao = OffsetDateTime.now();

    @Column(name = "thoi_gian_ra_cong")
    private OffsetDateTime thoiGianRaCong;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiPhieu trangThai = TrangThaiPhieu.CHO_RA_CONG;

    public boolean dangCho() {
        return trangThai == TrangThaiPhieu.CHO_RA_CONG;
    }

    /** Tên lớp của em, hoặc null nếu em chưa được xếp lớp. */
    public String tenLop() {
        return ghiDanh == null ? null : ghiDanh.getLopHoc().getTenLop();
    }
}
