# 03 — Mô hình dữ liệu (ERD)

## Sơ đồ quan hệ (Mermaid)

Dán đoạn dưới vào <https://mermaid.live> hoặc bất kỳ trình đọc Markdown hỗ trợ Mermaid
để xem sơ đồ trực quan.

```mermaid
erDiagram
    NAM_HOC ||--o{ LOP_HOC : "có"
    NGANH   ||--o{ LOP_HOC : "thuộc"
    NAM_HOC ||--o{ PHAN_CONG : ""
    NAM_HOC ||--o{ LICH_TRUC : ""

    NGUOI_DUNG ||--o{ NGUOI_DUNG_VAI_TRO : ""
    VAI_TRO    ||--o{ NGUOI_DUNG_VAI_TRO : ""
    NGUOI_DUNG ||--o{ PHAN_CONG : "được phân công"
    LOP_HOC    ||--o{ PHAN_CONG : ""
    NGANH      ||--o{ PHAN_CONG : ""

    THIEU_NHI ||--o{ BI_TICH : "lãnh nhận"
    THIEU_NHI ||--o{ GHI_DANH : ""
    LOP_HOC   ||--o{ GHI_DANH : ""

    GHI_DANH ||--o{ DIEM_DANH : ""
    GHI_DANH ||--|| DIEM_SO : ""

    TO_TRUC    ||--o{ THANH_VIEN_TO_TRUC : ""
    NGUOI_DUNG ||--o{ THANH_VIEN_TO_TRUC : ""
    TO_TRUC    ||--o{ LICH_TRUC : ""

    THIEU_NHI  ||--o{ PHIEU_RA_CONG : ""
    NGUOI_DUNG ||--o{ PHIEU_RA_CONG : "tạo/xác nhận"

    NGUOI_DUNG ||--o{ NHAT_KY_HE_THONG : ""

    NAM_HOC {
        uuid id PK
        varchar ten_nam_hoc UK
        date ngay_bat_dau
        date ngay_ket_thuc
        varchar trang_thai
    }
    NGANH {
        uuid id PK
        varchar ten_nganh UK
        int tuoi_toi_thieu
        int tuoi_toi_da
        int thu_tu
    }
    LOP_HOC {
        uuid id PK
        varchar ten_lop
        uuid nganh_id FK
        uuid nam_hoc_id FK
    }
    NGUOI_DUNG {
        uuid id PK
        varchar ten_thanh
        varchar ho_ten
        varchar email UK
        varchar so_dien_thoai UK
        varchar mat_khau_hash
        boolean can_doi_mat_khau
        boolean dang_hoat_dong
    }
    VAI_TRO {
        uuid id PK
        varchar ma UK
        varchar ten_hien_thi
    }
    PHAN_CONG {
        uuid id PK
        uuid nguoi_dung_id FK
        uuid lop_id FK
        uuid nganh_id FK
        uuid nam_hoc_id FK
        varchar chuc_vu
    }
    THIEU_NHI {
        uuid id PK
        varchar ma_thieu_nhi UK
        varchar ten_thanh
        varchar ho_ten
        date ngay_sinh
        varchar gioi_tinh
        varchar ten_bo
        varchar ten_me
        varchar sdt_phu_huynh
        varchar dia_chi
        varchar giao_ho
        boolean da_xoa
    }
    BI_TICH {
        uuid id PK
        uuid thieu_nhi_id FK
        varchar loai_bi_tich
        date ngay_cu_hanh
        varchar noi_cu_hanh
        varchar cha_chu_su
        varchar nguoi_do_dau
        varchar so_so
    }
    GHI_DANH {
        uuid id PK
        uuid thieu_nhi_id FK
        uuid lop_id FK
        varchar trang_thai
        date ngay_ghi_danh
    }
    DIEM_DANH {
        uuid id PK
        uuid ghi_danh_id FK
        date ngay_diem_danh
        boolean di_le
        boolean di_hoc
        boolean co_phep
        text ghi_chu
        uuid nguoi_diem_danh_id FK
    }
    DIEM_SO {
        uuid id PK
        uuid ghi_danh_id FK
        numeric diem_hk1
        numeric diem_hk2
        numeric diem_tb
        varchar ket_qua
    }
    TO_TRUC {
        uuid id PK
        varchar ten_to
        text mo_ta
    }
    THANH_VIEN_TO_TRUC {
        uuid id PK
        uuid to_truc_id FK
        uuid nguoi_dung_id FK
    }
    LICH_TRUC {
        uuid id PK
        uuid to_truc_id FK
        uuid nam_hoc_id FK
        date ngay_truc
        varchar ca_truc
    }
    PHIEU_RA_CONG {
        uuid id PK
        uuid thieu_nhi_id FK
        uuid nguoi_tao_id FK
        uuid nguoi_xac_nhan_id FK
        text ly_do
        timestamptz thoi_gian_tao
        timestamptz thoi_gian_ra_cong
        varchar trang_thai
    }
    NHAT_KY_HE_THONG {
        uuid id PK
        uuid nguoi_dung_id FK
        varchar hanh_dong
        varchar doi_tuong
        uuid doi_tuong_id
        jsonb du_lieu_cu
        jsonb du_lieu_moi
        timestamptz thoi_gian
    }
```

---

## Giải thích các quyết định thiết kế

### 1. Dùng UUID làm khoá chính
Không dùng số tự tăng 1, 2, 3. Lý do: người ngoài không đoán được số lượng hồ sơ qua URL;
và nếu sau này gộp dữ liệu của nhiều xứ đoàn thì không bị đụng ID.
Dùng **UUID v7** (`uuid_generate_v7` hoặc sinh ở tầng Java) để giữ tính tuần tự, tránh
phân mảnh index như UUID v4.

### 2. Tách bảng `BI_TICH` ra khỏi `THIEU_NHI`, quan hệ 1-N
Bản thiết kế ban đầu để 1-1 với các cột `ngay_rua_toi`, `ngay_ruoc_le`... Không đủ, vì:
- Mỗi bí tích cử hành ở nơi khác nhau, cha chủ sự khác nhau, người đỡ đầu khác nhau.
- Còn Xưng Tội lần đầu và Bao Đồng.
- Thêm loại bí tích mới sau này chỉ cần thêm dòng, không cần `ALTER TABLE`.

### 3. `NGUOI_DUNG_VAI_TRO` là N-N, không phải cột enum
Một người vừa là Huynh trưởng lớp Ấu 1A vừa thuộc Ban Kỷ luật là chuyện bình thường.
Cột enum đơn sẽ chật ngay ở sprint 7.

### 4. `PHAN_CONG` gắn với năm học
Phân công lớp thay đổi mỗi năm. Nếu để `lop_hoc.nguoi_dung_id` như thiết kế ban đầu thì
mất lịch sử "năm ngoái ai dạy lớp này" và không xử lý được lớp có hai huynh trưởng.
Cột `lop_id` và `nganh_id` loại trừ nhau: phân công cấp lớp thì `nganh_id` null, và ngược lại.

### 5. `DIEM_DANH` và `DIEM_SO` trỏ về `GHI_DANH`, không trỏ thẳng về `THIEU_NHI`
Vì điểm danh luôn thuộc về "em này, ở lớp này, năm học này". Trỏ qua ghi danh thì tự động
có đủ ngữ cảnh, không cần lặp lại `lop_id` và `nam_hoc_id`.

### 6. Ràng buộc chống trùng bắt buộc phải có
```sql
UNIQUE (ghi_danh_id, ngay_diem_danh)   -- chống điểm danh trùng khi 2 người cùng bấm
UNIQUE (thieu_nhi_id, loai_bi_tich)     -- một em chỉ lãnh nhận mỗi bí tích một lần
UNIQUE (ten_lop, nam_hoc_id)            -- tên lớp duy nhất trong một năm học
UNIQUE (ghi_danh_id)  ON diem_so        -- quan hệ 1-1
```
Cộng thêm partial index chống một em có hai ghi danh `DANG_HOC` cùng năm học, và chống
hai phiếu ra cổng `CHO_RA_CONG` cùng lúc cho một em.

### 7. Soft delete cho `THIEU_NHI`
Cột `da_xoa boolean`. Hồ sơ trẻ em không xoá cứng — có thể cần tra cứu bí tích nhiều năm sau.
Mọi truy vấn mặc định thêm `WHERE da_xoa = false`.

### 8. `NHAT_KY_HE_THONG` (audit log)
Bắt buộc vì đây là dữ liệu cá nhân của người dưới 18 tuổi. Ghi lại mọi thao tác thêm/sửa/xoá
hồ sơ thiếu nhi, tạo/xác nhận phiếu ra cổng, thay đổi phân quyền. Lưu `du_lieu_cu` và
`du_lieu_moi` dạng JSONB để truy vết được ai đổi cái gì.

### 9. Cột chuẩn ở mọi bảng
Mọi bảng đều có `ngay_tao timestamptz`, `ngay_cap_nhat timestamptz`, `nguoi_tao_id uuid`,
`nguoi_cap_nhat_id uuid`. Trong Java dùng `@MappedSuperclass BaseEntity` với
`@CreatedDate`, `@LastModifiedBy` (Spring Data JPA Auditing).

---

## Chỉ mục (Index) cần tạo

```sql
CREATE INDEX idx_ghi_danh_lop      ON ghi_danh(lop_id) WHERE trang_thai = 'DANG_HOC';
CREATE INDEX idx_diem_danh_ngay    ON diem_danh(ngay_diem_danh);
CREATE INDEX idx_diem_danh_ghidanh ON diem_danh(ghi_danh_id);
CREATE INDEX idx_thieu_nhi_hoten   ON thieu_nhi USING gin (to_tsvector('simple', ho_ten));
CREATE INDEX idx_phieu_cho_ra_cong ON phieu_ra_cong(trang_thai, thoi_gian_tao)
       WHERE trang_thai = 'CHO_RA_CONG';
CREATE INDEX idx_phan_cong_nguoi   ON phan_cong(nguoi_dung_id, nam_hoc_id);
```

Chỉ mục full-text trên `ho_ten` để tìm kiếm tên tiếng Việt nhanh khi có 3.000 hồ sơ.

---

## Enum sử dụng

| Enum | Giá trị |
|---|---|
| `trang_thai_nam_hoc` | `CHUAN_BI`, `DANG_HOAT_DONG`, `DA_KET_THUC` |
| `loai_bi_tich` | `RUA_TOI`, `XUNG_TOI_LAN_DAU`, `RUOC_LE_LAN_DAU`, `THEM_SUC`, `BAO_DONG` |
| `trang_thai_ghi_danh` | `DANG_HOC`, `CHUYEN_XU`, `NGHI_HOC`, `HOAN_THANH` |
| `ket_qua_hoc_tap` | `DAT`, `KHONG_DAT`, `CHUA_XET` |
| `trang_thai_phieu` | `CHO_RA_CONG`, `DA_RA_CONG`, `HUY` |
| `chuc_vu_phan_cong` | `CHU_NHIEM`, `PHU_TA`, `TRUONG_NGANH` |
| `ma_vai_tro` | `ADMIN`, `KHOI_TRUONG`, `HUYNH_TRUONG`, `KY_LUAT` |

Lưu dạng `VARCHAR` + `CHECK` constraint trong PostgreSQL (dễ thêm giá trị hơn native enum),
ánh xạ sang Java `enum` bằng `@Enumerated(EnumType.STRING)`.
