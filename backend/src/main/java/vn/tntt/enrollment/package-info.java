/**
 * <b>Module: Ghi danh</b>
 *
 * <p>Làm MỘT PHẦN ở Sprint 7 — docs/99 mục H2.
 *
 * <p><b>Đã có:</b> {@code GhiDanh} — xếp một em vào một lớp của một năm học,
 * kèm quy tắc "một em chỉ có tối đa một lớp DANG_HOC mỗi năm".
 *
 * <p><b>Còn lại của Sprint 5:</b> điểm danh hằng tuần ({@code diem_danh}).
 *
 * <p>Cột {@code nam_hoc_id} là dữ liệu LẶP có chủ đích (docs/99 mục B1): không
 * có nó thì quy tắc trên không ép được bằng ràng buộc DB. Khoá ngoại GHÉP
 * {@code (lop_id, nam_hoc_id)} khoá chặt nguy cơ hai cột lệch nhau — vì thế
 * service LUÔN lấy {@code nam_hoc_id} từ lớp, không nhận từ client.
 *
 * <p>Bảng phụ trách: {@code ghi_danh}, {@code diem_danh}
 */
package vn.tntt.enrollment;
