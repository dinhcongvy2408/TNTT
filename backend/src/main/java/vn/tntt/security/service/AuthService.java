package vn.tntt.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.tntt.common.exception.ApiException;
import vn.tntt.common.exception.BusinessRuleException;
import vn.tntt.common.exception.ResourceNotFoundException;
import vn.tntt.personnel.entity.NguoiDung;
import vn.tntt.personnel.repository.NguoiDungRepository;
import vn.tntt.security.dto.DangNhapRequest;
import vn.tntt.security.dto.DangNhapResponse;
import vn.tntt.security.dto.DoiMatKhauRequest;
import vn.tntt.security.dto.ThongTinToiResponse;
import vn.tntt.security.entity.RefreshToken;
import vn.tntt.security.repository.RefreshTokenRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Nghiệp vụ xác thực: đăng nhập, làm mới, đăng xuất, đổi mật khẩu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final NguoiDungRepository nguoiDungRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /** Kết quả đăng nhập, kèm refresh token thô để controller đặt vào cookie. */
    public record KetQuaDangNhap(DangNhapResponse phanHoi, String refreshTokenTho) {
    }

    // -----------------------------------------------------------------
    // Đăng nhập
    // -----------------------------------------------------------------

    /**
     * <p><b>Vì sao mọi nhánh thất bại đều trả cùng một câu?</b> "Email hoặc
     * mật khẩu không đúng" được dùng cho cả ba trường hợp: không có tài khoản,
     * sai mật khẩu, tài khoản bị khoá.
     *
     * <p>Nếu tách ra thành "email không tồn tại" và "mật khẩu sai" thì bất kỳ
     * ai cũng dò được danh sách email có thật trong hệ thống, chỉ bằng cách
     * thử. Với 150 huynh trưởng thì đó là danh sách người thật, kèm địa chỉ
     * liên lạc thật.
     *
     * <p>Vẫn ghi log phân biệt được các trường hợp — nhưng log ở lại server.
     */
    @Transactional
    public KetQuaDangNhap dangNhap(DangNhapRequest request, String diaChiIp) {
        NguoiDung nguoiDung = nguoiDungRepository.timTheoDinhDanh(request.dinhDanh())
                .orElseThrow(() -> {
                    // KHÔNG log định danh người dùng nhập vào: nếu ai đó gõ
                    // nhầm mật khẩu vào ô email thì mật khẩu đó nằm lại trong log.
                    log.warn("Đăng nhập thất bại: không tìm thấy tài khoản");
                    return saiThongTinDangNhap();
                });

        if (!passwordEncoder.matches(request.matKhau(), nguoiDung.getMatKhauHash())) {
            log.warn("Đăng nhập thất bại: sai mật khẩu, tài khoản {}", nguoiDung.getId());
            throw saiThongTinDangNhap();
        }

        if (!nguoiDung.isDangHoatDong()) {
            log.warn("Đăng nhập thất bại: tài khoản {} đã bị khoá", nguoiDung.getId());
            throw saiThongTinDangNhap();
        }

        nguoiDung.setLanDangNhapCuoi(OffsetDateTime.now());
        log.info("Đăng nhập thành công: tài khoản {}", nguoiDung.getId());

        return capToken(nguoiDung, diaChiIp);
    }

    // -----------------------------------------------------------------
    // Làm mới token
    // -----------------------------------------------------------------

    /**
     * Đổi refresh token lấy access token mới.
     *
     * <p><b>Có XOAY VÒNG token (rotation):</b> mỗi lần làm mới, token cũ bị
     * thu hồi và cấp một token hoàn toàn mới. Nhờ vậy một refresh token chỉ
     * dùng được đúng một lần.
     *
     * <p>Lợi ích không hiển nhiên: nếu token bị đánh cắp, kẻ trộm dùng trước
     * thì lần làm mới kế tiếp của người dùng thật sẽ thất bại — người dùng bị
     * đăng xuất bất thường và biết có chuyện. Không xoay vòng thì cả hai bên
     * cùng dùng êm ru suốt 7 ngày, không ai hay.
     */
    @Transactional
    public KetQuaDangNhap lamMoi(String refreshTokenTho, String diaChiIp) {
        if (refreshTokenTho == null || refreshTokenTho.isBlank()) {
            throw new BusinessRuleException("Thiếu refresh token", "THIEU_REFRESH_TOKEN");
        }

        RefreshToken token = refreshTokenRepository.timTheoMaBam(jwtService.bam(refreshTokenTho))
                .orElseThrow(() -> new BusinessRuleException(
                        "Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại",
                        "REFRESH_TOKEN_KHONG_HOP_LE"));

        if (!token.conHieuLuc()) {
            throw new BusinessRuleException(
                    "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại",
                    "REFRESH_TOKEN_HET_HAN");
        }

        NguoiDung nguoiDung = token.getNguoiDung();
        if (!nguoiDung.isDangHoatDong()) {
            throw new BusinessRuleException("Tài khoản đã bị khoá", "TAI_KHOAN_BI_KHOA");
        }

        token.setThuHoiLuc(OffsetDateTime.now());
        return capToken(nguoiDung, diaChiIp);
    }

    // -----------------------------------------------------------------
    // Đăng xuất
    // -----------------------------------------------------------------

    /**
     * Thu hồi refresh token.
     *
     * <p>KHÔNG báo lỗi khi token không tồn tại hay đã thu hồi: đăng xuất phải
     * luôn thành công dưới mắt người dùng. Bấm đăng xuất mà nhận thông báo lỗi
     * thì họ sẽ tưởng mình vẫn còn đăng nhập.
     */
    @Transactional
    public void dangXuat(String refreshTokenTho) {
        if (refreshTokenTho == null || refreshTokenTho.isBlank()) {
            return;
        }
        refreshTokenRepository.timTheoMaBam(jwtService.bam(refreshTokenTho))
                .filter(RefreshToken::conHieuLuc)
                .ifPresent(token -> {
                    token.setThuHoiLuc(OffsetDateTime.now());
                    log.info("Đăng xuất: tài khoản {}", token.getNguoiDung().getId());
                });
    }

    // -----------------------------------------------------------------
    // Thông tin và đổi mật khẩu
    // -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public ThongTinToiResponse thongTinToi(UUID nguoiDungId) {
        return ThongTinToiResponse.tu(timNguoiDung(nguoiDungId));
    }

    /**
     * Đổi mật khẩu.
     *
     * <p>Thu hồi TẤT CẢ refresh token của người này sau khi đổi. Người ta đổi
     * mật khẩu thường vì nghi bị lộ; nếu các phiên cũ vẫn sống thì việc đổi
     * gần như vô nghĩa — kẻ đã cầm token cũ dùng tiếp được đủ 7 ngày.
     *
     * <p>Đổi lại, người dùng bị đăng xuất khỏi mọi thiết bị khác. Đó là hành
     * vi đúng, và cũng là điều họ mong đợi.
     */
    @Transactional
    public void doiMatKhau(UUID nguoiDungId, DoiMatKhauRequest request) {
        NguoiDung nguoiDung = timNguoiDung(nguoiDungId);

        if (!passwordEncoder.matches(request.matKhauCu(), nguoiDung.getMatKhauHash())) {
            throw new BusinessRuleException("Mật khẩu hiện tại không đúng", "SAI_MAT_KHAU_CU");
        }
        if (request.matKhauCu().equals(request.matKhauMoi())) {
            throw new BusinessRuleException(
                    "Mật khẩu mới phải khác mật khẩu cũ", "MAT_KHAU_TRUNG_CU");
        }

        nguoiDung.setMatKhauHash(passwordEncoder.encode(request.matKhauMoi()));
        nguoiDung.setCanDoiMatKhau(false);

        int soTokenThuHoi = refreshTokenRepository.thuHoiTatCa(nguoiDungId, OffsetDateTime.now());
        log.info("Đổi mật khẩu: tài khoản {}, thu hồi {} phiên", nguoiDungId, soTokenThuHoi);
    }

    // -----------------------------------------------------------------
    // Dùng chung
    // -----------------------------------------------------------------

    private KetQuaDangNhap capToken(NguoiDung nguoiDung, String diaChiIp) {
        String refreshTokenTho = jwtService.taoRefreshToken();

        RefreshToken banGhi = new RefreshToken();
        banGhi.setNguoiDung(nguoiDung);
        banGhi.setMaBam(jwtService.bam(refreshTokenTho));
        banGhi.setHetHanLuc(jwtService.hanRefreshToken().atZone(
                OffsetDateTime.now().getOffset()).toOffsetDateTime());
        banGhi.setDiaChiIp(diaChiIp);
        refreshTokenRepository.save(banGhi);

        var phanHoi = new DangNhapResponse(
                jwtService.taoAccessToken(nguoiDung),
                jwtService.accessTtlGiay(),
                ThongTinToiResponse.tu(nguoiDung));

        return new KetQuaDangNhap(phanHoi, refreshTokenTho);
    }

    private NguoiDung timNguoiDung(UUID id) {
        return nguoiDungRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Người dùng", id));
    }

    /** Một câu duy nhất cho mọi kiểu thất bại — xem javadoc của dangNhap. */
    private ApiException saiThongTinDangNhap() {
        return new BusinessRuleException(
                "Email hoặc mật khẩu không đúng", "SAI_THONG_TIN_DANG_NHAP");
    }
}
