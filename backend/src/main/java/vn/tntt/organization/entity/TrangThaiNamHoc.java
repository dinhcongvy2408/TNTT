package vn.tntt.organization.entity;

/**
 * Trạng thái của một năm học. Ba giá trị này khớp ĐÚNG với ràng buộc CHECK
 * trong migration V1:
 *
 * <pre>
 * CHECK (trang_thai IN ('CHUAN_BI','DANG_HOAT_DONG','DA_KET_THUC'))
 * </pre>
 *
 * <p>Thêm giá trị mới ở đây mà quên viết migration sửa CHECK thì PostgreSQL
 * sẽ từ chối lúc INSERT — và đó là điều tốt: DB không cho code ghi vào thứ
 * nó không hiểu.
 *
 * <p>Chuyển trạng thái là MỘT CHIỀU:
 * <pre>
 * CHUAN_BI ──kích hoạt──▶ DANG_HOAT_DONG ──kết thúc──▶ DA_KET_THUC
 * </pre>
 * Không có đường quay lại. Mở lại một năm đã kết thúc nghĩa là cho phép sửa
 * điểm và điểm danh của năm cũ — thứ docs/02 bước 1 cấm.
 */
public enum TrangThaiNamHoc {

    /** Đã tạo, đang nhập lớp và phân công, chưa vận hành. */
    CHUAN_BI,

    /** Đang chạy. Chỉ được có ĐÚNG MỘT năm học ở trạng thái này. */
    DANG_HOAT_DONG,

    /** Đã đóng sổ. Dữ liệu chỉ đọc. */
    DA_KET_THUC
}
