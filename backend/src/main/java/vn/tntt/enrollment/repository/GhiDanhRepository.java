package vn.tntt.enrollment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.tntt.enrollment.entity.GhiDanh;
import vn.tntt.enrollment.entity.TrangThaiGhiDanh;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GhiDanhRepository extends JpaRepository<GhiDanh, UUID> {

    /** Sĩ số một lớp — truy vấn nóng nhất hệ thống vào sáng Chủ Nhật. */
    @Query("""
            SELECT g FROM GhiDanh g
            JOIN FETCH g.thieuNhi
            JOIN FETCH g.lopHoc
            WHERE g.lopHoc.id = :lopId AND g.trangThai = 'DANG_HOC'
            ORDER BY g.thieuNhi.hoTen ASC
            """)
    List<GhiDanh> danhSachLop(@Param("lopId") UUID lopId);

    /**
     * Lượt ghi danh ĐANG HỌC của một em trong một năm.
     *
     * <p>An toàn khi trả về tối đa một kết quả nhờ partial unique index
     * {@code uq_ghi_danh_dang_hoc}. Đây là truy vấn mà module phiếu ra cổng
     * dùng để biết em đang thuộc lớp nào.
     */
    @Query("""
            SELECT g FROM GhiDanh g
            JOIN FETCH g.lopHoc
            JOIN FETCH g.thieuNhi
            WHERE g.thieuNhi.id = :thieuNhiId
              AND g.namHoc.id = :namHocId
              AND g.trangThai = 'DANG_HOC'
            """)
    Optional<GhiDanh> dangHocTrongNam(@Param("thieuNhiId") UUID thieuNhiId,
                                      @Param("namHocId") UUID namHocId);

    boolean existsByThieuNhiIdAndNamHocIdAndTrangThai(
            UUID thieuNhiId, UUID namHocId, TrangThaiGhiDanh trangThai);

    @Query("SELECT count(g) FROM GhiDanh g WHERE g.lopHoc.id = :lopId AND g.trangThai = 'DANG_HOC'")
    long demDangHoc(@Param("lopId") UUID lopId);
}
