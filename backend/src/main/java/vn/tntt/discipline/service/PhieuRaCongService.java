package vn.tntt.discipline.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import vn.tntt.common.exception.AccessDeniedBusinessException;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ResourceNotFoundException;
import vn.tntt.discipline.dto.PhieuRaCongResponse;
import vn.tntt.discipline.dto.TaoPhieuRequest;
import vn.tntt.discipline.entity.PhieuRaCong;
import vn.tntt.discipline.entity.TrangThaiPhieu;
import vn.tntt.discipline.repository.PhieuRaCongRepository;
import vn.tntt.discipline.websocket.PhieuRaCongPublisher;
import vn.tntt.enrollment.repository.GhiDanhRepository;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.organization.entity.TrangThaiNamHoc;
import vn.tntt.organization.repository.NamHocRepository;
import vn.tntt.personnel.entity.NguoiDung;
import vn.tntt.personnel.repository.NguoiDungRepository;
import vn.tntt.student.entity.ThieuNhi;
import vn.tntt.student.repository.ThieuNhiRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ phiếu ra cổng — trái tim của Sprint 7 (docs/02 mục 6.2).
 *
 * <pre>
 * [Giáo lý viên]          [Server]              [Người trực cổng]
 *   tạo phiếu ────────────▶ lưu CHO_RA_CONG
 *                             └── WebSocket ──────▶ 🔔 chuông kêu
 *                                                  phụ huynh đến đón
 *                          ghi DA_RA_CONG ◀──────── bấm Xác nhận
 *                             └── WebSocket ──────▶ xoá khỏi danh sách chờ
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhieuRaCongService {

    private final PhieuRaCongRepository phieuRepository;
    private final ThieuNhiRepository thieuNhiRepository;
    private final GhiDanhRepository ghiDanhRepository;
    private final NamHocRepository namHocRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final PhieuRaCongPublisher publisher;

    // -----------------------------------------------------------------
    // Đọc
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PhieuRaCongResponse> dangCho() {
        NamHoc namHoc = namHocDangChay();
        return phieuRepository.dangCho(namHoc.getId()).stream()
                .map(PhieuRaCongResponse::tu).toList();
    }

    /**
     * Lịch sử phiếu trong một ngày.
     *
     * <p>Nhận {@link LocalDate} rồi tự đổi sang khoảng thời điểm đầu và cuối
     * ngày theo múi giờ Việt Nam. Không để client tự tính hai mốc đó: điện
     * thoại đặt sai múi giờ sẽ hỏi nhầm ngày, và ta sẽ mất cả buổi tìm xem vì
     * sao "không có phiếu nào hôm nay".
     */
    @Transactional(readOnly = true)
    public List<PhieuRaCongResponse> lichSu(LocalDate ngay) {
        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");
        OffsetDateTime tuLuc = ngay.atStartOfDay(vn).toOffsetDateTime();
        OffsetDateTime denLuc = ngay.atTime(LocalTime.MAX).atZone(vn).toOffsetDateTime();
        return phieuRepository.lichSuTrongNgay(tuLuc, denLuc).stream()
                .map(PhieuRaCongResponse::tu).toList();
    }

    // -----------------------------------------------------------------
    // Ghi
    // -----------------------------------------------------------------

    /**
     * Giáo lý viên tạo phiếu xin cho em về sớm.
     *
     * <p>{@code ghiDanh} có thể null: em mới chuyển đến, chưa xếp lớp, vẫn
     * phải cho về được. Màn hình trực cổng khi đó hiện "chưa có lớp".
     */
    @Transactional
    public PhieuRaCongResponse taoPhieu(TaoPhieuRequest request, UUID nguoiTaoId) {
        NamHoc namHoc = namHocDangChay();

        ThieuNhi thieuNhi = thieuNhiRepository.timChuaXoa(request.thieuNhiId())
                .orElseThrow(() -> ResourceNotFoundException.of("Thiếu nhi", request.thieuNhiId()));

        if (phieuRepository.existsByThieuNhiIdAndTrangThai(
                thieuNhi.getId(), TrangThaiPhieu.CHO_RA_CONG)) {
            throw new BusinessRuleException(
                    "Em %s đang có một phiếu chờ ra cổng".formatted(thieuNhi.tenDayDu()),
                    "PHIEU_DANG_CHO_TON_TAI");
        }

        NguoiDung nguoiTao = nguoiDungRepository.findById(nguoiTaoId)
                .orElseThrow(() -> ResourceNotFoundException.of("Người dùng", nguoiTaoId));

        PhieuRaCong phieu = new PhieuRaCong();
        phieu.setThieuNhi(thieuNhi);
        phieu.setNamHoc(namHoc);
        phieu.setNguoiTao(nguoiTao);
        phieu.setLyDo(request.lyDo().trim());
        phieu.setTrangThai(TrangThaiPhieu.CHO_RA_CONG);
        // Lượt ghi danh đang học, nếu có. orElse(null) chứ không ném lỗi.
        phieu.setGhiDanh(ghiDanhRepository
                .dangHocTrongNam(thieuNhi.getId(), namHoc.getId()).orElse(null));

        PhieuRaCong daLuu = phieuRepository.save(phieu);
        log.info("Tạo phiếu ra cổng cho {} bởi {}",
                thieuNhi.getMaThieuNhi(), nguoiTaoId);

        dayTinSauKhiCommit(namHoc.getId(), "PHIEU_MOI", daLuu);
        return PhieuRaCongResponse.tu(daLuu);
    }

    /** Người trực cổng xác nhận em đã ra về. */
    @Transactional
    public PhieuRaCongResponse xacNhan(UUID phieuId, UUID nguoiXacNhanId) {
        PhieuRaCong phieu = timPhieu(phieuId);

        if (!phieu.dangCho()) {
            throw new BusinessRuleException(
                    "Phiếu này đã được xử lý rồi", "PHIEU_DA_XU_LY");
        }

        NguoiDung nguoiXacNhan = nguoiDungRepository.findById(nguoiXacNhanId)
                .orElseThrow(() -> ResourceNotFoundException.of("Người dùng", nguoiXacNhanId));

        // Đặt CẢ BA cùng lúc. Ràng buộc ck_phieu_xac_nhan ở DB đòi đúng như
        // vậy: DA_RA_CONG thì bắt buộc có thời gian và người xác nhận. Thiếu
        // một cái là PostgreSQL từ chối cả câu UPDATE.
        phieu.setTrangThai(TrangThaiPhieu.DA_RA_CONG);
        phieu.setThoiGianRaCong(OffsetDateTime.now());
        phieu.setNguoiXacNhan(nguoiXacNhan);

        log.info("Xác nhận ra cổng: phiếu {}", phieuId);
        dayTinSauKhiCommit(phieu.getNamHoc().getId(), "DA_XAC_NHAN", phieu);
        return PhieuRaCongResponse.tu(phieu);
    }

    /**
     * Huỷ phiếu.
     *
     * <p>docs/04 quy định quyền là "người tạo, ADMIN". Kiểm tra theo DỮ LIỆU
     * như thế này {@code @PreAuthorize} không làm được — nó chỉ biết vai trò
     * chứ không biết bản ghi. Đây đúng là chỗ dùng
     * {@link AccessDeniedBusinessException} (xem javadoc lớp đó).
     */
    @Transactional
    public PhieuRaCongResponse huy(UUID phieuId, UUID nguoiHuyId, boolean laAdmin) {
        PhieuRaCong phieu = timPhieu(phieuId);

        if (!phieu.dangCho()) {
            throw new BusinessRuleException(
                    "Phiếu này đã được xử lý rồi", "PHIEU_DA_XU_LY");
        }
        if (!laAdmin && !phieu.getNguoiTao().getId().equals(nguoiHuyId)) {
            throw new AccessDeniedBusinessException(
                    "Chỉ người tạo phiếu hoặc quản trị viên mới huỷ được",
                    "KHONG_PHAI_NGUOI_TAO");
        }

        phieu.setTrangThai(TrangThaiPhieu.HUY);
        log.info("Huỷ phiếu {}", phieuId);
        dayTinSauKhiCommit(phieu.getNamHoc().getId(), "DA_HUY", phieu);
        return PhieuRaCongResponse.tu(phieu);
    }

    // -----------------------------------------------------------------

    /**
     * Đẩy tin WebSocket SAU KHI transaction commit thành công.
     *
     * <p><b>Đây là chi tiết dễ sai nhất trong cả module.</b> Nếu gọi thẳng
     * {@code publisher.day(...)} ngay tại chỗ, bản tin bay đi trước khi
     * transaction commit. Hai hậu quả:
     * <ul>
     *   <li>Transaction rollback ở bước sau (vi phạm ràng buộc DB chẳng hạn) →
     *       màn hình trực cổng đã kêu chuông cho một phiếu KHÔNG TỒN TẠI.</li>
     *   <li>Màn hình trực nhận tin rồi gọi API tải lại danh sách, nhưng dữ
     *       liệu chưa commit nên nó không thấy phiếu vừa được báo.</li>
     * </ul>
     *
     * <p>{@code TransactionSynchronization.afterCommit} bảo Spring: chờ commit
     * xong đã rồi hẵng chạy. Nếu không có transaction nào đang mở (gọi từ test
     * chẳng hạn) thì đẩy luôn.
     */
    private void dayTinSauKhiCommit(UUID namHocId, String loai, PhieuRaCong phieu) {
        PhieuRaCongResponse ban = PhieuRaCongResponse.tu(phieu);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.day(namHocId, loai, ban);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        publisher.day(namHocId, loai, ban);
                    }
                });
    }

    private PhieuRaCong timPhieu(UUID id) {
        return phieuRepository.timKemQuanHe(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Phiếu ra cổng", id));
    }

    /**
     * Phiếu ra cổng luôn thuộc năm học ĐANG HOẠT ĐỘNG.
     *
     * <p>Không cho client chọn năm: phiếu ra cổng là việc của hôm nay, không
     * ai viết phiếu cho năm học đã kết thúc. Ép ở đây thì không bao giờ có
     * phiếu lạc sang năm khác.
     */
    private NamHoc namHocDangChay() {
        return namHocRepository.findByTrangThai(TrangThaiNamHoc.DANG_HOAT_DONG)
                .orElseThrow(() -> new BusinessRuleException(
                        "Chưa có năm học nào đang hoạt động. Hãy kích hoạt năm học trước.",
                        "CHUA_CO_NAM_HOC_HOAT_DONG"));
    }
}
