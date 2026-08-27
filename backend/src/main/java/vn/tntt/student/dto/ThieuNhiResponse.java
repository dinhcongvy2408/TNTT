package vn.tntt.student.dto;

import vn.tntt.student.entity.ThieuNhi;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Hồ sơ thiếu nhi trả ra cho frontend.
 *
 * <p>Có {@code tuoi} tính sẵn: ngành được xếp theo tuổi (docs/02 bước 1), nên
 * màn hình nào cũng cần con số này. Tính ở server thì mọi client cho ra cùng
 * một kết quả, còn tính ở frontend thì phụ thuộc đồng hồ của điện thoại.
 */
public record ThieuNhiResponse(
        UUID id,
        String maThieuNhi,
        String tenThanh,
        String hoTen,
        String tenDayDu,
        LocalDate ngaySinh,
        int tuoi,
        String gioiTinh,
        String tenBo,
        String tenMe,
        String sdtPhuHuynh,
        String diaChi,
        String giaoHo,
        String ghiChu
) {
    public static ThieuNhiResponse tu(ThieuNhi tn) {
        return new ThieuNhiResponse(
                tn.getId(),
                tn.getMaThieuNhi(),
                tn.getTenThanh(),
                tn.getHoTen(),
                tn.tenDayDu(),
                tn.getNgaySinh(),
                Period.between(tn.getNgaySinh(), LocalDate.now()).getYears(),
                tn.getGioiTinh(),
                tn.getTenBo(),
                tn.getTenMe(),
                tn.getSdtPhuHuynh(),
                tn.getDiaChi(),
                tn.getGiaoHo(),
                tn.getGhiChu());
    }
}
