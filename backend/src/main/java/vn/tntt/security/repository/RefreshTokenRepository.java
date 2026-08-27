package vn.tntt.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.tntt.security.entity.RefreshToken;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /** Nạp kèm người dùng để tránh thêm một truy vấn ở mỗi lần làm mới token. */
    @Query("""
            SELECT r FROM RefreshToken r
            JOIN FETCH r.nguoiDung
            WHERE r.maBam = :maBam
            """)
    Optional<RefreshToken> timTheoMaBam(@Param("maBam") String maBam);

    /**
     * Thu hồi TẤT CẢ token của một người.
     *
     * <p>Gọi khi đổi mật khẩu: người dùng đổi mật khẩu thường là vì nghi ngờ
     * bị lộ, nên mọi phiên đang mở ở nơi khác phải chết theo. Không làm việc
     * này thì kẻ đã lấy được token cũ vẫn dùng tiếp được đủ 7 ngày.
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken r SET r.thuHoiLuc = :thoiDiem
            WHERE r.nguoiDung.id = :nguoiDungId AND r.thuHoiLuc IS NULL
            """)
    int thuHoiTatCa(@Param("nguoiDungId") UUID nguoiDungId,
                    @Param("thoiDiem") OffsetDateTime thoiDiem);

    /** Dọn rác: xoá token đã hết hạn từ lâu. Gọi định kỳ ở Sprint 8. */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.hetHanLuc < :moc")
    int xoaTokenCu(@Param("moc") OffsetDateTime moc);
}
