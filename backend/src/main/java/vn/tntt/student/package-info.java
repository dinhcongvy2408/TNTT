/**
 * <b>Module: Hồ sơ thiếu nhi</b>
 *
 * <p>Làm MỘT PHẦN ở Sprint 7 vì phiếu ra cổng cần có người để viết phiếu —
 * docs/99 mục H1.
 *
 * <p><b>Đã có:</b> {@code ThieuNhi} entity, CRUD, phân trang, mã tự sinh
 * {@code TN2026001}, xoá MỀM.
 *
 * <p><b>Còn lại của Sprint 4:</b> lịch sử bí tích (bảng {@code bi_tich}),
 * import Excel 1.000 dòng, tìm kiếm không dấu bằng chỉ mục GIN, và ghi
 * {@code nhat_ky_he_thong}.
 *
 * <p><b>Đây là dữ liệu cá nhân của người dưới 18 tuổi.</b> Mọi thay đổi trong
 * module này chịu ràng buộc CLAUDE.md mục 6: xoá mềm chứ không xoá cứng, không
 * log số điện thoại phụ huynh và ngày sinh, phân quyền kiểm ở tầng service.
 *
 * <p>Bảng phụ trách: {@code thieu_nhi}, {@code bi_tich}
 */
package vn.tntt.student;
