package vn.tntt.personnel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.tntt.personnel.entity.NguoiDung;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, UUID> {

    /**
     * Tìm theo email HOẶC số điện thoại — docs/02 bước 2 cho phép đăng nhập
     * bằng cả hai, và người dùng chỉ gõ vào MỘT ô duy nhất trên màn hình.
     *
     * <p>Viết một truy vấn thay vì gọi hai lần là để không lộ thông tin qua
     * thời gian phản hồi: hai lần truy vấn nối tiếp sẽ chậm hơn rõ rệt khi
     * email không tồn tại, và đó là một kênh rò rỉ.
     */
    @Query("""
            SELECT n FROM NguoiDung n
            WHERE n.email = :dinhDanh OR n.soDienThoai = :dinhDanh
            """)
    Optional<NguoiDung> timTheoDinhDanh(@Param("dinhDanh") String dinhDanh);
}
