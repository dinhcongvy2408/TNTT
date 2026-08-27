package vn.tntt.discipline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.tntt.discipline.entity.PhieuRaCong;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhieuRaCongRepository extends JpaRepository<PhieuRaCong, UUID> {

    /**
     * Toàn bộ quan hệ cần cho một dòng trên màn hình trực cổng, nạp trong MỘT
     * câu truy vấn.
     *
     * <p>{@code LEFT JOIN FETCH} cho {@code ghiDanh} vì em chưa xếp lớp thì
     * cột đó NULL, và {@code JOIN} thường sẽ làm phiếu của em biến mất khỏi
     * màn hình trực — đúng lúc người trực cần thấy nhất.
     */
    @Query("""
            SELECT p FROM PhieuRaCong p
            JOIN FETCH p.thieuNhi
            JOIN FETCH p.nguoiTao
            LEFT JOIN FETCH p.ghiDanh g
            LEFT JOIN FETCH g.lopHoc
            LEFT JOIN FETCH p.nguoiXacNhan
            WHERE p.namHoc.id = :namHocId AND p.trangThai = 'CHO_RA_CONG'
            ORDER BY p.thoiGianTao ASC
            """)
    List<PhieuRaCong> dangCho(@Param("namHocId") UUID namHocId);

    @Query("""
            SELECT p FROM PhieuRaCong p
            JOIN FETCH p.thieuNhi
            JOIN FETCH p.nguoiTao
            LEFT JOIN FETCH p.ghiDanh g
            LEFT JOIN FETCH g.lopHoc
            LEFT JOIN FETCH p.nguoiXacNhan
            WHERE p.thoiGianTao BETWEEN :tuLuc AND :denLuc
            ORDER BY p.thoiGianTao DESC
            """)
    List<PhieuRaCong> lichSuTrongNgay(@Param("tuLuc") OffsetDateTime tuLuc,
                                      @Param("denLuc") OffsetDateTime denLuc);

    @Query("""
            SELECT p FROM PhieuRaCong p
            JOIN FETCH p.thieuNhi
            JOIN FETCH p.nguoiTao
            JOIN FETCH p.namHoc
            LEFT JOIN FETCH p.ghiDanh g
            LEFT JOIN FETCH g.lopHoc
            LEFT JOIN FETCH p.nguoiXacNhan
            WHERE p.id = :id
            """)
    Optional<PhieuRaCong> timKemQuanHe(@Param("id") UUID id);

    /**
     * Một em có đang chờ ra cổng không.
     *
     * <p>Chốt chặn thật là partial unique index {@code uq_phieu_dang_cho}; đây
     * chỉ để có thông báo tử tế. Quy tắc này quan trọng hơn vẻ ngoài: hai phiếu
     * cùng chờ cho một em nghĩa là màn hình trực cổng hiện tên em hai lần, và
     * người trực có thể cho em ra cổng rồi vẫn thấy còn một phiếu đang chờ.
     */
    boolean existsByThieuNhiIdAndTrangThai(UUID thieuNhiId,
                                           vn.tntt.discipline.entity.TrangThaiPhieu trangThai);
}
