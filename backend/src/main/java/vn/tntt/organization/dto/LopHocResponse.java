package vn.tntt.organization.dto;

import vn.tntt.organization.entity.LopHoc;

import java.util.UUID;

/**
 * Lớp học trả ra cho frontend.
 *
 * <p>Trả kèm TÊN ngành và TÊN năm học chứ không chỉ id. Nếu chỉ trả id thì
 * frontend phải gọi thêm hai API nữa rồi tự ghép — vừa chậm trên mạng 3G ở
 * nhà thờ, vừa đẩy logic ghép dữ liệu sang chỗ khó test hơn.
 *
 * <p>Đọc được {@code lopHoc.getNganh().getTenNganh()} ở đây là nhờ truy vấn
 * đã {@code JOIN FETCH} sẵn. Nếu quên JOIN FETCH, dòng này chính là chỗ ném
 * {@code LazyInitializationException} — hoặc tệ hơn, âm thầm bắn thêm một câu
 * truy vấn cho mỗi lớp.
 */
public record LopHocResponse(
        UUID id,
        String tenLop,
        Short capDo,
        String ghiChu,
        UUID nganhId,
        String tenNganh,
        UUID namHocId,
        String tenNamHoc
) {
    public static LopHocResponse tu(LopHoc lopHoc) {
        return new LopHocResponse(
                lopHoc.getId(),
                lopHoc.getTenLop(),
                lopHoc.getCapDo(),
                lopHoc.getGhiChu(),
                lopHoc.getNganh().getId(),
                lopHoc.getNganh().getTenNganh(),
                lopHoc.getNamHoc().getId(),
                lopHoc.getNamHoc().getTenNamHoc()
        );
    }
}
