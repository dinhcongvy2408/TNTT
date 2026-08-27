package vn.tntt.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tntt.organization.entity.NamHoc;
import vn.tntt.organization.entity.TrangThaiNamHoc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy vấn bảng {@code nam_hoc}.
 *
 * <p>Ta không viết một dòng SQL nào ở đây: Spring Data đọc TÊN PHƯƠNG THỨC
 * và tự sinh câu truy vấn lúc khởi động. {@code findByTrangThai} →
 * {@code SELECT * FROM nam_hoc WHERE trang_thai = ?}.
 *
 * <p>Điểm mạnh thật sự của cách này không phải là gõ ít, mà là gõ SAI thì
 * app KHÔNG KHỞI ĐỘNG được. Viết nhầm {@code findByTrangThaii} là Spring
 * báo lỗi ngay lúc bật, chứ không phải tới lúc người dùng bấm nút.
 */
@Repository
public interface NamHocRepository extends JpaRepository<NamHoc, UUID> {

    /**
     * Tìm năm học đang hoạt động.
     *
     * <p>Trả {@link Optional} chứ không trả thẳng {@code NamHoc}: chưa có năm
     * nào được kích hoạt là chuyện BÌNH THƯỜNG (ngày đầu cài đặt hệ thống),
     * không phải lỗi. Optional buộc nơi gọi phải xử lý trường hợp đó.
     *
     * <p>An toàn khi trả về một kết quả duy nhất là nhờ partial unique index
     * {@code uq_nam_hoc_dang_hoat_dong} ở tầng DB — xem migration V1.
     */
    Optional<NamHoc> findByTrangThai(TrangThaiNamHoc trangThai);

    boolean existsByTenNamHoc(String tenNamHoc);

    /**
     * Năm mới nhất lên đầu. Người dùng gần như luôn quan tâm năm gần nhất,
     * nên sắp xếp sẵn ở DB rẻ hơn để frontend tự sắp.
     */
    List<NamHoc> findAllByOrderByNgayBatDauDesc();
}
