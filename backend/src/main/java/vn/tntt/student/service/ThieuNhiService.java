package vn.tntt.student.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ResourceNotFoundException;
import vn.tntt.common.response.PageResponse;
import vn.tntt.student.dto.LuuThieuNhiRequest;
import vn.tntt.student.dto.ThieuNhiResponse;
import vn.tntt.student.entity.ThieuNhi;
import vn.tntt.student.repository.ThieuNhiRepository;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Nghiệp vụ hồ sơ thiếu nhi.
 *
 * <p><b>Phạm vi của lát cắt này.</b> Đủ để module phiếu ra cổng (Sprint 7) có
 * người mà viết phiếu. CHƯA có: lịch sử bí tích, import Excel, tìm kiếm không
 * dấu bằng chỉ mục GIN. Xem docs/99 mục H1.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThieuNhiService {

    private final ThieuNhiRepository thieuNhiRepository;

    /** Tiền tố mã. docs/02 để trong bảng cau_hinh; lát cắt này tạm cố định. */
    private static final String TIEN_TO = "TN";

    /** Số lần thử lại khi hai người cùng tạo hồ sơ và giành nhau một mã. */
    private static final int SO_LAN_THU = 5;

    @Transactional(readOnly = true)
    public PageResponse<ThieuNhiResponse> tim(String tuKhoa, int trang, int coTrang) {
        // Chuỗi rỗng nghĩa là "không lọc". KHÔNG dùng null: xem javadoc của
        // ThieuNhiRepository.tim để biết vì sao null làm hỏng câu truy vấn.
        String khoa = tuKhoa == null ? "" : tuKhoa.trim();
        Page<ThieuNhi> ketQua = thieuNhiRepository.tim(khoa, PageRequest.of(trang, coTrang));
        return PageResponse.from(ketQua, ThieuNhiResponse::tu);
    }

    @Transactional(readOnly = true)
    public ThieuNhiResponse xem(UUID id) {
        return ThieuNhiResponse.tu(timHoSo(id));
    }

    /**
     * Tạo hồ sơ mới, tự sinh mã dạng {@code TN2026001}.
     *
     * <p><b>Vì sao có vòng lặp thử lại?</b> Sinh mã theo kiểu "đọc mã lớn nhất
     * rồi cộng một" có một khe hở kinh điển: hai người bấm Lưu cùng lúc thì cả
     * hai đọc được cùng một giá trị và cùng sinh ra một mã. Ràng buộc UNIQUE ở
     * DB sẽ chặn người thứ hai — đúng như phải thế — nhưng nếu ta để nguyên
     * thì họ nhận về một lỗi trùng khó hiểu cho thứ họ không hề nhập.
     *
     * <p>Nên ta bắt lỗi đó và thử lại với mã kế tiếp. DB vẫn là chốt chặn
     * thật; vòng lặp chỉ làm cho trải nghiệm đúng đắn.
     *
     * <p>Cách sạch hơn là dùng một {@code SEQUENCE} của PostgreSQL, nhưng mã
     * có nhúng năm nên sequence phải reset mỗi năm — thêm một việc phải nhớ
     * làm hằng năm, đổi lấy một chỗ đua tranh gần như không xảy ra ở quy mô
     * này (ban điều hành nhập hồ sơ, không phải 1.000 người đăng ký cùng lúc).
     */
    @Transactional
    public ThieuNhiResponse taoMoi(LuuThieuNhiRequest request) {
        for (int lan = 0; lan < SO_LAN_THU; lan++) {
            try {
                ThieuNhi hoSo = new ThieuNhi();
                hoSo.setMaThieuNhi(sinhMaKeTiep(lan));
                apDung(hoSo, request);
                ThieuNhi daLuu = thieuNhiRepository.saveAndFlush(hoSo);
                log.info("Đã tạo hồ sơ thiếu nhi {}", daLuu.getMaThieuNhi());
                return ThieuNhiResponse.tu(daLuu);
            } catch (DataIntegrityViolationException ex) {
                // saveAndFlush đẩy INSERT xuống DB NGAY, nên lỗi trùng nổ ở
                // đây chứ không phải lúc transaction đóng — nhờ vậy mới bắt
                // được để thử lại. Với save() thường thì Hibernate hoãn INSERT
                // tới cuối transaction và ta hết cơ hội.
                log.warn("Mã thiếu nhi bị trùng, thử lại lần {}", lan + 1);
            }
        }
        throw new BusinessRuleException(
                "Không sinh được mã thiếu nhi, thử lại sau ít giây",
                "KHONG_SINH_DUOC_MA");
    }

    @Transactional
    public ThieuNhiResponse capNhat(UUID id, LuuThieuNhiRequest request) {
        ThieuNhi hoSo = timHoSo(id);
        apDung(hoSo, request);
        log.info("Đã cập nhật hồ sơ thiếu nhi {}", hoSo.getMaThieuNhi());
        return ThieuNhiResponse.tu(hoSo);
    }

    /**
     * Xoá MỀM.
     *
     * <p>CLAUDE.md mục 6 yêu cầu rõ: "Soft delete cho hồ sơ thiếu nhi
     * ({@code da_xoa boolean}), không xoá cứng". Lý do không chỉ là tiếc dữ
     * liệu: {@code ghi_danh} trỏ tới id này với {@code ON DELETE CASCADE}, nên
     * xoá cứng một em là xoá luôn mọi ghi danh, điểm danh và điểm số của em
     * qua tất cả các năm.
     */
    @Transactional
    public void xoa(UUID id) {
        ThieuNhi hoSo = timHoSo(id);
        hoSo.setDaXoa(true);
        log.info("Đã xoá mềm hồ sơ thiếu nhi {}", hoSo.getMaThieuNhi());
    }

    // -----------------------------------------------------------------

    private ThieuNhi timHoSo(UUID id) {
        return thieuNhiRepository.timChuaXoa(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Thiếu nhi", id));
    }

    private void apDung(ThieuNhi hoSo, LuuThieuNhiRequest r) {
        hoSo.setTenThanh(rongThanhNull(r.tenThanh()));
        hoSo.setHoTen(r.hoTen().trim());
        hoSo.setNgaySinh(r.ngaySinh());
        hoSo.setGioiTinh(rongThanhNull(r.gioiTinh()));
        hoSo.setTenBo(rongThanhNull(r.tenBo()));
        hoSo.setTenMe(rongThanhNull(r.tenMe()));
        hoSo.setSdtPhuHuynh(rongThanhNull(r.sdtPhuHuynh()));
        hoSo.setDiaChi(rongThanhNull(r.diaChi()));
        hoSo.setGiaoHo(rongThanhNull(r.giaoHo()));
        hoSo.setGhiChu(rongThanhNull(r.ghiChu()));
    }

    /**
     * Chuỗi rỗng thành null.
     *
     * <p>Form HTML gửi lên chuỗi rỗng cho ô không nhập, chứ không gửi null.
     * Không xử lý thì DB đầy những {@code ''} — và {@code '' IS NOT NULL} là
     * đúng, nên mọi truy vấn kiểu "em nào chưa có số điện thoại phụ huynh" sẽ
     * bỏ sót.
     */
    private String rongThanhNull(String giaTri) {
        return giaTri == null || giaTri.isBlank() ? null : giaTri.trim();
    }

    /** {@code TN} + năm hiện tại + số thứ tự 3 chữ số. */
    private String sinhMaKeTiep(int buOffset) {
        String tienTo = TIEN_TO + LocalDate.now().getYear();
        int keTiep = thieuNhiRepository.maLonNhat(tienTo)
                .map(ma -> Integer.parseInt(ma.substring(tienTo.length())) + 1)
                .orElse(1) + buOffset;
        return "%s%03d".formatted(tienTo, keTiep);
    }
}
