package vn.tntt.organization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ConflictException;
import vn.tntt.common.exception.ResourceNotFoundException;
import vn.tntt.organization.dto.LuuLopHocRequest;
import vn.tntt.organization.entity.LopHoc;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.organization.entity.Nganh;
import vn.tntt.organization.entity.TrangThaiNamHoc;
import vn.tntt.organization.repository.LopHocRepository;
import vn.tntt.organization.repository.NamHocRepository;
import vn.tntt.organization.repository.NganhRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test quy tắc nghiệp vụ của lớp học.
 *
 * <p>Trọng tâm là ba hàng rào chống mất dữ liệu, vì đó là thứ hỏng thì không
 * cứu được: không sửa dữ liệu năm học đã đóng, không chuyển lớp sang năm
 * khác, và không xoá lớp đang có thiếu nhi.
 */
class LopHocServiceTest {

    private LopHocRepository lopHocRepository;
    private NamHocRepository namHocRepository;
    private NganhRepository nganhRepository;
    private LopHocService service;

    private NamHoc namHocDangChay;
    private NamHoc namHocDaDong;
    private Nganh nganhAuNhi;

    @BeforeEach
    void setUp() {
        lopHocRepository = mock(LopHocRepository.class);
        namHocRepository = mock(NamHocRepository.class);
        nganhRepository = mock(NganhRepository.class);
        service = new LopHocService(lopHocRepository, namHocRepository, nganhRepository);

        namHocDangChay = namHoc("2026-2027", TrangThaiNamHoc.DANG_HOAT_DONG);
        namHocDaDong = namHoc("2025-2026", TrangThaiNamHoc.DA_KET_THUC);
        nganhAuNhi = nganh();
    }

    private static NamHoc namHoc(String ten, TrangThaiNamHoc trangThai) {
        NamHoc n = new NamHoc(ten, LocalDate.of(2026, 9, 1), LocalDate.of(2027, 5, 31));
        n.setTrangThai(trangThai);
        n.setId(UUID.randomUUID());
        return n;
    }

    private static Nganh nganh() {
        Nganh n = new Nganh();
        n.setId(UUID.randomUUID());
        n.setTenNganh("Ấu Nhi");
        n.setMaNganh("AU_NHI");
        n.setThuTu((short) 2);
        return n;
    }

    private static LopHoc lopHoc(String ten, NamHoc namHoc, Nganh nganh) {
        LopHoc l = new LopHoc();
        l.setId(UUID.randomUUID());
        l.setTenLop(ten);
        l.setNamHoc(namHoc);
        l.setNganh(nganh);
        l.setCapDo((short) 1);
        return l;
    }

    private LuuLopHocRequest yeuCau(String tenLop, NamHoc namHoc) {
        return new LuuLopHocRequest(tenLop, nganhAuNhi.getId(), namHoc.getId(),
                (short) 1, null);
    }

    @Nested
    @DisplayName("Tạo lớp")
    class TaoMoi {

        @Test
        @DisplayName("Không tạo được lớp trong năm học đã kết thúc")
        void khongTaoDuocTrongNamDaDong() {
            when(namHocRepository.findById(namHocDaDong.getId()))
                    .thenReturn(Optional.of(namHocDaDong));

            assertThatThrownBy(() -> service.taoMoi(yeuCau("Ấu 1A", namHocDaDong)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("đã kết thúc");

            // Quan trọng: chặn TRƯỚC khi chạm tới repository ghi.
            verify(lopHocRepository, never()).save(any());
        }

        @Test
        @DisplayName("Tên lớp trùng trong CÙNG năm học thì báo 409")
        void tenTrungTrongCungNamHoc() {
            when(namHocRepository.findById(namHocDangChay.getId()))
                    .thenReturn(Optional.of(namHocDangChay));
            when(nganhRepository.findById(nganhAuNhi.getId()))
                    .thenReturn(Optional.of(nganhAuNhi));
            when(lopHocRepository.existsByTenLopAndNamHocId("Ấu 1A", namHocDangChay.getId()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.taoMoi(yeuCau("Ấu 1A", namHocDangChay)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("2026-2027");
        }

        @Test
        @DisplayName("Ngành không tồn tại thì báo 404")
        void nganhKhongTonTai() {
            when(namHocRepository.findById(namHocDangChay.getId()))
                    .thenReturn(Optional.of(namHocDangChay));
            when(nganhRepository.findById(nganhAuNhi.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.taoMoi(yeuCau("Ấu 1A", namHocDangChay)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Đủ điều kiện thì lưu được")
        void taoThanhCong() {
            when(namHocRepository.findById(namHocDangChay.getId()))
                    .thenReturn(Optional.of(namHocDangChay));
            when(nganhRepository.findById(nganhAuNhi.getId()))
                    .thenReturn(Optional.of(nganhAuNhi));
            when(lopHocRepository.existsByTenLopAndNamHocId(anyString(), any()))
                    .thenReturn(false);
            when(lopHocRepository.save(any(LopHoc.class))).thenAnswer(i -> i.getArgument(0));

            assertThat(service.taoMoi(yeuCau("Ấu 1A", namHocDangChay)).tenLop())
                    .isEqualTo("Ấu 1A");
        }
    }

    @Nested
    @DisplayName("Sửa lớp")
    class CapNhat {

        @Test
        @DisplayName("Không chuyển được lớp sang năm học khác")
        void khongDoiDuocNamHoc() {
            LopHoc lop = lopHoc("Ấu 1A", namHocDangChay, nganhAuNhi);
            when(lopHocRepository.timTheoIdKemQuanHe(lop.getId()))
                    .thenReturn(Optional.of(lop));

            // Gửi lên namHocId của một năm học KHÁC
            LuuLopHocRequest doiNam = new LuuLopHocRequest(
                    "Ấu 1A", nganhAuNhi.getId(), UUID.randomUUID(), (short) 1, null);

            assertThatThrownBy(() -> service.capNhat(lop.getId(), doiNam))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("năm học khác");
        }

        @Test
        @DisplayName("Lưu lại mà không đổi tên thì KHÔNG bị báo trùng với chính nó")
        void giuNguyenTenKhongBiBaoTrung() {
            LopHoc lop = lopHoc("Ấu 1A", namHocDangChay, nganhAuNhi);
            when(lopHocRepository.timTheoIdKemQuanHe(lop.getId()))
                    .thenReturn(Optional.of(lop));
            when(nganhRepository.findById(nganhAuNhi.getId()))
                    .thenReturn(Optional.of(nganhAuNhi));
            // Truy vấn loại trừ chính bản ghi đang sửa nên trả false
            when(lopHocRepository.existsByTenLopAndNamHocIdAndIdNot(
                    eq("Ấu 1A"), any(), eq(lop.getId()))).thenReturn(false);

            assertThat(service.capNhat(lop.getId(), yeuCau("Ấu 1A", namHocDangChay)).tenLop())
                    .isEqualTo("Ấu 1A");
        }

        @Test
        @DisplayName("Không sửa được lớp thuộc năm học đã kết thúc")
        void khongSuaDuocLopCuaNamDaDong() {
            LopHoc lopCu = lopHoc("Ấu 1A", namHocDaDong, nganhAuNhi);
            when(lopHocRepository.timTheoIdKemQuanHe(lopCu.getId()))
                    .thenReturn(Optional.of(lopCu));

            assertThatThrownBy(() -> service.capNhat(lopCu.getId(), yeuCau("Ấu 1B", namHocDaDong)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("đã kết thúc");
        }
    }

    @Nested
    @DisplayName("Xoá lớp")
    class Xoa {

        @Test
        @DisplayName("Lớp đang có ghi danh thì KHÔNG xoá được")
        void chanXoaKhiConGhiDanh() {
            LopHoc lop = lopHoc("Ấu 1A", namHocDangChay, nganhAuNhi);
            when(lopHocRepository.timTheoIdKemQuanHe(lop.getId()))
                    .thenReturn(Optional.of(lop));
            when(lopHocRepository.demGhiDanh(lop.getId())).thenReturn(30L);

            assertThatThrownBy(() -> service.xoa(lop.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    // Nêu rõ số em, để admin hiểu mức độ chứ không chỉ bị chặn
                    .hasMessageContaining("30");

            // Đây mới là điều thật sự phải chắc chắn: DELETE không được chạy.
            // ghi_danh.lop_id khai ON DELETE CASCADE, nên một lệnh delete lọt
            // qua sẽ cuốn theo cả điểm danh và điểm số của 30 em.
            verify(lopHocRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Lớp trống thì xoá được")
        void xoaDuocLopTrong() {
            LopHoc lop = lopHoc("Ấu 1A", namHocDangChay, nganhAuNhi);
            when(lopHocRepository.timTheoIdKemQuanHe(lop.getId()))
                    .thenReturn(Optional.of(lop));
            when(lopHocRepository.demGhiDanh(lop.getId())).thenReturn(0L);

            service.xoa(lop.getId());

            verify(lopHocRepository).delete(lop);
        }

        @Test
        @DisplayName("Không xoá được lớp của năm học đã kết thúc")
        void khongXoaDuocLopCuaNamDaDong() {
            LopHoc lopCu = lopHoc("Ấu 1A", namHocDaDong, nganhAuNhi);
            when(lopHocRepository.timTheoIdKemQuanHe(lopCu.getId()))
                    .thenReturn(Optional.of(lopCu));

            assertThatThrownBy(() -> service.xoa(lopCu.getId()))
                    .isInstanceOf(BusinessRuleException.class);

            verify(lopHocRepository, never()).delete(any());
        }
    }
}
