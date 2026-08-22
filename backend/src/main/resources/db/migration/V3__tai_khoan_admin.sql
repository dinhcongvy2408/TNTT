-- =====================================================================
-- V3 — Tài khoản quản trị đầu tiên
--
-- Tách riêng khỏi V2 CÓ CHỦ ĐÍCH: đây là file duy nhất chứa credential.
-- Khi deploy production, đây là file bạn phải nhìn lại trước tiên.
--
-- Mật khẩu mặc định: Admin@123
-- Hash BCrypt cost 10 lấy nguyên từ schema.sql gốc.
--
-- !!! TRƯỚC KHI DEPLOY PRODUCTION !!!
-- Hash này đã nằm công khai trong repo, coi như mật khẩu đã bị lộ.
-- Cột can_doi_mat_khau = true buộc đổi ở lần đăng nhập đầu (Sprint 1),
-- nhưng đó chỉ là hàng rào sau. Việc đúng phải làm:
--   1. Sinh hash mới:
--        htpasswd -bnBC 10 "" 'MatKhauThatCuaBan' | tr -d ':\n'
--   2. Viết migration MỚI (VD V9__doi_mat_khau_admin.sql) để UPDATE,
--      KHÔNG sửa file này — file đã chạy là bất biến với Flyway.
-- =====================================================================

INSERT INTO nguoi_dung (ten_thanh, ho_ten, email, mat_khau_hash, can_doi_mat_khau)
VALUES ('Giuse', 'Quản trị viên', 'admin@xudoan.local',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true)
ON CONFLICT (email) DO NOTHING;

-- Gán vai trò ADMIN. Viết bằng SELECT thay vì hardcode UUID vì id được
-- gen_random_uuid() sinh ra, mỗi lần cài mỗi khác.
INSERT INTO nguoi_dung_vai_tro (nguoi_dung_id, vai_tro_id)
SELECT u.id, r.id
FROM nguoi_dung u
CROSS JOIN vai_tro r
WHERE u.email = 'admin@xudoan.local' AND r.ma = 'ADMIN'
ON CONFLICT DO NOTHING;
