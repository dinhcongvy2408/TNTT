package vn.tntt.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.common.entity.BaseEntity;

/**
 * Ngành trong xứ đoàn: Chiên Con, Ấu Nhi, Thiếu Nhi, Nghĩa Sĩ, Hiệp Sĩ.
 *
 * <p>Đây là <b>dữ liệu gốc</b> (reference data), đã được seed sẵn ở migration
 * V2. Khác với năm học hay lớp học, ngành gần như không bao giờ đổi — nó là
 * cơ cấu của phong trào Thiếu Nhi Thánh Thể, không phải của riêng xứ đoàn này.
 *
 * <p><b>{@code thuTu} quan trọng hơn vẻ ngoài của nó.</b> Đó là thứ tự chuyển
 * cấp: em Đạt ở ngành {@code thuTu = n} thì sang năm lên ngành {@code n + 1}
 * (docs/02 mục 6). Vì vậy nó UNIQUE ở tầng DB — hai ngành cùng thứ tự thì
 * thuật toán chuyển cấp không biết đẩy các em đi đâu.
 */
@Entity
@Table(name = "nganh")
@Getter
@Setter
@NoArgsConstructor
public class Nganh extends BaseEntity {

    /** "Ấu Nhi" — tên hiển thị cho người đọc. */
    @Column(name = "ten_nganh", nullable = false, length = 50, unique = true)
    private String tenNganh;

    /** "AU_NHI" — mã ổn định cho code so sánh, không đổi khi đổi tên hiển thị. */
    @Column(name = "ma_nganh", nullable = false, length = 20, unique = true)
    private String maNganh;

    /**
     * Kiểu {@link Short} chứ không phải {@code Integer}, vì cột trong DB là
     * {@code SMALLINT} (int2). Dùng Integer thì {@code ddl-auto: validate} báo
     * lệch kiểu ngay lúc khởi động — và nó đúng: hai kiểu này khác nhau về
     * kích thước lưu trữ.
     */
    @Column(name = "tuoi_toi_thieu", nullable = false)
    private Short tuoiToiThieu;

    @Column(name = "tuoi_toi_da", nullable = false)
    private Short tuoiToiDa;

    /** Thứ tự chuyển cấp. Duy nhất toàn bảng. */
    @Column(name = "thu_tu", nullable = false, unique = true)
    private Short thuTu;
}
