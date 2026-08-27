package vn.tntt.enrollment.dto;

import vn.tntt.enrollment.entity.GhiDanh;
import vn.tntt.enrollment.entity.TrangThaiGhiDanh;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Một lượt ghi danh trả ra cho frontend.
 *
 * <p>Trả kèm tên em, mã em và tên lớp: màn hình sĩ số lớp cần đúng ba thứ đó,
 * và truy vấn đã {@code JOIN FETCH} sẵn nên không tốn thêm câu SQL nào.
 */
public record GhiDanhResponse(
        UUID id,
        UUID thieuNhiId,
        String maThieuNhi,
        String tenThieuNhi,
        UUID lopId,
        String tenLop,
        UUID namHocId,
        TrangThaiGhiDanh trangThai,
        LocalDate ngayGhiDanh
) {
    public static GhiDanhResponse tu(GhiDanh g) {
        return new GhiDanhResponse(
                g.getId(),
                g.getThieuNhi().getId(),
                g.getThieuNhi().getMaThieuNhi(),
                g.getThieuNhi().tenDayDu(),
                g.getLopHoc().getId(),
                g.getLopHoc().getTenLop(),
                g.getNamHoc().getId(),
                g.getTrangThai(),
                g.getNgayGhiDanh());
    }
}
