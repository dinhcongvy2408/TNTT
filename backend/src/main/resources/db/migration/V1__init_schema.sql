-- =====================================================================
-- V1 — Khởi tạo schema
-- Nguồn: /schema.sql (docs/03-du-lieu.md), có SỬA 4 chỗ, đánh dấu [SỬA].
--
-- NGUYÊN TẮC FLYWAY: file này đã chạy là BẤT BIẾN. Muốn đổi schema thì
-- viết V2, V3... Sửa lại file này sau khi đã chạy sẽ làm sai checksum và
-- app không khởi động được nữa.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA public;   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "unaccent" WITH SCHEMA public;   -- bỏ dấu tiếng Việt

-- ---------------------------------------------------------------------
-- [SỬA 1] Hàm bỏ dấu IMMUTABLE
--
-- schema.sql gốc viết:
--     CREATE INDEX ... USING gin (to_tsvector('simple', unaccent(ho_ten)));
-- Câu đó KHÔNG CHẠY ĐƯỢC. PostgreSQL chỉ cho phép hàm IMMUTABLE trong biểu
-- thức index, mà unaccent(text) 1 tham số chỉ là STABLE — nó phải tra
-- từ điển theo search_path hiện tại, nên kết quả có thể đổi giữa hai session.
--
-- Cách chuẩn: bọc lại bằng dạng 2 tham số có chỉ đích danh từ điển, khi đó
-- kết quả thật sự cố định và ta khai báo IMMUTABLE là trung thực.
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION f_unaccent(txt text)
    RETURNS text
    LANGUAGE sql
    IMMUTABLE PARALLEL SAFE STRICT
RETURN public.unaccent('public.unaccent'::regdictionary, txt);

COMMENT ON FUNCTION f_unaccent(text) IS
    'Bỏ dấu tiếng Việt, IMMUTABLE nên dùng được trong biểu thức index';

-- ---------------------------------------------------------------------
-- 1. TỔ CHỨC
-- ---------------------------------------------------------------------

CREATE TABLE nam_hoc (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_nam_hoc        VARCHAR(20)  NOT NULL UNIQUE,          -- '2026-2027'
    ngay_bat_dau       DATE         NOT NULL,
    ngay_ket_thuc      DATE         NOT NULL,
    trang_thai         VARCHAR(20)  NOT NULL DEFAULT 'CHUAN_BI'
                       CHECK (trang_thai IN ('CHUAN_BI','DANG_HOAT_DONG','DA_KET_THUC')),
    ngay_tao           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID,
    CONSTRAINT ck_nam_hoc_ngay CHECK (ngay_ket_thuc > ngay_bat_dau)
);

-- Chỉ duy nhất MỘT năm học được ở trạng thái DANG_HOAT_DONG.
-- Đây là "partial unique index": chỉ số chỉ tồn tại cho các dòng thoả WHERE.
-- Vì mọi dòng DANG_HOAT_DONG đều có cùng giá trị cột, dòng thứ hai sẽ đụng
-- ràng buộc và bị từ chối ngay ở tầng DB — không phụ thuộc code Java.
CREATE UNIQUE INDEX uq_nam_hoc_dang_hoat_dong
    ON nam_hoc (trang_thai) WHERE trang_thai = 'DANG_HOAT_DONG';

CREATE TABLE nganh (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_nganh          VARCHAR(50)  NOT NULL UNIQUE,          -- 'Ấu Nhi'
    ma_nganh           VARCHAR(20)  NOT NULL UNIQUE,          -- 'AU_NHI'
    tuoi_toi_thieu     SMALLINT     NOT NULL,
    tuoi_toi_da        SMALLINT     NOT NULL,
    thu_tu             SMALLINT     NOT NULL UNIQUE,          -- dùng khi chuyển cấp
    ngay_tao           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID,
    CONSTRAINT ck_nganh_tuoi CHECK (tuoi_toi_da >= tuoi_toi_thieu)
);

CREATE TABLE lop_hoc (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_lop            VARCHAR(50)  NOT NULL,                 -- 'Ấu 1A'
    nganh_id           UUID         NOT NULL REFERENCES nganh(id),
    nam_hoc_id         UUID         NOT NULL REFERENCES nam_hoc(id) ON DELETE CASCADE,
    cap_do             SMALLINT     NOT NULL DEFAULT 1,       -- Ấu 1 / Ấu 2 / Ấu 3
    ghi_chu            TEXT,
    ngay_tao           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID,
    CONSTRAINT uq_lop_ten_nam UNIQUE (ten_lop, nam_hoc_id),
    -- Không thừa: khoá ngoại ghép của ghi_danh sẽ trỏ vào cặp cột này.
    -- Xem [SỬA 3] ở phần ghi_danh.
    CONSTRAINT uq_lop_id_nam  UNIQUE (id, nam_hoc_id)
);

CREATE INDEX idx_lop_hoc_nam_hoc ON lop_hoc(nam_hoc_id);

-- ---------------------------------------------------------------------
-- 2. NHÂN SỰ & PHÂN QUYỀN
-- ---------------------------------------------------------------------

CREATE TABLE nguoi_dung (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_thanh           VARCHAR(50),
    ho_ten              VARCHAR(120) NOT NULL,
    ngay_sinh           DATE,
    email               VARCHAR(120) UNIQUE,
    so_dien_thoai       VARCHAR(20)  UNIQUE,
    mat_khau_hash       VARCHAR(100) NOT NULL,
    can_doi_mat_khau    BOOLEAN      NOT NULL DEFAULT true,
    dang_hoat_dong      BOOLEAN      NOT NULL DEFAULT true,
    lan_dang_nhap_cuoi  TIMESTAMPTZ,
    ngay_tao            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    nguoi_tao_id        UUID,
    nguoi_cap_nhat_id   UUID,
    -- Đăng nhập bằng email HOẶC số điện thoại, phải có ít nhất một
    CONSTRAINT ck_nguoi_dung_dinh_danh CHECK (email IS NOT NULL OR so_dien_thoai IS NOT NULL)
);

CREATE TABLE vai_tro (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ma                 VARCHAR(30) NOT NULL UNIQUE,   -- ADMIN, KHOI_TRUONG...
    ten_hien_thi       VARCHAR(80) NOT NULL,
    mo_ta              TEXT,
    ngay_tao           TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID
);

-- Bảng nối N-N. Khoá chính ghép, không cần cột id riêng.
CREATE TABLE nguoi_dung_vai_tro (
    nguoi_dung_id UUID NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    vai_tro_id    UUID NOT NULL REFERENCES vai_tro(id)    ON DELETE CASCADE,
    ngay_tao      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (nguoi_dung_id, vai_tro_id)
);

-- Phân công gắn với năm học: lop_id và nganh_id LOẠI TRỪ nhau
CREATE TABLE phan_cong (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nguoi_dung_id      UUID NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    nam_hoc_id         UUID NOT NULL REFERENCES nam_hoc(id)    ON DELETE CASCADE,
    lop_id             UUID REFERENCES lop_hoc(id) ON DELETE CASCADE,
    nganh_id           UUID REFERENCES nganh(id),
    chuc_vu            VARCHAR(20) NOT NULL
                       CHECK (chuc_vu IN ('CHU_NHIEM','PHU_TA','TRUONG_NGANH')),
    ngay_tao           TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID,
    CONSTRAINT ck_phan_cong_pham_vi CHECK (
        (lop_id IS NOT NULL AND nganh_id IS NULL) OR
        (lop_id IS NULL AND nganh_id IS NOT NULL)
    )
);

-- Chỉ mục này là XƯƠNG SỐNG của phân quyền: mỗi request của huynh trưởng
-- đều hỏi "người này được phân công lớp nào trong năm học đang hoạt động".
CREATE INDEX idx_phan_cong_nguoi ON phan_cong(nguoi_dung_id, nam_hoc_id);
CREATE INDEX idx_phan_cong_lop   ON phan_cong(lop_id);

-- ---------------------------------------------------------------------
-- 3. THIẾU NHI & BÍ TÍCH
-- ---------------------------------------------------------------------

CREATE TABLE thieu_nhi (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ma_thieu_nhi       VARCHAR(20)  NOT NULL UNIQUE,          -- 'TN2026001'
    ten_thanh          VARCHAR(50),
    ho_ten             VARCHAR(120) NOT NULL,
    ngay_sinh          DATE         NOT NULL,
    gioi_tinh          VARCHAR(10)  CHECK (gioi_tinh IN ('NAM','NU')),
    ten_bo             VARCHAR(120),
    ten_me             VARCHAR(120),
    sdt_phu_huynh      VARCHAR(20),
    dia_chi            TEXT,
    giao_ho            VARCHAR(80),
    ghi_chu            TEXT,
    da_xoa             BOOLEAN      NOT NULL DEFAULT false,   -- soft delete
    ngay_tao           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID
    -- [SỬA 2] Đã BỎ: CONSTRAINT ck_thieu_nhi_ngay_sinh CHECK (ngay_sinh <= CURRENT_DATE)
    --
    -- PostgreSQL TỪ CHỐI câu đó: "functions in check constraint must be
    -- marked IMMUTABLE". CURRENT_DATE là STABLE — hôm nay nó trả 23/08/2026,
    -- ngày mai trả giá trị khác. Nếu cho phép, một dòng đang hợp lệ hôm nay
    -- có thể thành không hợp lệ ngày mai, và DB không còn tự tin về dữ liệu
    -- của chính nó nữa (VACUUM FULL / pg_restore sẽ vỡ).
    --
    -- Thay bằng @Past trên DTO (Bean Validation) ở Sprint 4.
);

-- Tìm kiếm tên tiếng Việt KHÔNG DẤU: gõ "nguyen van a" vẫn ra "Nguyễn Văn A".
-- GIN + to_tsvector là full-text search, nhanh hơn nhiều so với LIKE '%...%'
-- (LIKE có ký tự % ở đầu thì không dùng được index nào cả).
CREATE INDEX idx_thieu_nhi_ho_ten
    ON thieu_nhi USING gin (to_tsvector('simple', f_unaccent(ho_ten)));

-- Phần lớn truy vấn đều kèm 'AND da_xoa = false' → index bỏ hẳn dòng đã xoá
CREATE INDEX idx_thieu_nhi_chua_xoa ON thieu_nhi(ma_thieu_nhi) WHERE da_xoa = false;

CREATE TABLE bi_tich (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thieu_nhi_id       UUID NOT NULL REFERENCES thieu_nhi(id) ON DELETE CASCADE,
    loai_bi_tich       VARCHAR(30) NOT NULL
                       CHECK (loai_bi_tich IN ('RUA_TOI','XUNG_TOI_LAN_DAU',
                                               'RUOC_LE_LAN_DAU','THEM_SUC','BAO_DONG')),
    ngay_cu_hanh       DATE,
    noi_cu_hanh        VARCHAR(150),
    cha_chu_su         VARCHAR(120),
    nguoi_do_dau       VARCHAR(120),
    so_so              VARCHAR(50),
    ngay_tao           TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID,
    CONSTRAINT uq_bi_tich_moi_loai UNIQUE (thieu_nhi_id, loai_bi_tich)
);

CREATE INDEX idx_bi_tich_loai ON bi_tich(loai_bi_tich);

-- ---------------------------------------------------------------------
-- 4. GHI DANH, ĐIỂM DANH, ĐIỂM SỐ
-- ---------------------------------------------------------------------

CREATE TABLE ghi_danh (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thieu_nhi_id       UUID NOT NULL REFERENCES thieu_nhi(id) ON DELETE CASCADE,
    lop_id             UUID NOT NULL REFERENCES lop_hoc(id)   ON DELETE CASCADE,
    -- [SỬA 3] Cột nam_hoc_id là bổ sung so với schema.sql gốc.
    --
    -- Vì sao cần? docs/02 mục 3.4 quy định: "một thiếu nhi chỉ có tối đa một
    -- ghi danh DANG_HOC trong cùng một năm học". Muốn ép bằng UNIQUE INDEX
    -- thì cả hai cột phải nằm TRÊN CÙNG MỘT BẢNG — mà năm học lại nằm ở
    -- lop_hoc. Không có cột này thì quy tắc chỉ tồn tại trong code Java, và
    -- hai request đồng thời sẽ lách qua được.
    --
    -- Cái giá: dữ liệu lặp, có nguy cơ ghi_danh.nam_hoc_id lệch với
    -- lop_hoc.nam_hoc_id. Khoá ngoại GHÉP bên dưới khoá chặt nguy cơ đó:
    -- cặp (lop_id, nam_hoc_id) phải khớp đúng một dòng trong lop_hoc.
    nam_hoc_id         UUID NOT NULL,
    trang_thai         VARCHAR(20) NOT NULL DEFAULT 'DANG_HOC'
                       CHECK (trang_thai IN ('DANG_HOC','CHUYEN_XU','NGHI_HOC','HOAN_THANH')),
    ngay_ghi_danh      DATE NOT NULL DEFAULT CURRENT_DATE,
    ngay_tao           TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID,
    CONSTRAINT uq_ghi_danh UNIQUE (thieu_nhi_id, lop_id),
    CONSTRAINT fk_ghi_danh_lop_nam FOREIGN KEY (lop_id, nam_hoc_id)
        REFERENCES lop_hoc (id, nam_hoc_id) ON DELETE CASCADE
);

-- Quy tắc "một em một lớp đang học mỗi năm", ép ở tầng DB
CREATE UNIQUE INDEX uq_ghi_danh_dang_hoc
    ON ghi_danh (thieu_nhi_id, nam_hoc_id) WHERE trang_thai = 'DANG_HOC';

-- Truy vấn nóng nhất hệ thống: "lấy sĩ số lớp này để điểm danh"
CREATE INDEX idx_ghi_danh_lop ON ghi_danh(lop_id) WHERE trang_thai = 'DANG_HOC';
CREATE INDEX idx_ghi_danh_thieu_nhi ON ghi_danh(thieu_nhi_id);

CREATE TABLE diem_danh (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ghi_danh_id         UUID NOT NULL REFERENCES ghi_danh(id) ON DELETE CASCADE,
    ngay_diem_danh      DATE NOT NULL,
    di_le               BOOLEAN NOT NULL DEFAULT false,
    di_hoc              BOOLEAN NOT NULL DEFAULT false,
    co_phep             BOOLEAN NOT NULL DEFAULT false,
    ghi_chu             TEXT,
    nguoi_diem_danh_id  UUID REFERENCES nguoi_dung(id),
    ngay_tao            TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat       TIMESTAMPTZ NOT NULL DEFAULT now(),
    nguoi_tao_id        UUID,
    nguoi_cap_nhat_id   UUID,
    -- Chốt chặn chống trùng khi hai huynh trưởng cùng bấm Lưu một lớp.
    -- Cũng chính là chìa khoá cho API batch upsert ở Sprint 5:
    --   INSERT ... ON CONFLICT (ghi_danh_id, ngay_diem_danh) DO UPDATE
    CONSTRAINT uq_diem_danh_ngay UNIQUE (ghi_danh_id, ngay_diem_danh)
);

CREATE INDEX idx_diem_danh_ngay     ON diem_danh(ngay_diem_danh);
CREATE INDEX idx_diem_danh_ghi_danh ON diem_danh(ghi_danh_id);

CREATE TABLE diem_so (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- UNIQUE trên khoá ngoại = quan hệ 1-1 với ghi_danh
    ghi_danh_id        UUID NOT NULL UNIQUE REFERENCES ghi_danh(id) ON DELETE CASCADE,
    diem_hk1           NUMERIC(4,2) CHECK (diem_hk1 BETWEEN 0 AND 10),
    diem_hk2           NUMERIC(4,2) CHECK (diem_hk2 BETWEEN 0 AND 10),
    diem_tb            NUMERIC(4,2) CHECK (diem_tb  BETWEEN 0 AND 10),
    ti_le_chuyen_can   NUMERIC(5,2) CHECK (ti_le_chuyen_can BETWEEN 0 AND 100),
    ket_qua            VARCHAR(20) NOT NULL DEFAULT 'CHUA_XET'
                       CHECK (ket_qua IN ('DAT','KHONG_DAT','CHUA_XET')),
    ngay_tao           TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID
);

-- ---------------------------------------------------------------------
-- 5. BAN KỶ LUẬT & TRỰC CỔNG
-- ---------------------------------------------------------------------

CREATE TABLE to_truc (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_to             VARCHAR(80) NOT NULL UNIQUE,
    mo_ta              TEXT,
    ngay_tao           TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID
);

CREATE TABLE thanh_vien_to_truc (
    to_truc_id     UUID NOT NULL REFERENCES to_truc(id)    ON DELETE CASCADE,
    nguoi_dung_id  UUID NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    la_to_truong   BOOLEAN NOT NULL DEFAULT false,
    ngay_tao       TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (to_truc_id, nguoi_dung_id)
);

CREATE TABLE lich_truc (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    to_truc_id         UUID NOT NULL REFERENCES to_truc(id) ON DELETE CASCADE,
    nam_hoc_id         UUID NOT NULL REFERENCES nam_hoc(id) ON DELETE CASCADE,
    ngay_truc          DATE        NOT NULL,
    ca_truc            VARCHAR(80) NOT NULL,   -- 'Thánh lễ thiếu nhi 7h30'
    ghi_chu            TEXT,
    ngay_tao           TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat      TIMESTAMPTZ NOT NULL DEFAULT now(),
    nguoi_tao_id       UUID,
    nguoi_cap_nhat_id  UUID,
    CONSTRAINT uq_lich_truc UNIQUE (ngay_truc, ca_truc, to_truc_id)
);

CREATE INDEX idx_lich_truc_ngay ON lich_truc(ngay_truc);

-- Bảng này KHÔNG dùng BaseEntity: thoi_gian_tao / nguoi_tao_id ở đây là
-- DỮ LIỆU NGHIỆP VỤ (ai xin cho em về, lúc mấy giờ) chứ không phải cột
-- kỹ thuật — chúng được hiển thị cho người trực cổng đọc.
CREATE TABLE phieu_ra_cong (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thieu_nhi_id       UUID NOT NULL REFERENCES thieu_nhi(id),
    ghi_danh_id        UUID REFERENCES ghi_danh(id),   -- để biết em thuộc lớp nào
    nam_hoc_id         UUID NOT NULL REFERENCES nam_hoc(id),
    -- [SỬA 4] nam_hoc_id là bổ sung. docs/04-api.md quy định topic WebSocket
    -- là /topic/phieu-ra-cong/{namHocId}, nhưng bảng gốc không có đường nào
    -- tới nam_hoc (ghi_danh_id lại cho phép NULL). Không có cột này thì
    -- server không biết đẩy bản tin vào topic nào.
    nguoi_tao_id       UUID NOT NULL REFERENCES nguoi_dung(id),
    nguoi_xac_nhan_id  UUID REFERENCES nguoi_dung(id),
    ly_do              TEXT NOT NULL,
    thoi_gian_tao      TIMESTAMPTZ NOT NULL DEFAULT now(),
    thoi_gian_ra_cong  TIMESTAMPTZ,
    trang_thai         VARCHAR(20) NOT NULL DEFAULT 'CHO_RA_CONG'
                       CHECK (trang_thai IN ('CHO_RA_CONG','DA_RA_CONG','HUY')),
    -- Máy trạng thái ép ngay ở DB: đã ra cổng thì BẮT BUỘC có người xác nhận
    -- và mốc thời gian. Không thể để code quên set một trong hai.
    CONSTRAINT ck_phieu_xac_nhan CHECK (
        (trang_thai = 'DA_RA_CONG' AND thoi_gian_ra_cong IS NOT NULL
                                    AND nguoi_xac_nhan_id IS NOT NULL)
        OR trang_thai <> 'DA_RA_CONG'
    )
);

-- Một em không thể có 2 phiếu đang chờ cùng lúc
CREATE UNIQUE INDEX uq_phieu_dang_cho
    ON phieu_ra_cong(thieu_nhi_id) WHERE trang_thai = 'CHO_RA_CONG';

-- Màn hình trực cổng poll bảng này liên tục → phải có index riêng
CREATE INDEX idx_phieu_cho_ra_cong
    ON phieu_ra_cong(trang_thai, thoi_gian_tao) WHERE trang_thai = 'CHO_RA_CONG';

CREATE INDEX idx_phieu_ngay ON phieu_ra_cong(thoi_gian_tao DESC);

-- ---------------------------------------------------------------------
-- 6. HỆ THỐNG
-- ---------------------------------------------------------------------

-- Bảng key-value cho các ngưỡng nghiệp vụ. Lý do tồn tại: docs/02 mục 5
-- yêu cầu "ngưỡng phải cấu hình được, không hardcode" — ban điều hành có
-- thể đổi điểm sàn mà không cần ta build lại và deploy.
CREATE TABLE cau_hinh (
    khoa           VARCHAR(80) PRIMARY KEY,
    gia_tri        TEXT NOT NULL,
    mo_ta          TEXT,
    ngay_cap_nhat  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Audit log — BẮT BUỘC vì đây là dữ liệu cá nhân của người dưới 18 tuổi
-- (CLAUDE.md mục 6). JSONB để truy vết được "ai đổi field nào, từ gì sang gì".
CREATE TABLE nhat_ky_he_thong (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nguoi_dung_id  UUID REFERENCES nguoi_dung(id),
    hanh_dong      VARCHAR(30) NOT NULL,   -- TAO, SUA, XOA, DANG_NHAP, XAC_NHAN...
    doi_tuong      VARCHAR(50) NOT NULL,   -- 'THIEU_NHI', 'PHIEU_RA_CONG'...
    doi_tuong_id   UUID,
    du_lieu_cu     JSONB,
    du_lieu_moi    JSONB,
    dia_chi_ip     VARCHAR(45),            -- 45 ký tự đủ cho IPv6
    thoi_gian      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_nhat_ky_thoi_gian ON nhat_ky_he_thong(thoi_gian DESC);
CREATE INDEX idx_nhat_ky_doi_tuong ON nhat_ky_he_thong(doi_tuong, doi_tuong_id);
CREATE INDEX idx_nhat_ky_nguoi     ON nhat_ky_he_thong(nguoi_dung_id, thoi_gian DESC);
