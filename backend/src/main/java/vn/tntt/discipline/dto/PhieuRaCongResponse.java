package vn.tntt.discipline.dto;

import vn.tntt.discipline.entity.PhieuRaCong;
import vn.tntt.discipline.entity.TrangThaiPhieu;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một phiếu ra cổng trả ra cho frontend VÀ đẩy qua WebSocket.
 *
 * <p>Dùng chung một record cho cả hai đường là có chủ đích: màn hình trực cổng
 * nạp danh sách ban đầu bằng HTTP rồi nhận cập nhật qua WebSocket. Hai đường
 * mà hai định dạng thì frontend phải viết hai bộ code xử lý, và chúng sẽ lệch
 * nhau sau vài lần sửa.
 *
 * <p>Khớp payload ở docs/04 mục WebSocket.
 */
public record PhieuRaCongResponse(
        UUID id,
        UUID thieuNhiId,
        String maThieuNhi,
        String tenThanh,
        String hoTen,
        /** null nếu em chưa được xếp lớp — màn hình trực hiện "chưa có lớp". */
        String tenLop,
        String lyDo,
        String nguoiTao,
        String nguoiXacNhan,
        OffsetDateTime thoiGianTao,
        OffsetDateTime thoiGianRaCong,
        TrangThaiPhieu trangThai
) {
    public static PhieuRaCongResponse tu(PhieuRaCong p) {
        return new PhieuRaCongResponse(
                p.getId(),
                p.getThieuNhi().getId(),
                p.getThieuNhi().getMaThieuNhi(),
                p.getThieuNhi().getTenThanh(),
                p.getThieuNhi().getHoTen(),
                p.tenLop(),
                p.getLyDo(),
                p.getNguoiTao().tenDayDu(),
                p.getNguoiXacNhan() == null ? null : p.getNguoiXacNhan().tenDayDu(),
                p.getThoiGianTao(),
                p.getThoiGianRaCong(),
                p.getTrangThai());
        // KHÔNG đưa số điện thoại phụ huynh hay ngày sinh vào đây. Payload này
        // được phát cho MỌI người đang mở màn hình trực cổng — chỉ nên chứa
        // đúng thứ người trực cần để nhận ra em và cho ra về.
    }
}
