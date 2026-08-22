# 99 — Chỗ code khác tài liệu, và vì sao

Ghi lại mọi điểm code chạy thật khác với `schema.sql` / `docs/03-du-lieu.md`.
Đọc file này khi thấy code "sai so với tài liệu" — nhiều khả năng là cố ý.

Cập nhật lần cuối: Sprint 0.

---

## A. Lỗi khiến `schema.sql` gốc không chạy được

### A1. `CHECK (ngay_sinh <= CURRENT_DATE)` — đã bỏ

```sql
-- schema.sql gốc, bảng thieu_nhi
CONSTRAINT ck_thieu_nhi_ngay_sinh CHECK (ngay_sinh <= CURRENT_DATE)
```

PostgreSQL từ chối câu này:

```
ERROR: functions in check constraint must be marked IMMUTABLE
```

**Vì sao PostgreSQL cấm.** `CURRENT_DATE` là hàm `STABLE`, không phải
`IMMUTABLE` — hôm nay nó trả một giá trị, ngày mai trả giá trị khác. Nếu cho
phép, một dòng hợp lệ hôm nay có thể thành không hợp lệ vào ngày mai, và DB
mất khả năng tin vào dữ liệu của chính nó: `pg_restore` hay `VACUUM FULL` sẽ
gãy vì dữ liệu cũ không còn thoả ràng buộc.

**Thay bằng.** Bean Validation ở tầng DTO (Sprint 4):

```java
@Past(message = "Ngày sinh phải ở quá khứ")
private LocalDate ngaySinh;
```

Nếu vẫn muốn chốt chặn ở DB thì dùng `BEFORE INSERT OR UPDATE` trigger —
trigger được phép gọi hàm không IMMUTABLE. Sprint 0 chưa cần.

---

### A2. Index dùng `unaccent()` — đã bọc lại

```sql
-- schema.sql gốc
CREATE INDEX idx_thieu_nhi_ho_ten
    ON thieu_nhi USING gin (to_tsvector('simple', unaccent(ho_ten)));
```

Cùng loại lỗi. `unaccent(text)` dạng một tham số chỉ là `STABLE`: nó tra từ
điển `unaccent` theo `search_path` hiện tại, mà `search_path` đổi được giữa
hai session — nên PostgreSQL không dám coi kết quả là cố định.

**Thay bằng** hàm bọc dùng dạng hai tham số, chỉ đích danh từ điển:

```sql
CREATE OR REPLACE FUNCTION f_unaccent(txt text)
    RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
RETURN public.unaccent('public.unaccent'::regdictionary, txt);
```

Lúc này khai báo `IMMUTABLE` là trung thực, index tạo được.

> Khi truy vấn tìm kiếm ở Sprint 4, phải dùng **đúng** `f_unaccent(ho_ten)`
> chứ không phải `unaccent(ho_ten)` — viết khác đi thì PostgreSQL không nhận
> ra là cùng biểu thức và sẽ bỏ qua index, quét toàn bảng.

---

## B. Bổ sung để ép được quy tắc nghiệp vụ

### B1. `ghi_danh.nam_hoc_id` — cột mới

`docs/02` mục 3.4 quy định:

> một thiếu nhi chỉ có tối đa một ghi danh `DANG_HOC` trong cùng một năm học

Quy tắc này **không thể ép bằng ràng buộc DB** với thiết kế gốc, vì `ghi_danh`
chỉ có `lop_id`, còn năm học nằm ở `lop_hoc`. Partial unique index đòi hỏi mọi
cột phải nằm trên cùng một bảng.

Để nó chỉ tồn tại trong code Java là không đủ: hai request đồng thời cùng đọc
"chưa có ghi danh nào", rồi cùng ghi — kiểm tra ở service không chặn được.

**Đã thêm** cột `nam_hoc_id` vào `ghi_danh`, kèm:

```sql
CREATE UNIQUE INDEX uq_ghi_danh_dang_hoc
    ON ghi_danh (thieu_nhi_id, nam_hoc_id) WHERE trang_thai = 'DANG_HOC';
```

**Cái giá phải trả** là dữ liệu lặp: `ghi_danh.nam_hoc_id` có thể lệch với
`lop_hoc.nam_hoc_id`. Khoá chặn bằng khoá ngoại **ghép**:

```sql
-- ở lop_hoc
CONSTRAINT uq_lop_id_nam UNIQUE (id, nam_hoc_id)

-- ở ghi_danh
CONSTRAINT fk_ghi_danh_lop_nam FOREIGN KEY (lop_id, nam_hoc_id)
    REFERENCES lop_hoc (id, nam_hoc_id) ON DELETE CASCADE
```

Cặp `(lop_id, nam_hoc_id)` buộc phải khớp đúng một dòng có thật trong
`lop_hoc`, nên không thể ghi lệch. Đây là kỹ thuật chuẩn khi phải phi chuẩn hoá
(denormalize) một cột để phục vụ ràng buộc.

---

### B2. `phieu_ra_cong.nam_hoc_id` — cột mới

`docs/04` quy định topic WebSocket là `/topic/phieu-ra-cong/{namHocId}`, nhưng
bảng gốc không có đường nào tới `nam_hoc`: `ghi_danh_id` lại cho phép `NULL`.
Không có cột này thì server không biết đẩy bản tin vào topic nào.

---

### B3. Cột audit ở mọi bảng

`docs/03` mục 9 nói mọi bảng đều có `ngay_tao`, `ngay_cap_nhat`,
`nguoi_tao_id`, `nguoi_cap_nhat_id`, nhưng `schema.sql` chỉ có hai cột đầu, và
cũng không đủ ở mọi bảng.

Đã bổ sung cho đủ, vì `BaseEntity` (`@MappedSuperclass` + JPA Auditing) ánh xạ
cả bốn cột — thiếu một cột là Hibernate báo lỗi ngay lúc khởi động do
`ddl-auto: validate`.

**Ngoại lệ có chủ đích:**

| Bảng | Vì sao không dùng `BaseEntity` |
|---|---|
| `phieu_ra_cong` | `thoi_gian_tao`, `nguoi_tao_id` ở đây là **dữ liệu nghiệp vụ** (ai xin cho em về, lúc mấy giờ) — được hiển thị cho người trực cổng đọc, không phải cột kỹ thuật |
| `nhat_ky_he_thong` | Bản thân nó là audit log, dùng `thoi_gian` |
| `cau_hinh` | Khoá chính là `khoa VARCHAR`, không phải UUID |
| `nguoi_dung_vai_tro`, `thanh_vien_to_truc` | Bảng nối, khoá chính ghép, không có id riêng |

---

## C. Quyết định hoãn lại

### C1. UUID v7 — chưa làm

`docs/03` mục 1 khuyến nghị UUID v7 để index bớt phân mảnh. PostgreSQL 16
chưa có hàm `uuidv7()` dựng sẵn (từ PG18 mới có), nên migration đang dùng
`gen_random_uuid()` (v4).

Hoãn tới Sprint 4, khi bảng `thieu_nhi` bắt đầu có vài nghìn dòng — lúc đó mới
đo được khác biệt thật thay vì tối ưu theo cảm tính. Cách làm khi tới lúc: sinh
UUID v7 ở tầng Java bằng một `IdentifierGenerator` riêng cho Hibernate.

### C2. Công thức điểm trung bình — cần xác nhận

`docs/02` mục 4.2 ghi `diem_tb = (diem_hk1 + diem_hk2 * 2) / 3` và **chính tài
liệu tự đánh dấu là cần xác nhận lại với ban điều hành**.

Migration `V2` đã tách thành hai khoá cấu hình `he_so_hk1` và `he_so_hk2` để
công thức thành tổng quát:

```
diem_tb = (hk1 * he_so_hk1 + hk2 * he_so_hk2) / (he_so_hk1 + he_so_hk2)
```

Nếu ban điều hành đổi sang hệ số 1:1 thì chỉ cần `UPDATE cau_hinh`, không phải
build lại và deploy lại ứng dụng.

**Việc cần làm:** hỏi ban điều hành trước khi bắt đầu Sprint 6.

---

## D. Rủi ro đã ghi nhận, xử lý ở sprint sau

### D1. Refresh token cookie khi frontend và backend khác domain — Sprint 1 & 8

`docs/04` quy định refresh token lưu trong HttpOnly cookie. Ở dev không sao vì
Vite proxy làm cho hai bên cùng origin.

Nhưng theo `CLAUDE.md`, production sẽ là frontend trên **Vercel**, backend trên
**VPS** — hai site khác nhau thật sự. Cookie khi đó bắt buộc:

```
Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=None; Path=/api/v1/auth
```

Kèm theo:
- Cả hai đầu **phải** chạy HTTPS (`SameSite=None` không có `Secure` bị trình
  duyệt bỏ qua).
- CORS phải bật `allowCredentials` và liệt kê origin cụ thể, **không được**
  dùng `*` (chuẩn CORS cấm kết hợp `*` với credentials).
- Safari và trình duyệt chặn cookie bên thứ ba có thể vẫn chặn. Nếu gặp, phương
  án là trỏ backend về subdomain cùng site, VD `api.tenmien.vn` với frontend ở
  `tenmien.vn` — lúc đó `SameSite=Lax` là đủ.

**Nên quyết định tên miền trước khi làm Sprint 1**, vì nó đổi cả cách viết
phần auth.

### D2. Ràng buộc "chỉ điểm danh trong 7 ngày gần nhất" — Sprint 5

`docs/02` mục 4.1. Không ép được bằng `CHECK` (cùng lý do A1 — cần
`CURRENT_DATE`). Sẽ kiểm ở tầng service, đọc ngưỡng từ
`cau_hinh.so_ngay_sua_diem_danh`.

### D3. Năm học `DA_KET_THUC` là chỉ đọc — Sprint 6

`docs/02` bước 1. Cũng phải ép ở tầng service, hoặc bằng trigger nếu muốn chắc
chắn hơn. Chưa có gì trong schema hiện tại chặn việc sửa dữ liệu của năm học
đã kết thúc.
