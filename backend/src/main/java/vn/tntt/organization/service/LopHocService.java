package vn.tntt.organization.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ConflictException;
import vn.tntt.common.exception.ResourceNotFoundException;
import vn.tntt.organization.dto.LopHocResponse;
import vn.tntt.organization.dto.LuuLopHocRequest;
import vn.tntt.organization.entity.LopHoc;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.organization.entity.Nganh;
import vn.tntt.organization.repository.LopHocRepository;
import vn.tntt.organization.repository.NamHocRepository;
import vn.tntt.organization.repository.NganhRepository;

import java.util.List;
import java.util.UUID;

/**
 * Nghiệp vụ lớp học.
 *
 * <p>Ba quy tắc ở đây không nằm trong đặc tả gốc nhưng bắt buộc phải có, mỗi
 * cái chặn một kiểu mất dữ liệu khác nhau — đọc phần javadoc của từng phương
 * thức để biết vì sao.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LopHocService {

    private final LopHocRepository lopHocRepository;
    private final NamHocRepository namHocRepository;
    private final NganhRepository nganhRepository;

    // -----------------------------------------------------------------
    // Đọc
    // -----------------------------------------------------------------

    /**
     * Danh sách lớp của một năm học, lọc thêm theo ngành nếu có.
     *
     * <p>Bắt buộc phải truyền {@code namHocId} chứ không cho lấy "tất cả lớp
     * mọi năm". Lý do: sau vài năm vận hành, bảng này có vài trăm dòng thuộc
     * nhiều năm khác nhau, và gần như không màn hình nào cần trộn chúng lại.
     * Ép chọn năm ngay ở API giúp không ai vô tình hiển thị lớp của năm 2026
     * lẫn vào danh sách năm 2029.
     */
    @Transactional(readOnly = true)
    public List<LopHocResponse> layTheoNamHoc(UUID namHocId, UUID nganhId) {
        return lopHocRepository.timTheoNamHoc(namHocId, nganhId)
                .stream()
                .map(LopHocResponse::tu)
                .toList();
    }

    // -----------------------------------------------------------------
    // Ghi
    // -----------------------------------------------------------------

    @Transactional
    public LopHocResponse taoMoi(LuuLopHocRequest request) {
        NamHoc namHoc = timNamHoc(request.namHocId());
        chanKhiNamHocDaKetThuc(namHoc);

        Nganh nganh = timNganh(request.nganhId());

        if (lopHocRepository.existsByTenLopAndNamHocId(request.tenLop(), namHoc.getId())) {
            throw new ConflictException(
                    "Lớp %s đã tồn tại trong năm học %s"
                            .formatted(request.tenLop(), namHoc.getTenNamHoc()),
                    "TEN_LOP_DA_TON_TAI");
        }

        LopHoc lopHoc = new LopHoc();
        lopHoc.setTenLop(request.tenLop());
        lopHoc.setNganh(nganh);
        lopHoc.setNamHoc(namHoc);
        lopHoc.setCapDo(request.capDo());
        lopHoc.setGhiChu(request.ghiChu());

        LopHoc daLuu = lopHocRepository.save(lopHoc);
        log.info("Đã tạo lớp {} thuộc năm học {}",
                daLuu.getTenLop(), namHoc.getTenNamHoc());
        return LopHocResponse.tu(daLuu);
    }

    /**
     * Sửa lớp học.
     *
     * <p><b>KHÔNG cho đổi năm học của lớp.</b> Nghe như một hạn chế tuỳ tiện,
     * thực ra nó chặn một kiểu hỏng dữ liệu khó thấy: bảng {@code ghi_danh}
     * lưu cả {@code lop_id} lẫn {@code nam_hoc_id}, và có khoá ngoại GHÉP trỏ
     * vào cặp {@code (id, nam_hoc_id)} của {@code lop_hoc} (xem docs/99 mục
     * B1). Chuyển lớp sang năm khác thì mọi ghi danh cũ trỏ vào một cặp không
     * còn tồn tại — PostgreSQL sẽ từ chối, và người dùng nhận về một lỗi ràng
     * buộc khó hiểu thay vì một câu giải thích.
     *
     * <p>Về nghiệp vụ cũng không có nghĩa: "Ấu 1A của năm 2026-2027" và
     * "Ấu 1A của năm 2027-2028" là hai lớp khác nhau với hai danh sách học
     * sinh khác nhau. Muốn có lớp ở năm khác thì tạo lớp mới.
     */
    @Transactional
    public LopHocResponse capNhat(UUID id, LuuLopHocRequest request) {
        LopHoc lopHoc = timLopKemQuanHe(id);
        chanKhiNamHocDaKetThuc(lopHoc.getNamHoc());

        if (!lopHoc.getNamHoc().getId().equals(request.namHocId())) {
            throw new BusinessRuleException(
                    "Không chuyển được lớp sang năm học khác. "
                            + "Hãy tạo lớp mới trong năm học đó.",
                    "KHONG_DOI_DUOC_NAM_HOC");
        }

        // Bỏ qua chính bản ghi đang sửa, nếu không thì lưu lại mà không đổi
        // tên cũng bị báo trùng với chính nó.
        if (lopHocRepository.existsByTenLopAndNamHocIdAndIdNot(
                request.tenLop(), lopHoc.getNamHoc().getId(), id)) {
            throw new ConflictException(
                    "Lớp %s đã tồn tại trong năm học này".formatted(request.tenLop()),
                    "TEN_LOP_DA_TON_TAI");
        }

        lopHoc.setTenLop(request.tenLop());
        lopHoc.setNganh(timNganh(request.nganhId()));
        lopHoc.setCapDo(request.capDo());
        lopHoc.setGhiChu(request.ghiChu());

        log.info("Đã cập nhật lớp {}", lopHoc.getTenLop());
        return LopHocResponse.tu(lopHoc);
    }

    /**
     * Xoá lớp học.
     *
     * <p><b>Chặn khi lớp đã có ghi danh — đây là hàng rào quan trọng nhất
     * trong cả lớp service này.</b>
     *
     * <p>Migration V1 khai {@code ghi_danh.lop_id ... ON DELETE CASCADE}, và
     * {@code diem_danh.ghi_danh_id} cùng {@code diem_so.ghi_danh_id} cũng
     * CASCADE tiếp. Nghĩa là một lệnh {@code DELETE} lên một lớp có 30 em sẽ
     * xoá sạch 30 ghi danh, toàn bộ điểm danh cả năm và toàn bộ điểm số của
     * các em đó. PostgreSQL làm việc ấy trong im lặng, không hỏi lại, và
     * không có nút hoàn tác.
     *
     * <p>CLAUDE.md mục 6 đã yêu cầu soft delete cho hồ sơ thiếu nhi vì lý do
     * tương tự. Bảng {@code lop_hoc} không có cột {@code da_xoa}, nên hàng rào
     * duy nhất ta dựng được lúc này là từ chối xoá.
     *
     * <p>Đếm bằng native query vì entity {@code GhiDanh} phải tới Sprint 5 mới
     * có — nhưng cái CASCADE thì đã nằm sẵn trong DB từ V1 rồi, không chờ ai.
     */
    @Transactional
    public void xoa(UUID id) {
        LopHoc lopHoc = timLopKemQuanHe(id);
        chanKhiNamHocDaKetThuc(lopHoc.getNamHoc());

        long soGhiDanh = lopHocRepository.demGhiDanh(id);
        if (soGhiDanh > 0) {
            throw new BusinessRuleException(
                    ("Lớp %s đang có %d thiếu nhi ghi danh nên không xoá được. "
                            + "Hãy chuyển các em sang lớp khác trước.")
                            .formatted(lopHoc.getTenLop(), soGhiDanh),
                    "LOP_DANG_CO_GHI_DANH");
        }

        lopHocRepository.delete(lopHoc);
        log.info("Đã xoá lớp {}", lopHoc.getTenLop());
    }

    // -----------------------------------------------------------------
    // Dùng chung
    // -----------------------------------------------------------------

    /**
     * docs/02 bước 1: "Năm học cũ chuyển sang DA_KET_THUC → dữ liệu chỉ đọc".
     *
     * <p>Quy tắc này KHÔNG ép được bằng ràng buộc DB (docs/99 mục D3), nên
     * tầng service là chốt chặn duy nhất. Gọi ở cả ba phương thức ghi.
     */
    private void chanKhiNamHocDaKetThuc(NamHoc namHoc) {
        if (namHoc.daKetThuc()) {
            throw new BusinessRuleException(
                    "Năm học %s đã kết thúc, dữ liệu chỉ đọc"
                            .formatted(namHoc.getTenNamHoc()),
                    "NAM_HOC_DA_KET_THUC");
        }
    }

    private NamHoc timNamHoc(UUID id) {
        return namHocRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Năm học", id));
    }

    private Nganh timNganh(UUID id) {
        return nganhRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Ngành", id));
    }

    private LopHoc timLopKemQuanHe(UUID id) {
        return lopHocRepository.timTheoIdKemQuanHe(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Lớp học", id));
    }
}
