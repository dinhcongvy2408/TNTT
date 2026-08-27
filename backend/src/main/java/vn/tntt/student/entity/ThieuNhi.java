package vn.tntt.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.common.entity.BaseEntity;

import java.time.LocalDate;

/**
 * Hồ sơ một em thiếu nhi.
 *
 * <p>Đây là dữ liệu cá nhân của người dưới 18 tuổi — mọi thứ trong lớp này
 * chịu ràng buộc của CLAUDE.md mục 6. Hai điều quan trọng nhất:
 * <ul>
 *   <li><b>Xoá mềm.</b> Cột {@code da_xoa} thay cho việc xoá cứng. Hồ sơ một
 *       em đã nghỉ vẫn phải giữ, vì điểm danh và điểm số các năm trước đều trỏ
 *       tới id này qua {@code ghi_danh}.</li>
 *   <li><b>Không log.</b> Số điện thoại phụ huynh và ngày sinh không được đưa
 *       vào application log. Vì thế lớp này KHÔNG có {@code @ToString} của
 *       Lombok — một dòng {@code log.info("{}", em)} vô ý là đủ để đưa dữ
 *       liệu của trẻ vào file log rồi copy đi khắp nơi.</li>
 * </ul>
 */
@Entity
@Table(name = "thieu_nhi")
@Getter
@Setter
@NoArgsConstructor
public class ThieuNhi extends BaseEntity {

    /** "TN2026001" — duy nhất toàn hệ thống, do service sinh. */
    @Column(name = "ma_thieu_nhi", nullable = false, length = 20, unique = true)
    private String maThieuNhi;

    @Column(name = "ten_thanh", length = 50)
    private String tenThanh;

    @Column(name = "ho_ten", nullable = false, length = 120)
    private String hoTen;

    /**
     * Ngày sinh.
     *
     * <p>Ràng buộc "không được ở tương lai" nằm ở DTO bằng {@code @Past}, KHÔNG
     * ở DB: PostgreSQL từ chối {@code CHECK (ngay_sinh <= CURRENT_DATE)} vì
     * {@code CURRENT_DATE} không phải hàm IMMUTABLE. Xem docs/99 mục A1.
     */
    @Column(name = "ngay_sinh", nullable = false)
    private LocalDate ngaySinh;

    @Column(name = "gioi_tinh", length = 10)
    private String gioiTinh;

    @Column(name = "ten_bo", length = 120)
    private String tenBo;

    @Column(name = "ten_me", length = 120)
    private String tenMe;

    @Column(name = "sdt_phu_huynh", length = 20)
    private String sdtPhuHuynh;

    @Column(name = "dia_chi", columnDefinition = "text")
    private String diaChi;

    @Column(name = "giao_ho", length = 80)
    private String giaoHo;

    @Column(name = "ghi_chu", columnDefinition = "text")
    private String ghiChu;

    /** Xoá mềm. Mọi truy vấn nghiệp vụ phải lọc {@code da_xoa = false}. */
    @Column(name = "da_xoa", nullable = false)
    private boolean daXoa = false;

    /** "Giuse Nguyễn Văn A" — dùng khi hiển thị và khi đẩy tin WebSocket. */
    public String tenDayDu() {
        return tenThanh == null || tenThanh.isBlank() ? hoTen : tenThanh + " " + hoTen;
    }
}
