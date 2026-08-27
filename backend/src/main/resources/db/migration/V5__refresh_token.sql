-- =====================================================================
-- V5 — Bảng lưu refresh token
--
-- VÌ SAO CẦN BẢNG NÀY. docs/04 quy định `POST /auth/logout` phải "thu hồi
-- refresh token". Muốn thu hồi được thì server phải NHỚ token nào còn hiệu
-- lực — mà `schema.sql` gốc không có bảng nào cho việc đó.
--
-- Nếu refresh token là một JWT tự chứa (stateless), server không cách nào
-- vô hiệu hoá nó trước hạn: kẻ lấy được token vẫn dùng được đủ 7 ngày dù
-- người dùng đã bấm đăng xuất và đổi mật khẩu. Với hệ thống giữ hồ sơ trẻ
-- em thì đó không phải đánh đổi chấp nhận được.
--
-- Nên refresh token ở đây là một chuỗi ngẫu nhiên (opaque), còn bảng này
-- là sổ cái quyết định nó còn sống hay không.
-- =====================================================================

CREATE TABLE refresh_token (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nguoi_dung_id  UUID NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,

    -- LƯU BẢN BĂM, KHÔNG LƯU TOKEN GỐC.
    -- Cùng lý do với mật khẩu: nếu ai đó đọc được bảng này (backup thất lạc,
    -- SQL injection, lộ quyền đọc DB) thì họ vẫn không đăng nhập được thay
    -- người dùng. SHA-256 hex là 64 ký tự.
    --
    -- Không dùng BCrypt như mật khẩu: token đã là 256 bit ngẫu nhiên nên
    -- không có gì để dò từ điển, mà BCrypt thì cố tình chậm — mỗi lần gọi
    -- /auth/refresh phải chờ vài chục mili-giây là vô ích.
    ma_bam         VARCHAR(64)  NOT NULL UNIQUE,

    het_han_luc    TIMESTAMPTZ  NOT NULL,

    -- NULL = còn hiệu lực. Có giá trị = đã bị thu hồi lúc đó.
    -- Giữ lại dòng đã thu hồi thay vì xoá, để còn truy vết được khi cần
    -- điều tra một phiên đăng nhập đáng ngờ.
    thu_hoi_luc    TIMESTAMPTZ,

    dia_chi_ip     VARCHAR(45),          -- 45 ký tự đủ cho IPv6
    ngay_tao       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Truy vấn nóng: "thu hồi mọi token của người này" khi đổi mật khẩu.
CREATE INDEX idx_refresh_token_nguoi_dung ON refresh_token(nguoi_dung_id);

-- Phục vụ việc dọn token hết hạn định kỳ.
CREATE INDEX idx_refresh_token_het_han ON refresh_token(het_han_luc);

COMMENT ON COLUMN refresh_token.ma_bam IS
    'SHA-256 dạng hex của refresh token. Token gốc không bao giờ được lưu.';
