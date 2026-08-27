-- =====================================================================
-- V4 — Sửa mật khẩu tài khoản quản trị
--
-- VÌ SAO CÓ FILE NÀY. Hash trong V3 được chép từ schema.sql gốc kèm chú
-- thích "mật khẩu Admin@123". Chú thích đó SAI: đem hash đó đi đối chiếu
-- bằng pgcrypto thì không khớp 'Admin@123', cũng không khớp bất kỳ mật
-- khẩu phổ biến nào. Nghĩa là tài khoản admin không đăng nhập được — và
-- ta sẽ chỉ phát hiện ra giữa Sprint 1, lúc đang bận việc khác.
--
-- Cách tự kiểm chứng lại (pgcrypto đã bật sẵn ở V1):
--   SELECT crypt('Admin@123', mat_khau_hash) = mat_khau_hash
--   FROM nguoi_dung WHERE email = 'admin@xudoan.local';
--
-- KHÔNG sửa V3. Flyway ghi checksum của mỗi file đã chạy; sửa file cũ là
-- app không khởi động nữa. Muốn đổi thì viết file mới — chính là file này.
--
-- Hash bên dưới sinh bằng crypt('Admin@123', gen_salt('bf', 10)), đã đối
-- chiếu lại: khớp 'Admin@123', từ chối mật khẩu sai.
-- Tiền tố $2a$ khớp mặc định của Spring BCryptPasswordEncoder.
--
-- !!! TRƯỚC KHI DEPLOY PRODUCTION !!!
-- Hash này nằm công khai trong repo → coi như mật khẩu đã lộ. can_doi_mat_khau
-- = true buộc đổi ở lần đăng nhập đầu (Sprint 1), nhưng đó chỉ là hàng rào
-- sau. Việc đúng phải làm: viết một migration MỚI để UPDATE hash thật.
-- =====================================================================

UPDATE nguoi_dung
SET mat_khau_hash    = '$2a$10$5LxGuXBz7WNcahHxu5J38ufZYqucvoNi3z912Q18Iiggh5gLL3h26',
    can_doi_mat_khau = true,
    ngay_cap_nhat    = now()
WHERE email = 'admin@xudoan.local';
