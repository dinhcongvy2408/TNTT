package vn.tntt.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.common.entity.BaseEntity;

import java.time.LocalDate;

/**
 * Năm học — trục xoay của toàn hệ thống.
 *
 * <p>docs/02 bước 1: "Không có gì hoạt động được nếu hệ thống chưa biết đang
 * ở năm học nào". Ghi danh, điểm danh, điểm số, lịch trực đều gắn với một
 * năm học, nên đây là entity đầu tiên phải có.
 *
 * <p>Năm cột kỹ thuật (id, ngày tạo, ngày cập nhật, người tạo, người cập
 * nhật) đến từ {@link BaseEntity} — xem lớp đó để biết vì sao.
 */
@Entity
@Table(name = "nam_hoc")
@Getter
@Setter
@NoArgsConstructor
public class NamHoc extends BaseEntity {

    /** Dạng "2026-2027". Duy nhất toàn bảng. */
    @Column(name = "ten_nam_hoc", nullable = false, length = 20, unique = true)
    private String tenNamHoc;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDate ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDate ngayKetThuc;

    /**
     * {@code EnumType.STRING} chứ TUYỆT ĐỐI không phải {@code ORDINAL}.
     *
     * <p>ORDINAL lưu thứ tự khai báo dưới dạng số: 0, 1, 2. Ngày nào đó ta
     * chèn một hằng số mới vào GIỮA enum, mọi dòng đã có trong DB lập tức bị
     * hiểu sang nghĩa khác — không lỗi, không cảnh báo, chỉ là dữ liệu sai
     * lặng lẽ. STRING lưu đúng chữ 'DANG_HOAT_DONG', đọc DB bằng mắt cũng
     * hiểu, và khớp với ràng buộc CHECK của PostgreSQL.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private TrangThaiNamHoc trangThai = TrangThaiNamHoc.CHUAN_BI;

    public NamHoc(String tenNamHoc, LocalDate ngayBatDau, LocalDate ngayKetThuc) {
        this.tenNamHoc = tenNamHoc;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.trangThai = TrangThaiNamHoc.CHUAN_BI;
    }

    /** Năm học đã đóng sổ thì mọi dữ liệu thuộc về nó là chỉ đọc. */
    public boolean daKetThuc() {
        return trangThai == TrangThaiNamHoc.DA_KET_THUC;
    }
}
