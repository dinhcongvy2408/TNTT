/**
 * <b>Module: Tổ chức xứ đoàn</b>
 *
 * <p>Triển khai ở <b>Sprint 2</b> — xem docs/05-lo-trinh.md.
 *
 * <p><b>Đã xong cả ba:</b> {@code NamHoc}, {@code Nganh}, {@code LopHoc}.
 * Máy trạng thái năm học là một chiều: CHUAN_BI to DANG_HOAT_DONG to
 * DA_KET_THUC. Endpoint kích hoạt không có trong docs/04, lý do ở docs/99 F1.
 *
 * <p>Ba hàng rào ở {@code LopHocService} nên đọc trước khi sửa gì trong module
 * này — chúng không có trong đặc tả nhưng thiếu là mất dữ liệu thật: năm học
 * đã kết thúc là chỉ đọc, không đổi năm học của một lớp đã tồn tại, và không
 * xoá lớp đang có ghi danh (khoá ngoại khai ON DELETE CASCADE). docs/99 F5.
 *
 * <p><b>Còn nợ sang Sprint 3:</b> {@code GET /lop/cua-toi} và lọc danh sách
 * lớp theo phân công của người đăng nhập — docs/99 mục F4.
 *
 * <p>Bảng dữ liệu phụ trách: {@code nam_hoc}, {@code nganh}, {@code lop_hoc}
 *
 * <p>Cấu trúc thư mục con theo quy ước ở CLAUDE.md mục 4:
 * {@code controller/ service/ repository/ entity/ dto/ mapper/}
 */
package vn.tntt.organization;
