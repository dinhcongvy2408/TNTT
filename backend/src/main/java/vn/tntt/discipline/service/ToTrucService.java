package vn.tntt.discipline.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ConflictException;
import vn.tntt.common.exception.ResourceNotFoundException;
import vn.tntt.discipline.dto.LichTrucResponse;
import vn.tntt.discipline.dto.TaoLuanPhienRequest;
import vn.tntt.discipline.dto.ToTrucResponse;
import vn.tntt.discipline.entity.LichTruc;
import vn.tntt.discipline.entity.ToTruc;
import vn.tntt.discipline.repository.LichTrucRepository;
import vn.tntt.discipline.repository.ToTrucRepository;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.organization.repository.NamHocRepository;
import vn.tntt.personnel.entity.NguoiDung;
import vn.tntt.personnel.repository.NguoiDungRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ tổ trực và lịch trực (docs/02 mục 6.1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToTrucService {

    private final ToTrucRepository toTrucRepository;
    private final LichTrucRepository lichTrucRepository;
    private final NamHocRepository namHocRepository;
    private final NguoiDungRepository nguoiDungRepository;

    /** Chặn người dùng lỡ tay sinh lịch cho 50 năm. */
    private static final int TOI_DA_CA_MOI_LAN = 200;

    // -----------------------------------------------------------------
    // Tổ trực
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ToTrucResponse> danhSachTo() {
        return toTrucRepository.tatCaKemThanhVien().stream().map(ToTrucResponse::tu).toList();
    }

    @Transactional
    public ToTrucResponse taoTo(String tenTo, String moTa) {
        if (toTrucRepository.existsByTenTo(tenTo)) {
            throw new ConflictException("Tổ %s đã tồn tại".formatted(tenTo), "TEN_TO_DA_TON_TAI");
        }
        ToTruc to = new ToTruc();
        to.setTenTo(tenTo.trim());
        to.setMoTa(moTa);
        return ToTrucResponse.tu(toTrucRepository.save(to));
    }

    /**
     * Thêm thành viên vào tổ.
     *
     * <p>{@code Set} nên thêm trùng là không có tác dụng gì — đúng như mong
     * đợi, và cũng tránh vi phạm khoá chính ghép của bảng nối.
     */
    @Transactional
    public ToTrucResponse themThanhVien(UUID toTrucId, UUID nguoiDungId) {
        ToTruc to = toTrucRepository.findById(toTrucId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tổ trực", toTrucId));
        NguoiDung nguoiDung = nguoiDungRepository.findById(nguoiDungId)
                .orElseThrow(() -> ResourceNotFoundException.of("Người dùng", nguoiDungId));

        to.getThanhVien().add(nguoiDung);
        log.info("Thêm {} vào tổ {}", nguoiDungId, to.getTenTo());
        return ToTrucResponse.tu(to);
    }

    @Transactional
    public void xoaThanhVien(UUID toTrucId, UUID nguoiDungId) {
        ToTruc to = toTrucRepository.findById(toTrucId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tổ trực", toTrucId));
        to.getThanhVien().removeIf(n -> n.getId().equals(nguoiDungId));
    }

    // -----------------------------------------------------------------
    // Lịch trực
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LichTrucResponse> lichTrong(UUID namHocId, LocalDate tuNgay, LocalDate denNgay) {
        return lichTrucRepository.trongKhoang(namHocId, tuNgay, denNgay)
                .stream().map(LichTrucResponse::tu).toList();
    }

    @Transactional(readOnly = true)
    public List<LichTrucResponse> lichHomNay() {
        return lichTrucRepository.theoNgay(LocalDate.now())
                .stream().map(LichTrucResponse::tu).toList();
    }

    /**
     * Sinh lịch trực luân phiên theo tuần (docs/02: "Tổ A tuần 1, Tổ B tuần 2").
     *
     * <p>Nhảy từng 7 ngày từ {@code tuNgay}, mỗi mốc gán cho tổ kế tiếp trong
     * danh sách rồi quay vòng. Người dùng chọn ngày đầu là Chủ Nhật thì mọi
     * ngày sinh ra đều là Chủ Nhật — ta không tự đoán hộ họ ngày nào trong
     * tuần, vì có xứ đoàn sinh hoạt thứ Bảy.
     *
     * <p><b>Bỏ qua ca đã tồn tại thay vì báo lỗi.</b> Sinh lịch là thao tác
     * người dùng sẽ chạy lại nhiều lần khi bổ sung tổ mới; báo lỗi ở lần trùng
     * đầu tiên nghĩa là họ phải xoá tay lịch cũ trước. Trả về số ca thật sự
     * được tạo để họ biết chuyện gì đã xảy ra.
     */
    @Transactional
    public List<LichTrucResponse> taoLuanPhien(TaoLuanPhienRequest request) {
        if (!request.denNgay().isAfter(request.tuNgay())) {
            throw new BusinessRuleException(
                    "Ngày kết thúc phải sau ngày bắt đầu", "KHOANG_NGAY_KHONG_HOP_LE");
        }

        long soCa = ChronoUnit.WEEKS.between(request.tuNgay(), request.denNgay()) + 1;
        if (soCa > TOI_DA_CA_MOI_LAN) {
            throw new BusinessRuleException(
                    "Khoảng ngày quá dài, tối đa %d ca mỗi lần".formatted(TOI_DA_CA_MOI_LAN),
                    "KHOANG_NGAY_QUA_DAI");
        }

        NamHoc namHoc = namHocRepository.findById(request.namHocId())
                .orElseThrow(() -> ResourceNotFoundException.of("Năm học", request.namHocId()));
        if (namHoc.daKetThuc()) {
            throw new BusinessRuleException(
                    "Năm học đã kết thúc, dữ liệu chỉ đọc", "NAM_HOC_DA_KET_THUC");
        }

        List<ToTruc> cacTo = request.toTrucIds().stream()
                .map(id -> toTrucRepository.findById(id)
                        .orElseThrow(() -> ResourceNotFoundException.of("Tổ trực", id)))
                .toList();

        List<LichTruc> daTao = new ArrayList<>();
        LocalDate ngay = request.tuNgay();
        int viTri = 0;

        while (!ngay.isAfter(request.denNgay())) {
            ToTruc to = cacTo.get(viTri % cacTo.size());
            if (!lichTrucRepository.existsByNgayTrucAndCaTrucAndToTrucId(
                    ngay, request.caTruc(), to.getId())) {
                LichTruc lich = new LichTruc();
                lich.setToTruc(to);
                lich.setNamHoc(namHoc);
                lich.setNgayTruc(ngay);
                lich.setCaTruc(request.caTruc().trim());
                daTao.add(lichTrucRepository.save(lich));
            }
            ngay = ngay.plusWeeks(1);
            viTri++;
        }

        log.info("Sinh {} ca trực luân phiên cho {} tổ", daTao.size(), cacTo.size());
        return daTao.stream().map(LichTrucResponse::tu).toList();
    }
}
