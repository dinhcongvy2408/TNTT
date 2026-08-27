package vn.tntt.discipline.entity;

/**
 * Máy trạng thái của phiếu ra cổng (docs/02 mục 6.2).
 *
 * <pre>
 * CHO_RA_CONG ──xác nhận──▶ DA_RA_CONG
 *      │
 *      └──────huỷ─────────▶ HUY
 * </pre>
 *
 * <p>Hai trạng thái cuối là ĐIỂM DỪNG: phiếu đã ra cổng hoặc đã huỷ thì không
 * đổi được nữa. Cho quay lại nghĩa là có thể "mở lại" một phiếu mà em đã ra
 * khỏi cổng từ lâu — không có nghĩa gì, và làm hỏng dấu vết truy trách nhiệm.
 *
 * <p>Ràng buộc {@code ck_phieu_xac_nhan} ở migration V1 ép ở tầng DB: đã
 * DA_RA_CONG thì bắt buộc phải có {@code thoi_gian_ra_cong} VÀ
 * {@code nguoi_xac_nhan_id}. Code không thể quên set một trong hai.
 */
public enum TrangThaiPhieu {
    CHO_RA_CONG,
    DA_RA_CONG,
    HUY
}
