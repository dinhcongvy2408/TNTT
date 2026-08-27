package vn.tntt.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chốt chặn cho lỗi ở docs/99 mục E1.
 *
 * <p><b>Chuyện đã xảy ra.</b> Migration V3 chép hash mật khẩu admin từ
 * {@code schema.sql} gốc kèm chú thích "mật khẩu Admin@123". Chú thích đó sai:
 * hash ấy không khớp {@code Admin@123}, cũng không khớp bất kỳ mật khẩu phổ
 * biến nào. Tài khoản quản trị DUY NHẤT của hệ thống không đăng nhập được, và
 * không ai biết cho tới khi có người thử.
 *
 * <p><b>Test này chính là thứ đáng lẽ phải có từ đầu.</b> Nó đọc thẳng file
 * migration đang có hiệu lực và đối chiếu hash trong đó bằng cùng thuật toán
 * mà ứng dụng dùng lúc chạy. Ai sửa hash mà không đổi mật khẩu tương ứng —
 * hoặc chép nhầm một lần nữa — thì CI đỏ ngay.
 *
 * <p>Không cần database: chỉ cần file migration và {@code BCryptPasswordEncoder}.
 */
class MatKhauAdminTest {

    /**
     * Mật khẩu mặc định của tài khoản quản trị ở môi trường dev.
     *
     * <p>Nó nằm công khai trong repo, và điều đó CHẤP NHẬN ĐƯỢC vì
     * {@code can_doi_mat_khau = true} bắt đổi ngay lần đăng nhập đầu, còn
     * production thì phải viết migration mới đặt hash thật (xem V4).
     */
    private static final String MAT_KHAU_MONG_DOI = "Admin@123";

    /** File migration đang quyết định mật khẩu admin. Đổi V mới thì sửa ở đây. */
    private static final Path FILE_MIGRATION =
            Path.of("src/main/resources/db/migration/V4__sua_mat_khau_admin.sql");

    /** Hash BCrypt luôn có dạng $2a$<cost>$<22 ký tự salt><31 ký tự hash>. */
    private static final Pattern MAU_HASH =
            Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    @Test
    @DisplayName("Hash trong migration V4 phải khớp đúng mật khẩu Admin@123")
    void hashTrongMigrationPhaiKhopMatKhau() throws IOException {
        String noiDung = Files.readString(FILE_MIGRATION, StandardCharsets.UTF_8);

        Matcher matcher = MAU_HASH.matcher(noiDung);
        assertThat(matcher.find())
                .as("Không tìm thấy hash BCrypt nào trong %s", FILE_MIGRATION)
                .isTrue();

        String hash = matcher.group();

        assertThat(new BCryptPasswordEncoder().matches(MAT_KHAU_MONG_DOI, hash))
                .as("""
                    Hash trong %s KHÔNG khớp mật khẩu '%s'.
                    Đây đúng là lỗi đã xảy ra một lần ở migration V3 (docs/99 mục E1):
                    tài khoản quản trị duy nhất không đăng nhập được.
                    Sinh hash mới bằng PostgreSQL (pgcrypto đã bật từ V1):
                      SELECT crypt('%s', gen_salt('bf', 10));
                    """, FILE_MIGRATION, MAT_KHAU_MONG_DOI, MAT_KHAU_MONG_DOI)
                .isTrue();
    }

    @Test
    @DisplayName("Hash phải dùng cost 10, khớp với BCryptPasswordEncoder trong SecurityConfig")
    void hashPhaiDungCost10() throws IOException {
        String noiDung = Files.readString(FILE_MIGRATION, StandardCharsets.UTF_8);
        Matcher matcher = MAU_HASH.matcher(noiDung);
        assertThat(matcher.find()).isTrue();

        // Cost nằm ngay sau "$2a$". Không bắt buộc phải khớp encoder để đăng
        // nhập được (BCrypt đọc cost từ chính hash), nhưng lệch cost nghĩa là
        // mật khẩu cũ và mật khẩu mới được bảo vệ ở hai mức khác nhau.
        assertThat(matcher.group()).startsWith("$2a$10$");
    }
}
