package vn.tntt.organization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.common.entity.BaseEntity;

/**
 * Một lớp học trong một năm học, VD "Ấu 1A" của năm 2026-2027.
 *
 * <p>Lớp học KHÔNG sống qua năm: sang năm học mới phải tạo lớp mới, vì
 * {@code nam_hoc_id} là một phần định danh nghiệp vụ của lớp. Nhờ vậy dữ liệu
 * điểm danh và điểm số của từng năm nằm tách biệt, không lẫn vào nhau.
 */
@Entity
@Table(name = "lop_hoc")
@Getter
@Setter
@NoArgsConstructor
public class LopHoc extends BaseEntity {

    /** "Ấu 1A". Duy nhất trong phạm vi MỘT năm học, không phải toàn bảng. */
    @Column(name = "ten_lop", nullable = false, length = 50)
    private String tenLop;

    /**
     * {@code FetchType.LAZY} — và đây là quyết định quan trọng nhất trong lớp này.
     *
     * <p>Mặc định của {@code @ManyToOne} là EAGER: mỗi lần nạp một lớp học,
     * Hibernate nạp luôn cả ngành và năm học kèm theo. Nạp danh sách 40 lớp
     * thì thành 1 + 40 + 40 câu truy vấn — bài toán N+1 kinh điển, và nó chỉ
     * lộ ra khi dữ liệu đã nhiều, tức là ở production.
     *
     * <p>Với LAZY, Hibernate chỉ nạp khi ta thật sự chạm tới. Nhưng
     * {@code open-in-view: false} (xem application.yml) đóng session ngay khi
     * controller trả về, nên chạm sau đó sẽ ném
     * {@code LazyInitializationException}. Nghe như phiền, thực ra là hàng rào:
     * nó buộc ta phải NÓI RÕ mình cần gì, bằng {@code JOIN FETCH} trong
     * repository. Xem {@code LopHocRepository}.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nganh_id", nullable = false)
    private Nganh nganh;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nam_hoc_id", nullable = false)
    private NamHoc namHoc;

    /** Ấu 1 / Ấu 2 / Ấu 3 — cấp trong ngành. SMALLINT nên là Short. */
    @Column(name = "cap_do", nullable = false)
    private Short capDo = 1;

    @Column(name = "ghi_chu", columnDefinition = "text")
    private String ghiChu;
}
