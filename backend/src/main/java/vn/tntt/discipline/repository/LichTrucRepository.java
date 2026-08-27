package vn.tntt.discipline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.tntt.discipline.entity.LichTruc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LichTrucRepository extends JpaRepository<LichTruc, UUID> {

    @Query("""
            SELECT l FROM LichTruc l
            JOIN FETCH l.toTruc
            WHERE l.namHoc.id = :namHocId
              AND l.ngayTruc BETWEEN :tuNgay AND :denNgay
            ORDER BY l.ngayTruc ASC, l.caTruc ASC
            """)
    List<LichTruc> trongKhoang(@Param("namHocId") UUID namHocId,
                               @Param("tuNgay") LocalDate tuNgay,
                               @Param("denNgay") LocalDate denNgay);

    /**
     * Ca trực của một ngày, kèm thành viên từng tổ.
     *
     * <p>Dùng để trả lời "hôm nay ai đang trực" — nền cho quy tắc phân quyền
     * "KY_LUAT đang trực ca" ở docs/04.
     */
    @Query("""
            SELECT DISTINCT l FROM LichTruc l
            JOIN FETCH l.toTruc t
            LEFT JOIN FETCH t.thanhVien
            WHERE l.ngayTruc = :ngay
            ORDER BY l.caTruc ASC
            """)
    List<LichTruc> theoNgay(@Param("ngay") LocalDate ngay);

    boolean existsByNgayTrucAndCaTrucAndToTrucId(LocalDate ngayTruc, String caTruc, UUID toTrucId);
}
