package vn.tntt.student.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.tntt.student.entity.ThieuNhi;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThieuNhiRepository extends JpaRepository<ThieuNhi, UUID> {

    /**
     * Danh sách hồ sơ CHƯA xoá, có tìm theo tên.
     *
     * <p>Dùng {@code LIKE} với {@code lower()} chứ chưa dùng chỉ mục GIN
     * {@code f_unaccent} đã tạo ở V1. Lý do: tìm không dấu cần viết truy vấn
     * native khớp CHÍNH XÁC biểu thức của index, viết khác đi là PostgreSQL bỏ
     * qua index và quét toàn bảng — tệ hơn cả không có index (docs/99 mục A2).
     * Việc đó thuộc phần Sprint 4 đầy đủ; ở lát cắt này vài trăm hồ sơ thì
     * {@code LIKE} vẫn nhanh.
     *
     * <p><b>Vì sao dùng chuỗi rỗng thay vì null cho "không lọc"?</b> Viết
     * {@code :tuKhoa IS NULL OR ...} thì JDBC không suy ra được kiểu của tham
     * số null, gửi xuống PostgreSQL dưới dạng {@code bytea}, và câu truy vấn
     * chết với {@code function lower(bytea) does not exist}. Chuỗi rỗng luôn
     * có kiểu rõ ràng nên không có chỗ để đoán sai.
     */
    @Query("""
            SELECT t FROM ThieuNhi t
            WHERE t.daXoa = false
              AND ( :tuKhoa = ''
                    OR lower(t.hoTen)      LIKE lower(concat('%', :tuKhoa, '%'))
                    OR lower(t.maThieuNhi) LIKE lower(concat('%', :tuKhoa, '%')) )
            ORDER BY t.hoTen ASC
            """)
    Page<ThieuNhi> tim(@Param("tuKhoa") String tuKhoa, Pageable pageable);

    /** Tìm theo id, bỏ qua hồ sơ đã xoá mềm. */
    @Query("SELECT t FROM ThieuNhi t WHERE t.id = :id AND t.daXoa = false")
    Optional<ThieuNhi> timChuaXoa(@Param("id") UUID id);

    /**
     * Mã lớn nhất đang dùng cho một năm, để sinh mã kế tiếp.
     *
     * <p>Sắp theo chuỗi được vì mã có độ dài cố định và số đã đệm số 0
     * ("TN2026009" &lt; "TN2026010").
     */
    @Query("""
            SELECT max(t.maThieuNhi) FROM ThieuNhi t
            WHERE t.maThieuNhi LIKE concat(:tienTo, '%')
            """)
    Optional<String> maLonNhat(@Param("tienTo") String tienTo);
}
