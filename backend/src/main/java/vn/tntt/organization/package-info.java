/**
 * <b>Module: Tổ chức xứ đoàn</b>
 *
 * <p>Triển khai ở <b>Sprint 2</b> — xem docs/05-lo-trinh.md.
 *
 * <p><b>Đã xong:</b> {@code NamHoc} — entity, repository, service, controller.
 * Máy trạng thái một chiều CHUAN_BI to DANG_HOAT_DONG to DA_KET_THUC.
 * Endpoint kích hoạt không có trong docs/04, lý do ở docs/99 mục F1.
 *
 * <p><b>Còn lại:</b> {@code Nganh} (đã seed ở migration V2, cần API đọc) và
 * {@code LopHoc} (CRUD, tên lớp duy nhất trong phạm vi một năm học).
 *
 * <p>Bảng dữ liệu phụ trách: {@code nam_hoc}, {@code nganh}, {@code lop_hoc}
 *
 * <p>Cấu trúc thư mục con theo quy ước ở CLAUDE.md mục 4:
 * {@code controller/ service/ repository/ entity/ dto/ mapper/}
 */
package vn.tntt.organization;
