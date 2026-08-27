package vn.tntt.organization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ConflictException;
import vn.tntt.common.exception.ResourceNotFoundException;
import vn.tntt.organization.dto.NamHocResponse;
import vn.tntt.organization.dto.TaoNamHocRequest;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.organization.entity.TrangThaiNamHoc;
import vn.tntt.organization.repository.NamHocRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test quy tắc nghiệp vụ của năm học.
 *
 * <p>Repository được thay bằng mock, nên test này KHÔNG cần PostgreSQL và
 * chạy được trên CI. Đổi lại, nó không kiểm chứng được các ràng buộc ở tầng
 * DB (partial unique index) — phần đó cần test tích hợp với Testcontainers,
 * để dành cho lúc CI đã ổn định.
 *
 * <p>Điều đáng test ở đây là các NHÁNH TỪ CHỐI, không phải nhánh thành công.
 * Nhánh thành công hỏng thì ai cũng thấy ngay khi bấm thử; còn "kích hoạt
 * năm thứ hai khi đã có năm đang chạy" thì phải cố tình mới gặp.
 */
class NamHocServiceTest {

    private NamHocRepository repository;
    private NamHocService service;

    private static final LocalDate BAT_DAU = LocalDate.of(2026, 9, 1);
    private static final LocalDate KET_THUC = LocalDate.of(2027, 5, 31);

    @BeforeEach
    void setUp() {
        repository = mock(NamHocRepository.class);
        service = new NamHocService(repository);
    }

    /** Tạo entity kèm id giả, vì id thật do Hibernate sinh lúc save. */
    private static NamHoc namHoc(String ten, TrangThaiNamHoc trangThai) {
        NamHoc n = new NamHoc(ten, BAT_DAU, KET_THUC);
        n.setTrangThai(trangThai);
        n.setId(UUID.randomUUID());
        return n;
    }

    @Nested
    @DisplayName("Tạo năm học")
    class TaoMoi {

        @Test
        @DisplayName("Năm học mới luôn ở trạng thái CHUAN_BI, không phải đang hoạt động")
        void luonBatDauOTrangThaiChuanBi() {
            when(repository.existsByTenNamHoc("2026-2027")).thenReturn(false);
            when(repository.save(any(NamHoc.class))).thenAnswer(i -> i.getArgument(0));

            NamHocResponse ketQua = service.taoMoi(
                    new TaoNamHocRequest("2026-2027", BAT_DAU, KET_THUC));

            // Đây là điểm mấu chốt: tạo bản ghi KHÔNG được đồng thời đổi năm
            // học của cả xứ đoàn. Muốn chạy phải kích hoạt bằng thao tác riêng.
            assertThat(ketQua.trangThai()).isEqualTo(TrangThaiNamHoc.CHUAN_BI);
        }

        @Test
        @DisplayName("Ngày kết thúc trước ngày bắt đầu thì bị từ chối")
        void ngayKetThucPhaiSauNgayBatDau() {
            assertThatThrownBy(() -> service.taoMoi(
                    new TaoNamHocRequest("2026-2027", KET_THUC, BAT_DAU)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("phải sau ngày bắt đầu");
        }

        @Test
        @DisplayName("Hai ngày trùng nhau cũng bị từ chối, không chỉ ngày lùi")
        void haiNgayTrungNhauCungBiTuChoi() {
            assertThatThrownBy(() -> service.taoMoi(
                    new TaoNamHocRequest("2026-2027", BAT_DAU, BAT_DAU)))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("Tên năm học trùng thì báo 409")
        void tenTrungThiBaoConflict() {
            when(repository.existsByTenNamHoc("2026-2027")).thenReturn(true);

            assertThatThrownBy(() -> service.taoMoi(
                    new TaoNamHocRequest("2026-2027", BAT_DAU, KET_THUC)))
                    .isInstanceOf(ConflictException.class);
        }
    }

    @Nested
    @DisplayName("Kích hoạt")
    class KichHoat {

        @Test
        @DisplayName("Không kích hoạt được khi đã có năm học khác đang chạy")
        void chiMotNamHocDuocHoatDong() {
            NamHoc namMoi = namHoc("2027-2028", TrangThaiNamHoc.CHUAN_BI);
            NamHoc dangChay = namHoc("2026-2027", TrangThaiNamHoc.DANG_HOAT_DONG);

            when(repository.findById(namMoi.getId())).thenReturn(Optional.of(namMoi));
            when(repository.findByTrangThai(TrangThaiNamHoc.DANG_HOAT_DONG))
                    .thenReturn(Optional.of(dangChay));

            assertThatThrownBy(() -> service.kichHoat(namMoi.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    // Thông báo phải nêu ĐÍCH DANH năm đang chặn, để admin
                    // biết phải đi kết thúc năm nào.
                    .hasMessageContaining("2026-2027");
        }

        @Test
        @DisplayName("Không mở lại được năm học đã kết thúc")
        void khongMoLaiNamDaKetThuc() {
            NamHoc daDong = namHoc("2025-2026", TrangThaiNamHoc.DA_KET_THUC);
            when(repository.findById(daDong.getId())).thenReturn(Optional.of(daDong));

            assertThatThrownBy(() -> service.kichHoat(daDong.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("đã kết thúc");
        }

        @Test
        @DisplayName("Kích hoạt thành công thì trạng thái đổi sang DANG_HOAT_DONG")
        void kichHoatThanhCong() {
            NamHoc nam = namHoc("2026-2027", TrangThaiNamHoc.CHUAN_BI);
            when(repository.findById(nam.getId())).thenReturn(Optional.of(nam));
            when(repository.findByTrangThai(TrangThaiNamHoc.DANG_HOAT_DONG))
                    .thenReturn(Optional.empty());

            assertThat(service.kichHoat(nam.getId()).trangThai())
                    .isEqualTo(TrangThaiNamHoc.DANG_HOAT_DONG);
        }

        @Test
        @DisplayName("Id không tồn tại thì báo 404")
        void idKhongTonTai() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.kichHoat(id))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Kết thúc")
    class KetThuc {

        @Test
        @DisplayName("Chỉ kết thúc được năm đang hoạt động, không kết thúc năm CHUAN_BI")
        void khongKetThucNamChuaChay() {
            NamHoc chuanBi = namHoc("2027-2028", TrangThaiNamHoc.CHUAN_BI);
            when(repository.findById(chuanBi.getId())).thenReturn(Optional.of(chuanBi));

            assertThatThrownBy(() -> service.ketThuc(chuanBi.getId()))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("Kết thúc thành công thì trạng thái đổi sang DA_KET_THUC")
        void ketThucThanhCong() {
            NamHoc dangChay = namHoc("2026-2027", TrangThaiNamHoc.DANG_HOAT_DONG);
            when(repository.findById(dangChay.getId())).thenReturn(Optional.of(dangChay));

            assertThat(service.ketThuc(dangChay.getId()).trangThai())
                    .isEqualTo(TrangThaiNamHoc.DA_KET_THUC);
        }
    }
}
