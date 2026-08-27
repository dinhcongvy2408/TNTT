package vn.tntt.enrollment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ResourceNotFoundException;
import vn.tntt.enrollment.dto.GhiDanhResponse;
import vn.tntt.enrollment.entity.GhiDanh;
import vn.tntt.enrollment.entity.TrangThaiGhiDanh;
import vn.tntt.enrollment.repository.GhiDanhRepository;
import vn.tntt.organization.entity.LopHoc;
import vn.tntt.organization.repository.LopHocRepository;
import vn.tntt.student.entity.ThieuNhi;
import vn.tntt.student.repository.ThieuNhiRepository;

import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ ghi danh: đưa một em vào một lớp của một năm học.
 *
 * <p><b>Phạm vi lát cắt này.</b> Đủ để phiếu ra cổng biết em thuộc lớp nào.
 * CHƯA có điểm danh — đó là phần còn lại của Sprint 5. docs/99 mục H2.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GhiDanhService {

    private final GhiDanhRepository ghiDanhRepository;
    private final ThieuNhiRepository thieuNhiRepository;
    private final LopHocRepository lopHocRepository;

    @Transactional(readOnly = true)
    public List<GhiDanhResponse> danhSachLop(UUID lopId) {
        return ghiDanhRepository.danhSachLop(lopId).stream().map(GhiDanhResponse::tu).toList();
    }

    /**
     * Ghi danh một em vào một lớp.
     *
     * <p>Ba hàng rào, theo thứ tự rẻ tiền trước:
     * <ol>
     *   <li>Năm học của lớp phải chưa kết thúc — không thêm học sinh vào một
     *       năm đã đóng sổ.</li>
     *   <li>Em chưa có lượt ghi danh DANG_HOC nào trong năm đó (docs/02 mục
     *       3.4). Chốt chặn thật là partial unique index
     *       {@code uq_ghi_danh_dang_hoc}; kiểm ở đây chỉ để có câu thông báo
     *       tử tế.</li>
     *   <li>{@code nam_hoc_id} LUÔN lấy từ lớp, không nhận từ client. Khoá
     *       ngoại ghép {@code (lop_id, nam_hoc_id)} sẽ từ chối nếu hai cột
     *       lệch nhau, nhưng lấy đúng từ đầu thì không bao giờ chạm tới nó.</li>
     * </ol>
     */
    @Transactional
    public GhiDanhResponse ghiDanh(UUID thieuNhiId, UUID lopId) {
        ThieuNhi thieuNhi = thieuNhiRepository.timChuaXoa(thieuNhiId)
                .orElseThrow(() -> ResourceNotFoundException.of("Thiếu nhi", thieuNhiId));

        LopHoc lopHoc = lopHocRepository.timTheoIdKemQuanHe(lopId)
                .orElseThrow(() -> ResourceNotFoundException.of("Lớp học", lopId));

        if (lopHoc.getNamHoc().daKetThuc()) {
            throw new BusinessRuleException(
                    "Năm học %s đã kết thúc, không ghi danh thêm được"
                            .formatted(lopHoc.getNamHoc().getTenNamHoc()),
                    "NAM_HOC_DA_KET_THUC");
        }

        UUID namHocId = lopHoc.getNamHoc().getId();
        if (ghiDanhRepository.existsByThieuNhiIdAndNamHocIdAndTrangThai(
                thieuNhiId, namHocId, TrangThaiGhiDanh.DANG_HOC)) {
            throw new BusinessRuleException(
                    "Em %s đã có lớp đang học trong năm %s. Hãy chuyển lớp thay vì ghi danh mới."
                            .formatted(thieuNhi.tenDayDu(), lopHoc.getNamHoc().getTenNamHoc()),
                    "DA_CO_LOP_DANG_HOC");
        }

        GhiDanh ghiDanh = new GhiDanh();
        ghiDanh.setThieuNhi(thieuNhi);
        ghiDanh.setLopHoc(lopHoc);
        // Lấy từ lớp, KHÔNG nhận từ client — xem javadoc trên.
        ghiDanh.setNamHoc(lopHoc.getNamHoc());
        ghiDanh.setTrangThai(TrangThaiGhiDanh.DANG_HOC);

        GhiDanh daLuu = ghiDanhRepository.save(ghiDanh);
        log.info("Ghi danh {} vào lớp {}", thieuNhi.getMaThieuNhi(), lopHoc.getTenLop());
        return GhiDanhResponse.tu(daLuu);
    }

    /**
     * Đổi trạng thái một lượt ghi danh (nghỉ học, chuyển xứ, hoàn thành).
     *
     * <p>Không cho quay lại DANG_HOC: em đã nghỉ rồi quay lại thì đó là một
     * lượt ghi danh MỚI, không phải sửa lượt cũ. Giữ nguyên lịch sử thì mới
     * trả lời được câu "em này nghỉ từ lúc nào".
     */
    @Transactional
    public GhiDanhResponse doiTrangThai(UUID ghiDanhId, TrangThaiGhiDanh trangThaiMoi) {
        GhiDanh ghiDanh = ghiDanhRepository.findById(ghiDanhId)
                .orElseThrow(() -> ResourceNotFoundException.of("Ghi danh", ghiDanhId));

        if (trangThaiMoi == TrangThaiGhiDanh.DANG_HOC) {
            throw new BusinessRuleException(
                    "Không đưa lại về Đang học được. Hãy ghi danh mới cho em.",
                    "KHONG_QUAY_LAI_DANG_HOC");
        }
        if (ghiDanh.getNamHoc().daKetThuc()) {
            throw new BusinessRuleException(
                    "Năm học đã kết thúc, dữ liệu chỉ đọc", "NAM_HOC_DA_KET_THUC");
        }

        ghiDanh.setTrangThai(trangThaiMoi);
        return GhiDanhResponse.tu(ghiDanh);
    }
}
