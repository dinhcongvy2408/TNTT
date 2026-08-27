package vn.tntt.organization.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ConflictException;
import vn.tntt.common.exception.ResourceNotFoundException;
import vn.tntt.organization.dto.NamHocResponse;
import vn.tntt.organization.dto.TaoNamHocRequest;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.organization.entity.TrangThaiNamHoc;
import vn.tntt.organization.repository.NamHocRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Nghiệp vụ năm học.
 *
 * <p>Đây là nơi DUY NHẤT chứa quy tắc nghiệp vụ. Controller chỉ nhận request
 * rồi gọi xuống đây (CLAUDE.md mục 5), còn repository chỉ biết đọc ghi.
 *
 * <p><b>Vì sao vẫn kiểm tra ở đây khi DB đã có ràng buộc?</b> Hai tầng phục
 * vụ hai mục đích khác nhau:
 * <ul>
 *   <li>Kiểm ở service cho ra câu thông báo tiếng Việt dễ hiểu.</li>
 *   <li>Ràng buộc ở DB là chốt chặn CUỐI CÙNG. Hai admin bấm Lưu cùng lúc
 *       thì cả hai đều đọc thấy "chưa có năm nào hoạt động" rồi cùng ghi —
 *       kiểm ở service không chặn được, chỉ partial unique index mới chặn.
 *       Khi đó {@code GlobalExceptionHandler} dịch tên ràng buộc sang tiếng
 *       Việt.</li>
 * </ul>
 * Bỏ tầng nào cũng sai: bỏ service thì thông báo xấu, bỏ DB thì có lúc lọt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NamHocService {

    private final NamHocRepository namHocRepository;

    // -----------------------------------------------------------------
    // Đọc
    // -----------------------------------------------------------------

    /**
     * {@code readOnly = true} không chỉ để cho đẹp: Hibernate bỏ qua bước
     * dirty checking (so sánh entity trước/sau để phát hiện thay đổi) và
     * PostgreSQL biết transaction này không ghi. Với danh sách dài thì đây
     * là khác biệt đo được.
     */
    @Transactional(readOnly = true)
    public List<NamHocResponse> layTatCa() {
        return namHocRepository.findAllByOrderByNgayBatDauDesc()
                .stream()
                .map(NamHocResponse::tu)
                .toList();
    }

    /**
     * Năm học đang hoạt động, hoặc rỗng nếu chưa kích hoạt năm nào.
     *
     * <p>Trả {@link Optional} lên tới controller là cố ý — "chưa có năm học
     * nào" là trạng thái hợp lệ của hệ thống mới cài, không phải lỗi 404.
     */
    @Transactional(readOnly = true)
    public Optional<NamHocResponse> layNamHocHienTai() {
        return namHocRepository.findByTrangThai(TrangThaiNamHoc.DANG_HOAT_DONG)
                .map(NamHocResponse::tu);
    }

    // -----------------------------------------------------------------
    // Ghi
    // -----------------------------------------------------------------

    @Transactional
    public NamHocResponse taoMoi(TaoNamHocRequest request) {
        // Bean Validation đã lo từng field riêng lẻ (@NotBlank, @Pattern).
        // Nó KHÔNG kiểm được quan hệ GIỮA hai field, nên phần đó nằm ở đây.
        if (!request.ngayKetThuc().isAfter(request.ngayBatDau())) {
            throw new BusinessRuleException(
                    "Ngày kết thúc phải sau ngày bắt đầu",
                    "NGAY_KET_THUC_KHONG_HOP_LE");
        }

        if (namHocRepository.existsByTenNamHoc(request.tenNamHoc())) {
            throw new ConflictException(
                    "Năm học %s đã tồn tại".formatted(request.tenNamHoc()),
                    "NAM_HOC_DA_TON_TAI");
        }

        // Năm học mới LUÔN ở trạng thái CHUAN_BI. Client không được chọn
        // trạng thái — muốn chạy thì phải kích hoạt bằng một thao tác riêng,
        // để việc "đổi năm học của cả xứ đoàn" là một quyết định có ý thức
        // chứ không phải hệ quả phụ của việc tạo bản ghi.
        NamHoc namHoc = new NamHoc(
                request.tenNamHoc(), request.ngayBatDau(), request.ngayKetThuc());

        NamHoc daLuu = namHocRepository.save(namHoc);
        log.info("Đã tạo năm học {}", daLuu.getTenNamHoc());
        return NamHocResponse.tu(daLuu);
    }

    /**
     * Đưa năm học vào vận hành: CHUAN_BI → DANG_HOAT_DONG.
     *
     * <p><b>Endpoint này KHÔNG có trong docs/04</b> — đó là lỗ hổng của tài
     * liệu, không phải ta tự ý thêm. Schema đặt mặc định CHUAN_BI, docs/02
     * đòi phải có một năm DANG_HOAT_DONG, mà không đặc tả nào nối hai đầu
     * lại. Ghi nhận ở docs/99 mục F1.
     */
    @Transactional
    public NamHocResponse kichHoat(UUID id) {
        NamHoc namHoc = timTheoId(id);

        if (namHoc.getTrangThai() == TrangThaiNamHoc.DANG_HOAT_DONG) {
            throw new BusinessRuleException(
                    "Năm học này đang hoạt động rồi", "NAM_HOC_DANG_HOAT_DONG");
        }
        if (namHoc.daKetThuc()) {
            throw new BusinessRuleException(
                    "Không mở lại được năm học đã kết thúc", "NAM_HOC_DA_KET_THUC");
        }

        // Chặn sớm để báo câu dễ hiểu. Chốt chặn thật là partial unique index
        // uq_nam_hoc_dang_hoat_dong ở migration V1.
        namHocRepository.findByTrangThai(TrangThaiNamHoc.DANG_HOAT_DONG)
                .ifPresent(dangChay -> {
                    throw new BusinessRuleException(
                            "Năm học %s đang hoạt động. Hãy kết thúc năm đó trước."
                                    .formatted(dangChay.getTenNamHoc()),
                            "DA_CO_NAM_HOC_HOAT_DONG");
                });

        namHoc.setTrangThai(TrangThaiNamHoc.DANG_HOAT_DONG);
        log.info("Đã kích hoạt năm học {}", namHoc.getTenNamHoc());

        // Không cần gọi save(): entity đang được quản lý trong transaction,
        // Hibernate tự phát hiện thay đổi và UPDATE khi transaction đóng.
        // Đây là "dirty checking" — lý do @Transactional phải nằm ở service.
        return NamHocResponse.tu(namHoc);
    }

    /** Đóng sổ năm học: DANG_HOAT_DONG → DA_KET_THUC. Không có đường lùi. */
    @Transactional
    public NamHocResponse ketThuc(UUID id) {
        NamHoc namHoc = timTheoId(id);

        if (namHoc.getTrangThai() != TrangThaiNamHoc.DANG_HOAT_DONG) {
            throw new BusinessRuleException(
                    "Chỉ kết thúc được năm học đang hoạt động",
                    "NAM_HOC_CHUA_HOAT_DONG");
        }

        namHoc.setTrangThai(TrangThaiNamHoc.DA_KET_THUC);
        log.info("Đã kết thúc năm học {}", namHoc.getTenNamHoc());
        return NamHocResponse.tu(namHoc);
    }

    private NamHoc timTheoId(UUID id) {
        return namHocRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Năm học", id));
    }
}
