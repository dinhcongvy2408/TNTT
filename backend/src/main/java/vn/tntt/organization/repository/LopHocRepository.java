package vn.tntt.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.tntt.organization.entity.LopHoc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LopHocRepository extends JpaRepository<LopHoc, UUID> {

    /**
     * Danh sách lớp, lọc theo năm học và (tuỳ chọn) ngành.
     *
     * <p><b>{@code JOIN FETCH} là phần đáng chú ý.</b> Quan hệ tới
     * {@code nganh} và {@code namHoc} khai LAZY, nên nếu chỉ viết
     * {@code findByNamHocId} rồi đọc {@code lop.getNganh().getTenNganh()} ở
     * service, Hibernate sẽ bắn thêm một câu SELECT cho MỖI lớp — 40 lớp
     * thành 41 câu truy vấn.
     *
     * <p>{@code JOIN FETCH} bảo Hibernate lấy hết trong MỘT câu duy nhất. Đây
     * là cách sửa N+1 tường minh: nhìn vào truy vấn là biết nó nạp những gì,
     * không phụ thuộc vào việc service có vô tình chạm tới field nào hay không.
     *
     * <p>{@code :nganhId IS NULL OR ...} cho phép một truy vấn phục vụ cả hai
     * trường hợp có lọc và không lọc ngành, khỏi phải viết hai phương thức.
     */
    @Query("""
            SELECT l FROM LopHoc l
            JOIN FETCH l.nganh
            JOIN FETCH l.namHoc
            WHERE l.namHoc.id = :namHocId
              AND (:nganhId IS NULL OR l.nganh.id = :nganhId)
            ORDER BY l.nganh.thuTu ASC, l.tenLop ASC
            """)
    List<LopHoc> timTheoNamHoc(@Param("namHocId") UUID namHocId,
                              @Param("nganhId") UUID nganhId);

    /** Nạp kèm ngành và năm học, dùng khi trả về một lớp sau khi tạo/sửa. */
    @Query("""
            SELECT l FROM LopHoc l
            JOIN FETCH l.nganh
            JOIN FETCH l.namHoc
            WHERE l.id = :id
            """)
    Optional<LopHoc> timTheoIdKemQuanHe(@Param("id") UUID id);

    boolean existsByTenLopAndNamHocId(String tenLop, UUID namHocId);

    /** Dùng khi đổi tên lớp: bỏ qua chính bản ghi đang sửa. */
    boolean existsByTenLopAndNamHocIdAndIdNot(String tenLop, UUID namHocId, UUID id);

    /**
     * Đếm số ghi danh đang trỏ vào lớp này.
     *
     * <p>Viết bằng native query vì Sprint 5 mới có entity {@code GhiDanh}. Chờ
     * tới đó thì muộn: khoá ngoại {@code ghi_danh.lop_id} khai
     * {@code ON DELETE CASCADE}, nên xoá một lớp đã có học sinh sẽ cuốn theo
     * toàn bộ ghi danh, rồi điểm danh, rồi điểm số của các em — im lặng, không
     * một cảnh báo. Đây là hàng rào chặn việc đó.
     */
    @Query(value = "SELECT count(*) FROM ghi_danh WHERE lop_id = :lopId",
           nativeQuery = true)
    long demGhiDanh(@Param("lopId") UUID lopId);
}
