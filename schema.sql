-- =====================================================================
-- Hệ thống Quản lý Xứ đoàn Thiếu Nhi Thánh Thể
-- PostgreSQL 16 — DDL khởi tạo
-- Dùng làm Flyway migration: V1__init.sql
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "unaccent";   -- tìm kiếm tiếng Việt không dấu

-- ---------------------------------------------------------------------
-- 1. TỔ CHỨC
-- ---------------------------------------------------------------------

CREATE TABLE nam_hoc (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_nam_hoc     VARCHAR(20)  NOT NULL UNIQUE,          -- '2026-2027'
    ngay_bat_dau    DATE         NOT NULL,
    ngay_ket_thuc   DATE         NOT NULL,
    trang_thai      VARCHAR(20)  NOT NULL DEFAULT 'CHUAN_BI'
                    CHECK (trang_thai IN ('CHUAN_BI','DANG_HOAT_DONG','DA_KET_THUC')),
    ngay_tao        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_nam_hoc_ngay CHECK (ngay_ket_thuc > ngay_bat_dau)
);

-- Chỉ cho phép duy nhất một năm học đang hoạt động
CREATE UNIQUE INDEX uq_nam_hoc_dang_hoat_dong
    ON nam_hoc ((trang_thai)) WHERE trang_thai = 'DANG_HOAT_DONG';

CREATE TABLE nganh (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_nganh       VARCHAR(50)  NOT NULL UNIQUE,          -- 'Ấu Nhi'
    ma_nganh        VARCHAR(20)  NOT NULL UNIQUE,          -- 'AU_NHI'
    tuoi_toi_thieu  SMALLINT     NOT NULL,
    tuoi_toi_da     SMALLINT     NOT NULL,
    thu_tu          SMALLINT     NOT NULL UNIQUE,          -- dùng cho chuyển cấp
    ngay_tao        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE lop_hoc (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_lop         VARCHAR(50)  NOT NULL,                 -- 'Ấu 1A'
    nganh_id        UUID         NOT NULL REFERENCES nganh(id),
    nam_hoc_id      UUID         NOT NULL REFERENCES nam_hoc(id) ON DELETE CASCADE,
    cap_do          SMALLINT     NOT NULL DEFAULT 1,       -- Ấu 1 / Ấu 2 / Ấu 3
    ghi_chu         TEXT,
    ngay_tao        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_lop_ten_nam UNIQUE (ten_lop, nam_hoc_id)
);

-- ---------------------------------------------------------------------
-- 2. NHÂN SỰ & PHÂN QUYỀN
-- ---------------------------------------------------------------------

CREATE TABLE nguoi_dung (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_thanh         VARCHAR(50),
    ho_ten            VARCHAR(120) NOT NULL,
    ngay_sinh         DATE,
    email             VARCHAR(120) UNIQUE,
    so_dien_thoai     VARCHAR(20)  UNIQUE,
    mat_khau_hash     VARCHAR(100) NOT NULL,
    can_doi_mat_khau  BOOLEAN      NOT NULL DEFAULT true,
    dang_hoat_dong    BOOLEAN      NOT NULL DEFAULT true,
    lan_dang_nhap_cuoi TIMESTAMPTZ,
    ngay_tao          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_nguoi_dung_dinh_danh CHECK (email IS NOT NULL OR so_dien_thoai IS NOT NULL)
);

CREATE TABLE vai_tro (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ma            VARCHAR(30) NOT NULL UNIQUE,
    ten_hien_thi  VARCHAR(80) NOT NULL,
    mo_ta         TEXT
);

CREATE TABLE nguoi_dung_vai_tro (
    nguoi_dung_id UUID NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    vai_tro_id    UUID NOT NULL REFERENCES vai_tro(id)    ON DELETE CASCADE,
    PRIMARY KEY (nguoi_dung_id, vai_tro_id)
);

-- Phân công gắn với năm học: lop_id và nganh_id loại trừ nhau
CREATE TABLE phan_cong (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nguoi_dung_id  UUID NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    nam_hoc_id     UUID NOT NULL REFERENCES nam_hoc(id)    ON DELETE CASCADE,
    lop_id         UUID REFERENCES lop_hoc(id) ON DELETE CASCADE,
    nganh_id       UUID REFERENCES nganh(id),
    chuc_vu        VARCHAR(20) NOT NULL
                   CHECK (chuc_vu IN ('CHU_NHIEM','PHU_TA','TRUONG_NGANH')),
    ngay_tao       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_phan_cong_pham_vi CHECK (
        (lop_id IS NOT NULL AND nganh_id IS NULL) OR
        (lop_id IS NULL AND nganh_id IS NOT NULL)
    )
);

CREATE INDEX idx_phan_cong_nguoi ON phan_cong(nguoi_dung_id, nam_hoc_id);
CREATE INDEX idx_phan_cong_lop   ON phan_cong(lop_id);

-- ---------------------------------------------------------------------
-- 3. THIẾU NHI & BÍ TÍCH
-- ---------------------------------------------------------------------

CREATE TABLE thieu_nhi (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ma_thieu_nhi    VARCHAR(20)  NOT NULL UNIQUE,          -- 'TN2026001'
    ten_thanh       VARCHAR(50),
    ho_ten          VARCHAR(120) NOT NULL,
    ngay_sinh       DATE         NOT NULL,
    gioi_tinh       VARCHAR(10)  CHECK (gioi_tinh IN ('NAM','NU')),
    ten_bo          VARCHAR(120),
    ten_me          VARCHAR(120),
    sdt_phu_huynh   VARCHAR(20),
    dia_chi         TEXT,
    giao_ho         VARCHAR(80),
    ghi_chu         TEXT,
    da_xoa          BOOLEAN      NOT NULL DEFAULT false,
    ngay_tao        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ngay_cap_nhat   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_thieu_nhi_ngay_sinh CHECK (ngay_sinh <= CURRENT_DATE)
);

CREATE INDEX idx_thieu_nhi_ho_ten
    ON thieu_nhi USING gin (to_tsvector('simple', unaccent(ho_ten)));
CREATE INDEX idx_thieu_nhi_chua_xoa ON thieu_nhi(id) WHERE da_xoa = false;

CREATE TABLE bi_tich (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thieu_nhi_id   UUID NOT NULL REFERENCES thieu_nhi(id) ON DELETE CASCADE,
    loai_bi_tich   VARCHAR(30) NOT NULL
                   CHECK (loai_bi_tich IN ('RUA_TOI','XUNG_TOI_LAN_DAU',
                                           'RUOC_LE_LAN_DAU','THEM_SUC','BAO_DONG')),
    ngay_cu_hanh   DATE,
    noi_cu_hanh    VARCHAR(150),
    cha_chu_su     VARCHAR(120),
    nguoi_do_dau   VARCHAR(120),
    so_so          VARCHAR(50),
    ngay_tao       TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_bi_tich_moi_loai UNIQUE (thieu_nhi_id, loai_bi_tich)
);

-- ---------------------------------------------------------------------
-- 4. GHI DANH, ĐIỂM DANH, ĐIỂM SỐ
-- ---------------------------------------------------------------------

CREATE TABLE ghi_danh (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thieu_nhi_id   UUID NOT NULL REFERENCES thieu_nhi(id) ON DELETE CASCADE,
    lop_id         UUID NOT NULL REFERENCES lop_hoc(id)   ON DELETE CASCADE,
    trang_thai     VARCHAR(20) NOT NULL DEFAULT 'DANG_HOC'
                   CHECK (trang_thai IN ('DANG_HOC','CHUYEN_XU','NGHI_HOC','HOAN_THANH')),
    ngay_ghi_danh  DATE NOT NULL DEFAULT CURRENT_DATE,
    ngay_tao       TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ghi_danh UNIQUE (thieu_nhi_id, lop_id)
);

CREATE INDEX idx_ghi_danh_lop ON ghi_danh(lop_id) WHERE trang_thai = 'DANG_HOC';

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
    CONSTRAINT uq_diem_danh_ngay UNIQUE (ghi_danh_id, ngay_diem_danh)
);

CREATE INDEX idx_diem_danh_ngay ON diem_danh(ngay_diem_danh);

CREATE TABLE diem_so (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ghi_danh_id    UUID NOT NULL UNIQUE REFERENCES ghi_danh(id) ON DELETE CASCADE,
    diem_hk1       NUMERIC(4,2) CHECK (diem_hk1 BETWEEN 0 AND 10),
    diem_hk2       NUMERIC(4,2) CHECK (diem_hk2 BETWEEN 0 AND 10),
    diem_tb        NUMERIC(4,2) CHECK (diem_tb  BETWEEN 0 AND 10),
    ti_le_chuyen_can NUMERIC(5,2),
    ket_qua        VARCHAR(20) NOT NULL DEFAULT 'CHUA_XET'
                   CHECK (ket_qua IN ('DAT','KHONG_DAT','CHUA_XET')),
    ngay_tao       TIMESTAMPTZ NOT NULL DEFAULT now(),
    ngay_cap_nhat  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- 5. BAN KỶ LUẬT & TRỰC CỔNG
-- ---------------------------------------------------------------------

CREATE TABLE to_truc (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ten_to    VARCHAR(80) NOT NULL UNIQUE,
    mo_ta     TEXT,
    ngay_tao  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE thanh_vien_to_truc (
    to_truc_id     UUID NOT NULL REFERENCES to_truc(id)    ON DELETE CASCADE,
    nguoi_dung_id  UUID NOT NULL REFERENCES nguoi_dung(id) ON DELETE CASCADE,
    la_to_truong   BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (to_truc_id, nguoi_dung_id)
);

CREATE TABLE lich_truc (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    to_truc_id  UUID NOT NULL REFERENCES to_truc(id) ON DELETE CASCADE,
    nam_hoc_id  UUID NOT NULL REFERENCES nam_hoc(id) ON DELETE CASCADE,
    ngay_truc   DATE        NOT NULL,
    ca_truc     VARCHAR(80) NOT NULL,   -- 'Thánh lễ thiếu nhi 7h30'
    ghi_chu     TEXT,
    ngay_tao    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lich_truc UNIQUE (ngay_truc, ca_truc, to_truc_id)
);

CREATE INDEX idx_lich_truc_ngay ON lich_truc(ngay_truc);

CREATE TABLE phieu_ra_cong (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    thieu_nhi_id       UUID NOT NULL REFERENCES thieu_nhi(id),
    ghi_danh_id        UUID REFERENCES ghi_danh(id),   -- để biết lớp nào
    nguoi_tao_id       UUID NOT NULL REFERENCES nguoi_dung(id),
    nguoi_xac_nhan_id  UUID REFERENCES nguoi_dung(id),
    ly_do              TEXT NOT NULL,
    thoi_gian_tao      TIMESTAMPTZ NOT NULL DEFAULT now(),
    thoi_gian_ra_cong  TIMESTAMPTZ,
    trang_thai         VARCHAR(20) NOT NULL DEFAULT 'CHO_RA_CONG'
                       CHECK (trang_thai IN ('CHO_RA_CONG','DA_RA_CONG','HUY')),
    CONSTRAINT ck_phieu_xac_nhan CHECK (
        (trang_thai = 'DA_RA_CONG' AND thoi_gian_ra_cong IS NOT NULL
                                    AND nguoi_xac_nhan_id IS NOT NULL)
        OR trang_thai <> 'DA_RA_CONG'
    )
);

-- Một em không thể có 2 phiếu đang chờ cùng lúc
CREATE UNIQUE INDEX uq_phieu_dang_cho
    ON phieu_ra_cong(thieu_nhi_id) WHERE trang_thai = 'CHO_RA_CONG';

CREATE INDEX idx_phieu_cho_ra_cong
    ON phieu_ra_cong(trang_thai, thoi_gian_tao) WHERE trang_thai = 'CHO_RA_CONG';

-- ---------------------------------------------------------------------
-- 6. HỆ THỐNG
-- ---------------------------------------------------------------------

CREATE TABLE cau_hinh (
    khoa       VARCHAR(80) PRIMARY KEY,
    gia_tri    TEXT NOT NULL,
    mo_ta      TEXT,
    ngay_cap_nhat TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE nhat_ky_he_thong (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nguoi_dung_id  UUID REFERENCES nguoi_dung(id),
    hanh_dong      VARCHAR(30) NOT NULL,   -- TAO, SUA, XOA, DANG_NHAP, XAC_NHAN...
    doi_tuong      VARCHAR(50) NOT NULL,   -- 'THIEU_NHI', 'PHIEU_RA_CONG'...
    doi_tuong_id   UUID,
    du_lieu_cu     JSONB,
    du_lieu_moi    JSONB,
    dia_chi_ip     VARCHAR(45),
    thoi_gian      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_nhat_ky_thoi_gian ON nhat_ky_he_thong(thoi_gian DESC);
CREATE INDEX idx_nhat_ky_doi_tuong ON nhat_ky_he_thong(doi_tuong, doi_tuong_id);

-- =====================================================================
-- DỮ LIỆU KHỞI TẠO
-- =====================================================================

INSERT INTO vai_tro (ma, ten_hien_thi, mo_ta) VALUES
 ('ADMIN',        'Quản trị viên / Ban điều hành', 'Toàn quyền trên toàn xứ đoàn'),
 ('KHOI_TRUONG',  'Trưởng ngành / Khối trưởng',    'Quản lý các lớp thuộc ngành phụ trách'),
 ('HUYNH_TRUONG', 'Huynh trưởng / Giáo lý viên',   'Quản lý lớp được phân công'),
 ('KY_LUAT',      'Thành viên Ban Kỷ luật',        'Trực cổng, xác nhận phiếu ra về');

INSERT INTO nganh (ten_nganh, ma_nganh, tuoi_toi_thieu, tuoi_toi_da, thu_tu) VALUES
 ('Chiên Con',  'CHIEN_CON',  4,  6, 1),
 ('Ấu Nhi',     'AU_NHI',     7,  9, 2),
 ('Thiếu Nhi',  'THIEU_NHI', 10, 12, 3),
 ('Nghĩa Sĩ',   'NGHIA_SI',  13, 15, 4),
 ('Hiệp Sĩ',    'HIEP_SI',   16, 18, 5);

INSERT INTO cau_hinh (khoa, gia_tri, mo_ta) VALUES
 ('diem_dat_toi_thieu',      '5.0',  'Điểm trung bình tối thiểu để được lên lớp'),
 ('chuyen_can_toi_thieu',    '70',   'Tỉ lệ chuyên cần tối thiểu (%)'),
 ('he_so_hk2',               '2',    'Hệ số nhân của điểm học kỳ 2'),
 ('so_ngay_sua_diem_danh',   '7',    'Số ngày lùi tối đa được phép sửa điểm danh'),
 ('tien_to_ma_thieu_nhi',    'TN',   'Tiền tố mã định danh thiếu nhi');

-- Tài khoản admin đầu tiên — mật khẩu: Admin@123 (BẮT BUỘC đổi sau lần đăng nhập đầu)
-- Hash BCrypt cost 10. Sinh lại hash của bạn trước khi chạy ở production.
INSERT INTO nguoi_dung (ten_thanh, ho_ten, email, mat_khau_hash, can_doi_mat_khau)
VALUES ('Giuse', 'Quản trị viên', 'admin@xudoan.local',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true);

INSERT INTO nguoi_dung_vai_tro (nguoi_dung_id, vai_tro_id)
SELECT u.id, r.id FROM nguoi_dung u, vai_tro r
WHERE u.email = 'admin@xudoan.local' AND r.ma = 'ADMIN';
