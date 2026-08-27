package vn.tntt.enrollment.entity;

/**
 * Trạng thái một lượt ghi danh, khớp ràng buộc CHECK ở migration V1.
 *
 * <p>Chỉ {@link #DANG_HOC} là "đang chiếm chỗ": partial unique index
 * {@code uq_ghi_danh_dang_hoc} chỉ áp cho các dòng ở trạng thái này, nên một
 * em có thể có nhiều dòng NGHI_HOC trong cùng năm nhưng chỉ một dòng DANG_HOC.
 */
public enum TrangThaiGhiDanh {

    /** Đang theo học. Tối đa MỘT dòng như vậy cho mỗi em trong mỗi năm học. */
    DANG_HOC,

    /** Chuyển sang xứ đoàn khác giữa chừng. */
    CHUYEN_XU,

    /** Nghỉ học giữa chừng. */
    NGHI_HOC,

    /** Học hết năm. Đặt khi kết thúc năm học. */
    HOAN_THANH
}
