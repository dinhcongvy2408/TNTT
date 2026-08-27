/**
 * <b>Module: Ban Kỷ luật và trực cổng</b>
 *
 * <p>Hoàn tất ở <b>Sprint 7</b> — docs/02 mục 6, docs/99 mục H.
 *
 * <p><b>Luồng chính:</b>
 * <pre>
 * [Giáo lý viên]          [Server]              [Người trực cổng]
 *   tạo phiếu ────────────▶ lưu CHO_RA_CONG
 *                             └── WebSocket ──────▶ chuông kêu
 *                          ghi DA_RA_CONG ◀──────── bấm Xác nhận
 * </pre>
 *
 * <p>Bảng phụ trách: {@code to_truc}, {@code thanh_vien_to_truc},
 * {@code lich_truc}, {@code phieu_ra_cong}
 *
 * <p><b>Đọc trước khi sửa:</b> {@code PhieuRaCongService.dayTinSauKhiCommit}.
 * Bản tin WebSocket phải đẩy SAU KHI transaction commit; gọi thẳng thì
 * rollback ở bước sau sẽ khiến màn hình trực cổng kêu chuông cho một phiếu
 * không tồn tại.
 *
 * <p><b>Còn nợ:</b> quyền "KY_LUAT đang trực ca" mới làm một nửa — hiện mọi
 * tài khoản KY_LUAT đều xác nhận được phiếu, chưa đối chiếu {@code lich_truc}
 * của hôm nay (docs/99 mục H4). Và chưa có job cuối ngày tự huỷ phiếu chưa
 * xác nhận (docs/05 Sprint 7).
 */
package vn.tntt.discipline;
