-- =====================================================================
-- V2 — Dữ liệu gốc (reference data)
--
-- Vì sao tách khỏi V1? V1 là CẤU TRÚC, V2 là NỘI DUNG. Sau này khi cần
-- thêm một ngành mới hay một khoá cấu hình mới, ta viết V4, V5... mà không
-- phải đụng vào file định nghĩa bảng. Đọc lịch sử migration cũng rõ ràng hơn.
--
-- ON CONFLICT DO NOTHING: để chạy lại an toàn nếu ai đó đã chèn tay.
-- =====================================================================

-- --------------------------- Vai trò ---------------------------------
INSERT INTO vai_tro (ma, ten_hien_thi, mo_ta) VALUES
 ('ADMIN',        'Quản trị viên / Ban điều hành', 'Toàn quyền trên toàn xứ đoàn'),
 ('KHOI_TRUONG',  'Trưởng ngành / Khối trưởng',    'Quản lý các lớp thuộc ngành phụ trách'),
 ('HUYNH_TRUONG', 'Huynh trưởng / Giáo lý viên',   'Quản lý lớp được phân công'),
 ('KY_LUAT',      'Thành viên Ban Kỷ luật',        'Trực cổng, xác nhận phiếu ra về')
ON CONFLICT (ma) DO NOTHING;

-- --------------------------- Ngành -----------------------------------
-- thu_tu quyết định thứ tự chuyển cấp: Đạt ở ngành thu_tu = n thì lên n+1
INSERT INTO nganh (ten_nganh, ma_nganh, tuoi_toi_thieu, tuoi_toi_da, thu_tu) VALUES
 ('Chiên Con',  'CHIEN_CON',  4,  6, 1),
 ('Ấu Nhi',     'AU_NHI',     7,  9, 2),
 ('Thiếu Nhi',  'THIEU_NHI', 10, 12, 3),
 ('Nghĩa Sĩ',   'NGHIA_SI',  13, 15, 4),
 ('Hiệp Sĩ',    'HIEP_SI',   16, 18, 5)
ON CONFLICT (ma_nganh) DO NOTHING;

-- --------------------------- Cấu hình --------------------------------
INSERT INTO cau_hinh (khoa, gia_tri, mo_ta) VALUES
 ('diem_dat_toi_thieu',    '5.0', 'Điểm trung bình tối thiểu để được lên lớp'),
 ('chuyen_can_toi_thieu',  '70',  'Tỉ lệ chuyên cần tối thiểu, tính bằng %'),
 ('he_so_hk1',             '1',   'Hệ số nhân của điểm học kỳ 1'),
 ('he_so_hk2',             '2',   'Hệ số nhân của điểm học kỳ 2 — CẦN XÁC NHẬN với ban điều hành'),
 ('so_ngay_sua_diem_danh', '7',   'Số ngày lùi tối đa được phép sửa điểm danh'),
 ('tien_to_ma_thieu_nhi',  'TN',  'Tiền tố mã định danh thiếu nhi, VD TN2026001')
ON CONFLICT (khoa) DO NOTHING;

-- Ghi chú cho Sprint 6:
-- Công thức trong docs/02 là (hk1 + hk2*2)/3. Ta lưu he_so_hk1 và he_so_hk2
-- riêng để công thức thành tổng quát:
--     diem_tb = (hk1*he_so_hk1 + hk2*he_so_hk2) / (he_so_hk1 + he_so_hk2)
-- Nhờ vậy nếu ban điều hành đổi sang hệ số 1:1 thì chỉ cần sửa một dòng
-- trong bảng cau_hinh, không phải build lại ứng dụng.
