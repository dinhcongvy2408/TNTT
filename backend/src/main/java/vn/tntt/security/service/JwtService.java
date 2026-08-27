package vn.tntt.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.tntt.common.config.AppProperties;
import vn.tntt.personnel.entity.NguoiDung;
import vn.tntt.personnel.entity.VaiTro;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sinh và kiểm tra token.
 *
 * <p><b>Hai loại token, hai thiết kế khác hẳn nhau — đây là phần đáng hiểu
 * nhất của cả Sprint 1:</b>
 *
 * <table border="1">
 *   <tr><th></th><th>Access token</th><th>Refresh token</th></tr>
 *   <tr><td>Dạng</td><td>JWT có chữ ký</td><td>chuỗi ngẫu nhiên 256 bit</td></tr>
 *   <tr><td>Sống</td><td>30 phút</td><td>7 ngày</td></tr>
 *   <tr><td>Server lưu</td><td>không</td><td>có, dạng băm</td></tr>
 *   <tr><td>Thu hồi được</td><td>không</td><td>có</td></tr>
 * </table>
 *
 * <p><b>Vì sao access token không lưu ở server?</b> Nó được gửi kèm MỌI
 * request. Nếu mỗi request phải tra DB thì ta mất đúng cái lợi của JWT. Đổi
 * lại, không thu hồi được — nên nó chỉ sống 30 phút, đủ ngắn để thiệt hại
 * khi lộ là có giới hạn.
 *
 * <p><b>Vì sao refresh token KHÔNG phải JWT?</b> Vì docs/04 đòi
 * {@code POST /auth/logout} thu hồi được nó. Một JWT tự chứa thì server không
 * có cách nào vô hiệu hoá trước hạn. Chuỗi ngẫu nhiên tra bảng thì có — và
 * refresh token chỉ được dùng vài lần mỗi ngày nên chi phí tra DB không đáng kể.
 */
@Slf4j
@Service
public class JwtService {

    /** Tên claim chứa danh sách vai trò. Ngắn để token nhẹ. */
    public static final String CLAIM_VAI_TRO = "vt";

    /** Cờ bắt đổi mật khẩu, nhét vào token để filter chặn được mà khỏi tra DB. */
    public static final String CLAIM_CAN_DOI_MAT_KHAU = "doiMk";

    public static final String CLAIM_HO_TEN = "hoTen";

    private final SecretKey khoaKy;
    private final int accessTtlPhut;
    private final int refreshTtlNgay;
    private final SecureRandom nguonNgauNhien = new SecureRandom();

    public JwtService(AppProperties appProperties) {
        AppProperties.Jwt cauHinh = appProperties.jwt();
        // HMAC-SHA256 đòi khoá tối thiểu 256 bit. AppProperties đã ép độ dài
        // tối thiểu 64 ký tự nên tới đây chắc chắn đủ; nếu không, Keys sẽ ném
        // ngay lúc khởi động chứ không phải lúc có người đăng nhập.
        this.khoaKy = Keys.hmacShaKeyFor(cauHinh.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlPhut = cauHinh.accessTtlMinutes();
        this.refreshTtlNgay = cauHinh.refreshTtlDays();
    }

    // -----------------------------------------------------------------
    // Access token
    // -----------------------------------------------------------------

    public String taoAccessToken(NguoiDung nguoiDung) {
        Instant bayGio = Instant.now();
        List<String> maVaiTro = nguoiDung.getVaiTro().stream().map(VaiTro::getMa).toList();

        return Jwts.builder()
                // subject là id chứ không phải email: email có thể đổi, id thì không.
                .subject(nguoiDung.getId().toString())
                .claim(CLAIM_HO_TEN, nguoiDung.tenDayDu())
                .claim(CLAIM_VAI_TRO, maVaiTro)
                .claim(CLAIM_CAN_DOI_MAT_KHAU, nguoiDung.isCanDoiMatKhau())
                .issuedAt(Date.from(bayGio))
                .expiration(Date.from(bayGio.plus(accessTtlPhut, ChronoUnit.MINUTES)))
                .signWith(khoaKy)
                .compact();
        // KHÔNG nhét email, số điện thoại hay ngày sinh vào token: phần thân
        // JWT chỉ là Base64, ai cầm token cũng đọc được. Chữ ký chống SỬA,
        // không chống ĐỌC.
    }

    /**
     * Đọc token, trả rỗng nếu chữ ký sai / hết hạn / định dạng hỏng.
     *
     * <p>Trả {@link Optional} thay vì ném exception vì với filter xác thực thì
     * "token không hợp lệ" là chuyện xảy ra hằng ngày (token hết hạn), không
     * phải sự cố.
     */
    public Optional<Claims> docToken(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(khoaKy)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            // Chỉ log loại lỗi, KHÔNG log nội dung token — token là thông tin
            // đăng nhập, log ra là tự tạo lỗ hổng.
            log.debug("Token không hợp lệ: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    // -----------------------------------------------------------------
    // Refresh token
    // -----------------------------------------------------------------

    /**
     * Sinh refresh token: 32 byte ngẫu nhiên mã Base64-URL.
     *
     * <p>{@link SecureRandom} chứ không phải {@code Math.random()} hay
     * {@code Random}: hai cái kia sinh số theo công thức đoán được, biết vài
     * giá trị là suy ra được các giá trị sau. Với thứ đóng vai trò chìa khoá
     * thì đó là hỏng hoàn toàn.
     */
    public String taoRefreshToken() {
        byte[] bytes = new byte[32];
        nguonNgauNhien.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Băm refresh token để lưu vào DB.
     *
     * <p>SHA-256 chứ không phải BCrypt — ngược với mật khẩu. Lý do: BCrypt cố
     * tình chậm để chống dò từ điển, nhưng token đã là 32 byte ngẫu nhiên thì
     * không có từ điển nào dò nổi. Bắt mỗi lần gọi {@code /auth/refresh} chờ
     * thêm vài chục mili-giây là trả giá mà không mua được gì.
     */
    public String bam(String token) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            // SHA-256 là thuật toán bắt buộc có trong mọi JVM. Tới được đây
            // nghĩa là môi trường chạy hỏng nặng, không phải lỗi nghiệp vụ.
            throw new IllegalStateException("JVM không có SHA-256", ex);
        }
    }

    public Instant hanRefreshToken() {
        return Instant.now().plus(refreshTtlNgay, ChronoUnit.DAYS);
    }

    public int refreshTtlNgay() {
        return refreshTtlNgay;
    }

    /** Access token sống bao nhiêu giây — để frontend chủ động làm mới sớm. */
    public long accessTtlGiay() {
        return accessTtlPhut * 60L;
    }

    /** Lấy id người dùng từ claims. */
    public UUID layNguoiDungId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }
}
