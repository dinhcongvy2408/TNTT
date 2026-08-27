package vn.tntt.personnel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.common.entity.BaseEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Tài khoản huynh trưởng / ban điều hành.
 *
 * <p>Thiếu nhi và phụ huynh KHÔNG có tài khoản ở giai đoạn 1 (CLAUDE.md mục 1),
 * nên bảng này chỉ khoảng 150 dòng.
 */
@Entity
@Table(name = "nguoi_dung")
@Getter
@Setter
@NoArgsConstructor
public class NguoiDung extends BaseEntity {

    @Column(name = "ten_thanh", length = 50)
    private String tenThanh;

    @Column(name = "ho_ten", nullable = false, length = 120)
    private String hoTen;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    /** Đăng nhập bằng email HOẶC số điện thoại — DB đòi có ít nhất một. */
    @Column(name = "email", length = 120, unique = true)
    private String email;

    @Column(name = "so_dien_thoai", length = 20, unique = true)
    private String soDienThoai;

    /**
     * BCrypt hash. TUYỆT ĐỐI không log ra (CLAUDE.md mục 6).
     *
     * <p>Cũng vì thế lớp này KHÔNG dùng {@code @ToString} của Lombok: một
     * dòng {@code log.info("{}", nguoiDung)} vô tình là hash nằm trong file
     * log, và file log thì được copy đi copy lại khắp nơi.
     */
    @Column(name = "mat_khau_hash", nullable = false, length = 100)
    private String matKhauHash;

    /** true = phải đổi mật khẩu ngay lần đăng nhập đầu (docs/02 bước 2). */
    @Column(name = "can_doi_mat_khau", nullable = false)
    private boolean canDoiMatKhau = true;

    /**
     * false = khoá tài khoản. Dùng thay cho việc XOÁ người dùng: huynh trưởng
     * nghỉ vẫn phải giữ hồ sơ, vì bảng nhat_ky_he_thong và các bản ghi điểm
     * danh cũ đều trỏ tới id của họ.
     */
    @Column(name = "dang_hoat_dong", nullable = false)
    private boolean dangHoatDong = true;

    @Column(name = "lan_dang_nhap_cuoi")
    private OffsetDateTime lanDangNhapCuoi;

    /**
     * {@code FetchType.EAGER} — ngoại lệ có chủ đích so với quy tắc "luôn LAZY".
     *
     * <p>Vai trò được đọc ở MỌI request (để dựng {@code UserDetails}), và mỗi
     * người chỉ có 1-2 vai trò. Nạp LAZY ở đây nghĩa là thêm một truy vấn
     * riêng cho mỗi lần xác thực, mà không tiết kiệm được gì.
     *
     * <p>Được phép làm vậy vì tập dữ liệu nhỏ và bị chặn: 150 người dùng, mỗi
     * người vài vai trò. Với quan hệ có thể phình to (lớp học, ghi danh) thì
     * EAGER luôn sai.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "nguoi_dung_vai_tro",
            joinColumns = @JoinColumn(name = "nguoi_dung_id"),
            inverseJoinColumns = @JoinColumn(name = "vai_tro_id"))
    private Set<VaiTro> vaiTro = new HashSet<>();

    /** Tên để hiển thị: "Giuse Nguyễn Văn A". */
    public String tenDayDu() {
        return tenThanh == null || tenThanh.isBlank() ? hoTen : tenThanh + " " + hoTen;
    }
}
