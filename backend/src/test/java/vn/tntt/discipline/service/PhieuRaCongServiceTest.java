package vn.tntt.discipline.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.tntt.common.exception.AccessDeniedBusinessException;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.discipline.dto.PhieuRaCongResponse;
import vn.tntt.discipline.dto.TaoPhieuRequest;
import vn.tntt.discipline.entity.PhieuRaCong;
import vn.tntt.discipline.entity.TrangThaiPhieu;
import vn.tntt.discipline.repository.PhieuRaCongRepository;
import vn.tntt.discipline.websocket.PhieuRaCongPublisher;
import vn.tntt.enrollment.entity.GhiDanh;
import vn.tntt.enrollment.repository.GhiDanhRepository;
import vn.tntt.organization.entity.LopHoc;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.organization.entity.TrangThaiNamHoc;
import vn.tntt.organization.repository.NamHocRepository;
import vn.tntt.personnel.entity.NguoiDung;
import vn.tntt.personnel.repository.NguoiDungRepository;
import vn.tntt.student.entity.ThieuNhi;
import vn.tntt.student.repository.ThieuNhiRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test máy trạng thái phiếu ra cổng.
 *
 * <p>Trọng tâm là các nhánh TỪ CHỐI và việc bản tin WebSocket có được đẩy đúng
 * lúc hay không. Nhánh thành công hỏng thì bấm thử là thấy ngay; còn "xác nhận
 * hai lần" hay "người khác huỷ phiếu của mình" thì phải cố tình mới gặp.
 */
class PhieuRaCongServiceTest {

    private PhieuRaCongRepository phieuRepository;
    private ThieuNhiRepository thieuNhiRepository;
    private GhiDanhRepository ghiDanhRepository;
    private NamHocRepository namHocRepository;
    private NguoiDungRepository nguoiDungRepository;
    private PhieuRaCongPublisher publisher;
    private PhieuRaCongService service;

    private NamHoc namHoc;
    private ThieuNhi thieuNhi;
    private NguoiDung giaoLyVien;

    @BeforeEach
    void setUp() {
        phieuRepository = mock(PhieuRaCongRepository.class);
        thieuNhiRepository = mock(ThieuNhiRepository.class);
        ghiDanhRepository = mock(GhiDanhRepository.class);
        namHocRepository = mock(NamHocRepository.class);
        nguoiDungRepository = mock(NguoiDungRepository.class);
        publisher = mock(PhieuRaCongPublisher.class);

        service = new PhieuRaCongService(phieuRepository, thieuNhiRepository,
                ghiDanhRepository, namHocRepository, nguoiDungRepository, publisher);

        namHoc = new NamHoc("2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 5, 31));
        namHoc.setId(UUID.randomUUID());
        namHoc.setTrangThai(TrangThaiNamHoc.DANG_HOAT_DONG);

        thieuNhi = new ThieuNhi();
        thieuNhi.setId(UUID.randomUUID());
        thieuNhi.setMaThieuNhi("TN2026001");
        thieuNhi.setTenThanh("Giuse");
        thieuNhi.setHoTen("Nguyễn Văn An");
        thieuNhi.setNgaySinh(LocalDate.of(2018, 4, 12));

        giaoLyVien = new NguoiDung();
        giaoLyVien.setId(UUID.randomUUID());
        giaoLyVien.setHoTen("Trần Thị B");

        when(namHocRepository.findByTrangThai(TrangThaiNamHoc.DANG_HOAT_DONG))
                .thenReturn(Optional.of(namHoc));
    }

    private PhieuRaCong phieu(TrangThaiPhieu trangThai, NguoiDung nguoiTao) {
        PhieuRaCong p = new PhieuRaCong();
        p.setId(UUID.randomUUID());
        p.setThieuNhi(thieuNhi);
        p.setNamHoc(namHoc);
        p.setNguoiTao(nguoiTao);
        p.setLyDo("Ốm");
        p.setTrangThai(trangThai);
        return p;
    }

    @Nested
    @DisplayName("Tạo phiếu")
    class TaoPhieu {

        @Test
        @DisplayName("Chưa có năm học hoạt động thì không tạo được phiếu")
        void chuaCoNamHocHoatDong() {
            when(namHocRepository.findByTrangThai(TrangThaiNamHoc.DANG_HOAT_DONG))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.taoPhieu(
                    new TaoPhieuRequest(thieuNhi.getId(), "Ốm"), giaoLyVien.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Chưa có năm học");
        }

        @Test
        @DisplayName("Em đang có phiếu chờ thì không tạo phiếu thứ hai")
        void khongTaoHaiPhieuChoCungMotEm() {
            when(thieuNhiRepository.timChuaXoa(thieuNhi.getId()))
                    .thenReturn(Optional.of(thieuNhi));
            when(phieuRepository.existsByThieuNhiIdAndTrangThai(
                    thieuNhi.getId(), TrangThaiPhieu.CHO_RA_CONG)).thenReturn(true);

            assertThatThrownBy(() -> service.taoPhieu(
                    new TaoPhieuRequest(thieuNhi.getId(), "Ốm"), giaoLyVien.getId()))
                    .isInstanceOf(BusinessRuleException.class);

            // Hai phiếu cùng chờ cho một em nghĩa là màn hình trực hiện tên em
            // hai lần, và người trực có thể cho em ra cổng rồi vẫn thấy còn
            // một phiếu đang chờ.
            verify(phieuRepository, never()).save(any());
        }

        @Test
        @DisplayName("Em CHƯA được xếp lớp vẫn tạo được phiếu, tenLop để trống")
        void emChuaXepLopVanTaoDuocPhieu() {
            when(thieuNhiRepository.timChuaXoa(thieuNhi.getId()))
                    .thenReturn(Optional.of(thieuNhi));
            when(phieuRepository.existsByThieuNhiIdAndTrangThai(any(), any())).thenReturn(false);
            when(nguoiDungRepository.findById(giaoLyVien.getId()))
                    .thenReturn(Optional.of(giaoLyVien));
            // Không có lượt ghi danh nào
            when(ghiDanhRepository.dangHocTrongNam(any(), any())).thenReturn(Optional.empty());
            when(phieuRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            PhieuRaCongResponse ketQua = service.taoPhieu(
                    new TaoPhieuRequest(thieuNhi.getId(), "Ốm"), giaoLyVien.getId());

            // Em mới chuyển đến, chưa xếp lớp, vẫn phải cho về được. Từ chối ở
            // đây là bắt phụ huynh đứng chờ ngoài cổng vì một chuyện hành chính.
            assertThat(ketQua.tenLop()).isNull();
            assertThat(ketQua.trangThai()).isEqualTo(TrangThaiPhieu.CHO_RA_CONG);
        }

        @Test
        @DisplayName("Tạo phiếu thành công thì đẩy bản tin PHIEU_MOI")
        void dayBanTinPhieuMoi() {
            when(thieuNhiRepository.timChuaXoa(thieuNhi.getId()))
                    .thenReturn(Optional.of(thieuNhi));
            when(phieuRepository.existsByThieuNhiIdAndTrangThai(any(), any())).thenReturn(false);
            when(nguoiDungRepository.findById(giaoLyVien.getId()))
                    .thenReturn(Optional.of(giaoLyVien));
            when(ghiDanhRepository.dangHocTrongNam(any(), any())).thenReturn(Optional.empty());
            when(phieuRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.taoPhieu(new TaoPhieuRequest(thieuNhi.getId(), "Ốm"), giaoLyVien.getId());

            // Ngoài transaction thì đẩy ngay; trong transaction thì hoãn tới
            // afterCommit. Test này chạy ngoài transaction nên đẩy ngay.
            verify(publisher).day(eq(namHoc.getId()), eq("PHIEU_MOI"), any());
        }
    }

    @Nested
    @DisplayName("Xác nhận ra cổng")
    class XacNhan {

        @Test
        @DisplayName("Phiếu đã xác nhận rồi thì không xác nhận lần hai")
        void khongXacNhanHaiLan() {
            PhieuRaCong daXong = phieu(TrangThaiPhieu.DA_RA_CONG, giaoLyVien);
            when(phieuRepository.timKemQuanHe(daXong.getId())).thenReturn(Optional.of(daXong));

            assertThatThrownBy(() -> service.xacNhan(daXong.getId(), UUID.randomUUID()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("đã được xử lý");
        }

        @Test
        @DisplayName("Xác nhận đặt ĐỦ CẢ BA: trạng thái, thời gian, người xác nhận")
        void datDuBaTruong() {
            PhieuRaCong dangCho = phieu(TrangThaiPhieu.CHO_RA_CONG, giaoLyVien);
            NguoiDung nguoiTruc = new NguoiDung();
            nguoiTruc.setId(UUID.randomUUID());
            nguoiTruc.setHoTen("Lê Văn C");

            when(phieuRepository.timKemQuanHe(dangCho.getId())).thenReturn(Optional.of(dangCho));
            when(nguoiDungRepository.findById(nguoiTruc.getId()))
                    .thenReturn(Optional.of(nguoiTruc));

            service.xacNhan(dangCho.getId(), nguoiTruc.getId());

            // Ràng buộc ck_phieu_xac_nhan ở DB đòi đúng như vậy: DA_RA_CONG thì
            // bắt buộc có thời gian VÀ người xác nhận. Thiếu một cái là
            // PostgreSQL từ chối cả câu UPDATE.
            assertThat(dangCho.getTrangThai()).isEqualTo(TrangThaiPhieu.DA_RA_CONG);
            assertThat(dangCho.getThoiGianRaCong()).isNotNull();
            assertThat(dangCho.getNguoiXacNhan()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Huỷ phiếu")
    class Huy {

        @Test
        @DisplayName("Người KHÁC không huỷ được phiếu của mình")
        void nguoiKhacKhongHuyDuoc() {
            PhieuRaCong dangCho = phieu(TrangThaiPhieu.CHO_RA_CONG, giaoLyVien);
            when(phieuRepository.timKemQuanHe(dangCho.getId())).thenReturn(Optional.of(dangCho));

            assertThatThrownBy(() -> service.huy(dangCho.getId(), UUID.randomUUID(), false))
                    .isInstanceOf(AccessDeniedBusinessException.class);
        }

        @Test
        @DisplayName("Người tạo huỷ được phiếu của mình")
        void nguoiTaoHuyDuoc() {
            PhieuRaCong dangCho = phieu(TrangThaiPhieu.CHO_RA_CONG, giaoLyVien);
            when(phieuRepository.timKemQuanHe(dangCho.getId())).thenReturn(Optional.of(dangCho));

            assertThat(service.huy(dangCho.getId(), giaoLyVien.getId(), false).trangThai())
                    .isEqualTo(TrangThaiPhieu.HUY);
        }

        @Test
        @DisplayName("ADMIN huỷ được phiếu của người khác")
        void adminHuyDuoc() {
            PhieuRaCong dangCho = phieu(TrangThaiPhieu.CHO_RA_CONG, giaoLyVien);
            when(phieuRepository.timKemQuanHe(dangCho.getId())).thenReturn(Optional.of(dangCho));

            assertThat(service.huy(dangCho.getId(), UUID.randomUUID(), true).trangThai())
                    .isEqualTo(TrangThaiPhieu.HUY);
        }

        @Test
        @DisplayName("Phiếu đã ra cổng thì không huỷ ngược lại được")
        void khongHuyPhieuDaRaCong() {
            PhieuRaCong daXong = phieu(TrangThaiPhieu.DA_RA_CONG, giaoLyVien);
            when(phieuRepository.timKemQuanHe(daXong.getId())).thenReturn(Optional.of(daXong));

            // Em đã ra khỏi cổng từ lâu; "huỷ" phiếu đó không có nghĩa gì và
            // làm hỏng dấu vết truy trách nhiệm.
            assertThatThrownBy(() -> service.huy(daXong.getId(), giaoLyVien.getId(), true))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }
}
