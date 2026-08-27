package vn.tntt.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.tntt.common.config.AppProperties;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.personnel.entity.NguoiDung;
import vn.tntt.personnel.entity.VaiTro;
import vn.tntt.personnel.repository.NguoiDungRepository;
import vn.tntt.security.dto.DangNhapRequest;
import vn.tntt.security.dto.DoiMatKhauRequest;
import vn.tntt.security.entity.RefreshToken;
import vn.tntt.security.repository.RefreshTokenRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test nghiệp vụ xác thực.
 *
 * <p>Dùng {@link BCryptPasswordEncoder} THẬT chứ không mock: việc băm và đối
 * chiếu mật khẩu chính là thứ đáng kiểm chứng nhất ở đây, mock nó đi thì test
 * chỉ còn kiểm tra rằng ta có gọi hàm — không kiểm tra rằng nó đúng.
 */
class AuthServiceTest {

    private static final String MAT_KHAU = "Admin@123";

    private NguoiDungRepository nguoiDungRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService service;
    private NguoiDung nguoiDung;

    @BeforeEach
    void setUp() {
        nguoiDungRepository = mock(NguoiDungRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = new BCryptPasswordEncoder(4); // cost thấp cho test nhanh

        var cauHinh = new AppProperties(
                List.of("http://localhost:5173"),
                new AppProperties.Jwt(
                        "khoa_test_dai_it_nhat_64_ky_tu_de_hmac_sha256_chap_nhan_duoc_2026_ok",
                        30, 7));
        service = new AuthService(nguoiDungRepository, refreshTokenRepository,
                passwordEncoder, new JwtService(cauHinh));

        nguoiDung = new NguoiDung();
        nguoiDung.setId(UUID.randomUUID());
        nguoiDung.setHoTen("Quản trị viên");
        nguoiDung.setTenThanh("Giuse");
        nguoiDung.setEmail("admin@xudoan.local");
        nguoiDung.setMatKhauHash(passwordEncoder.encode(MAT_KHAU));
        nguoiDung.setDangHoatDong(true);
        nguoiDung.setCanDoiMatKhau(true);

        VaiTro admin = new VaiTro();
        admin.setId(UUID.randomUUID());
        admin.setMa("ADMIN");
        admin.setTenHienThi("Quản trị viên");
        nguoiDung.getVaiTro().add(admin);
    }

    @Nested
    @DisplayName("Đăng nhập")
    class DangNhap {

        @Test
        @DisplayName("Đúng mật khẩu thì cấp access token kèm vai trò")
        void dangNhapThanhCong() {
            when(nguoiDungRepository.timTheoDinhDanh("admin@xudoan.local"))
                    .thenReturn(Optional.of(nguoiDung));
            when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var ketQua = service.dangNhap(
                    new DangNhapRequest("admin@xudoan.local", MAT_KHAU), "127.0.0.1");

            assertThat(ketQua.phanHoi().accessToken()).isNotBlank();
            assertThat(ketQua.refreshTokenTho()).isNotBlank();
            assertThat(ketQua.phanHoi().nguoiDung().vaiTro()).containsExactly("ADMIN");
            assertThat(ketQua.phanHoi().nguoiDung().canDoiMatKhau()).isTrue();
        }

        @Test
        @DisplayName("Sai mật khẩu và không có tài khoản trả CÙNG một thông báo")
        void khongLoRaTaiKhoanNaoCoThat() {
            when(nguoiDungRepository.timTheoDinhDanh("admin@xudoan.local"))
                    .thenReturn(Optional.of(nguoiDung));
            when(nguoiDungRepository.timTheoDinhDanh("khong-co@dau.vn"))
                    .thenReturn(Optional.empty());

            String loiSaiMatKhau = batLoi(() -> service.dangNhap(
                    new DangNhapRequest("admin@xudoan.local", "sai-be-bét"), null));
            String loiKhongCoTaiKhoan = batLoi(() -> service.dangNhap(
                    new DangNhapRequest("khong-co@dau.vn", MAT_KHAU), null));

            // Đây mới là điều phải chắc chắn: hai câu PHẢI giống hệt nhau.
            // Khác nhau một chữ là đủ để dò ra email nào có thật trong hệ thống.
            assertThat(loiSaiMatKhau).isEqualTo(loiKhongCoTaiKhoan);
        }

        @Test
        @DisplayName("Tài khoản bị khoá thì không đăng nhập được")
        void taiKhoanBiKhoa() {
            nguoiDung.setDangHoatDong(false);
            when(nguoiDungRepository.timTheoDinhDanh(anyString()))
                    .thenReturn(Optional.of(nguoiDung));

            assertThatThrownBy(() -> service.dangNhap(
                    new DangNhapRequest("admin@xudoan.local", MAT_KHAU), null))
                    .isInstanceOf(BusinessRuleException.class);

            verify(refreshTokenRepository, never()).save(any());
        }

        private String batLoi(Runnable hanhDong) {
            try {
                hanhDong.run();
                throw new AssertionError("Đáng lẽ phải ném lỗi");
            } catch (BusinessRuleException ex) {
                return ex.getMessage();
            }
        }
    }

    @Nested
    @DisplayName("Làm mới token")
    class LamMoi {

        @Test
        @DisplayName("Token đã thu hồi thì không dùng lại được")
        void khongDungLaiTokenDaThuHoi() {
            var token = new RefreshToken();
            token.setNguoiDung(nguoiDung);
            token.setHetHanLuc(OffsetDateTime.now().plusDays(7));
            token.setThuHoiLuc(OffsetDateTime.now().minusMinutes(1));
            when(refreshTokenRepository.timTheoMaBam(anyString())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.lamMoi("bat-ky", null))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("Token hết hạn thì không dùng được")
        void tokenHetHan() {
            var token = new RefreshToken();
            token.setNguoiDung(nguoiDung);
            token.setHetHanLuc(OffsetDateTime.now().minusSeconds(1));
            when(refreshTokenRepository.timTheoMaBam(anyString())).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.lamMoi("bat-ky", null))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("Làm mới thành công thì token CŨ bị thu hồi (xoay vòng)")
        void xoayVongToken() {
            var tokenCu = new RefreshToken();
            tokenCu.setNguoiDung(nguoiDung);
            tokenCu.setHetHanLuc(OffsetDateTime.now().plusDays(7));
            when(refreshTokenRepository.timTheoMaBam(anyString())).thenReturn(Optional.of(tokenCu));
            when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var ketQua = service.lamMoi("bat-ky", null);

            // Không xoay vòng thì một token bị đánh cắp dùng được suốt 7 ngày
            // song song với người dùng thật mà không ai phát hiện.
            assertThat(tokenCu.getThuHoiLuc()).isNotNull();
            assertThat(ketQua.refreshTokenTho()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Đổi mật khẩu")
    class DoiMatKhau {

        @Test
        @DisplayName("Đổi xong thì hết cờ bắt đổi và MỌI phiên khác bị thu hồi")
        void doiThanhCong() {
            when(nguoiDungRepository.findById(nguoiDung.getId()))
                    .thenReturn(Optional.of(nguoiDung));

            service.doiMatKhau(nguoiDung.getId(),
                    new DoiMatKhauRequest(MAT_KHAU, "MatKhauMoi@2026"));

            assertThat(nguoiDung.isCanDoiMatKhau()).isFalse();
            assertThat(passwordEncoder.matches("MatKhauMoi@2026", nguoiDung.getMatKhauHash()))
                    .isTrue();
            // Đổi mật khẩu mà không thu hồi phiên cũ thì việc đổi gần như vô
            // nghĩa: kẻ đã cầm token cũ vẫn dùng tiếp đủ 7 ngày.
            verify(refreshTokenRepository).thuHoiTatCa(org.mockito.ArgumentMatchers.eq(
                    nguoiDung.getId()), any());
        }

        @Test
        @DisplayName("Sai mật khẩu cũ thì bị từ chối")
        void saiMatKhauCu() {
            when(nguoiDungRepository.findById(nguoiDung.getId()))
                    .thenReturn(Optional.of(nguoiDung));

            assertThatThrownBy(() -> service.doiMatKhau(nguoiDung.getId(),
                    new DoiMatKhauRequest("sai-rồi", "MatKhauMoi@2026")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("hiện tại không đúng");
        }

        @Test
        @DisplayName("Mật khẩu mới trùng mật khẩu cũ thì bị từ chối")
        void trungMatKhauCu() {
            when(nguoiDungRepository.findById(nguoiDung.getId()))
                    .thenReturn(Optional.of(nguoiDung));

            assertThatThrownBy(() -> service.doiMatKhau(nguoiDung.getId(),
                    new DoiMatKhauRequest(MAT_KHAU, MAT_KHAU)))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }
}
