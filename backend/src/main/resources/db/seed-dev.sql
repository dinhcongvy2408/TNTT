-- =====================================================================
-- DỮ LIỆU THỬ NGHIỆM CHO MÔI TRƯỜNG DEV
--
-- !!! FILE NÀY KHÔNG PHẢI MIGRATION FLYWAY !!!
--
-- Nó nằm ở db/ chứ KHÔNG nằm trong db/migration/, và đó là chủ đích: Flyway
-- chạy mọi file trong db/migration/ ở MỌI môi trường, kể cả production. Bốn
-- tài khoản dưới đây có mật khẩu công khai trong repo — đưa vào migration là
-- tự tạo bốn cửa hậu trên máy chủ thật.
--
-- CÁCH CHẠY:
--   docker exec -i tntt-postgres psql -U tntt -d tntt \
--     < backend/src/main/resources/db/seed-dev.sql
--
-- Chạy lại nhiều lần được: mọi câu đều ON CONFLICT DO NOTHING.
--
-- Vì sao cần file này? Chưa có màn hình quản lý tài khoản (Sprint 3), mà muốn
-- thử phân quyền thì phải có đủ bốn vai trò. Khi Sprint 3 xong, file này chỉ
-- còn dùng để dựng nhanh môi trường thử.
-- =====================================================================

-- --------------------------------------------------------------------
-- Bốn tài khoản, mỗi tài khoản một vai trò trong ma trận ở docs/02
-- --------------------------------------------------------------------
--
-- can_doi_mat_khau = false: các tài khoản THỬ không bị bắt đổi mật khẩu, để
-- đăng nhập phát là dùng được ngay. Tài khoản admin thật ở migration V4 thì
-- vẫn để true — đó là hàng rào cho môi trường thật.
--
-- Mật khẩu băm bằng pgcrypto (đã bật từ V1) với BCrypt cost 10, khớp
-- BCryptPasswordEncoder(10) trong SecurityConfig.

INSERT INTO nguoi_dung (ten_thanh, ho_ten, email, mat_khau_hash, can_doi_mat_khau, dang_hoat_dong)
VALUES
 ('Maria',  'Trần Thị Huynh Trưởng', 'huynhtruong@xudoan.local',
  crypt('Huynh@123',  gen_salt('bf', 10)), false, true),

 ('Phêrô',  'Lê Văn Kỷ Luật',        'kyluat@xudoan.local',
  crypt('KyLuat@123', gen_salt('bf', 10)), false, true),

 ('Anna',   'Phạm Thị Khối Trưởng',  'khoitruong@xudoan.local',
  crypt('Khoi@123',   gen_salt('bf', 10)), false, true),

 ('Gioan',  'Vũ Văn Nhiều Vai',      'daivai@xudoan.local',
  crypt('DaiVai@123', gen_salt('bf', 10)), false, true)
ON CONFLICT (email) DO NOTHING;

-- --------------------------------------------------------------------
-- Gán vai trò
-- --------------------------------------------------------------------
-- Viết bằng SELECT thay vì hardcode UUID: id do gen_random_uuid() sinh, mỗi
-- lần cài mỗi khác.

INSERT INTO nguoi_dung_vai_tro (nguoi_dung_id, vai_tro_id)
SELECT u.id, r.id
FROM nguoi_dung u
CROSS JOIN vai_tro r
WHERE (u.email = 'huynhtruong@xudoan.local' AND r.ma = 'HUYNH_TRUONG')
   OR (u.email = 'kyluat@xudoan.local'      AND r.ma = 'KY_LUAT')
   OR (u.email = 'khoitruong@xudoan.local'  AND r.ma = 'KHOI_TRUONG')
   -- Tài khoản "nhiều vai" để thử trường hợp một người kiêm nhiều việc —
   -- rất phổ biến ở xứ đoàn nhỏ, và docs/02 bước 2 nói rõ "một người có thể
   -- nhiều vai trò". Đây là chỗ dễ sinh lỗi nhất khi kiểm quyền.
   OR (u.email = 'daivai@xudoan.local'      AND r.ma IN ('HUYNH_TRUONG', 'KY_LUAT'))
ON CONFLICT DO NOTHING;

-- --------------------------------------------------------------------
-- Kiểm chứng
-- --------------------------------------------------------------------
SELECT u.email,
       string_agg(r.ma, ', ' ORDER BY r.ma) AS vai_tro,
       u.can_doi_mat_khau
FROM nguoi_dung u
LEFT JOIN nguoi_dung_vai_tro nv ON nv.nguoi_dung_id = u.id
LEFT JOIN vai_tro r ON r.id = nv.vai_tro_id
GROUP BY u.email, u.can_doi_mat_khau
ORDER BY u.email;
