package vn.tntt.discipline.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.tntt.discipline.entity.ToTruc;

import java.util.List;
import java.util.UUID;

@Repository
public interface ToTrucRepository extends JpaRepository<ToTruc, UUID> {

    boolean existsByTenTo(String tenTo);

    /**
     * {@code LEFT JOIN FETCH} chứ không {@code JOIN FETCH}: tổ mới tạo chưa có
     * thành viên nào, và {@code JOIN} thường sẽ loại nó khỏi kết quả — người
     * dùng vừa tạo tổ xong đã không thấy nó đâu.
     *
     * <p>{@code DISTINCT} vì fetch một quan hệ N-N làm mỗi tổ xuất hiện một
     * lần cho mỗi thành viên.
     */
    @Query("SELECT DISTINCT t FROM ToTruc t LEFT JOIN FETCH t.thanhVien ORDER BY t.tenTo")
    List<ToTruc> tatCaKemThanhVien();
}
