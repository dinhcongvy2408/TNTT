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

---

## E. Sửa sau khi rà soát cuối Sprint 0

Ghi ngày 27/08/2026, sau một lượt kiểm tra toàn bộ code base.

### E1. Hash mật khẩu admin trong `schema.sql` gốc là SAI — đã sửa ở V4

`schema.sql` (và `V3__tai_khoan_admin.sql` chép theo) ghi:

```sql
-- Mật khẩu mặc định: Admin@123
'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
```

Chú thích đó sai. Đem hash đi đối chiếu thì nó **không khớp `Admin@123`**, mà
cũng không khớp bất kỳ mật khẩu phổ biến nào — tức là tài khoản quản trị duy
nhất của hệ thống không đăng nhập được bằng cách nào cả.

Cách kiểm chứng (không cần cài gì, `pgcrypto` đã bật từ V1):

```sql
SELECT crypt('Admin@123', mat_khau_hash) = mat_khau_hash
FROM nguoi_dung WHERE email = 'admin@xudoan.local';
```

`crypt(mat_khau, hash_day_du)` tự rút phần salt nằm sẵn trong hash để băm lại,
nên so sánh được — đây cũng chính là cách `BCryptPasswordEncoder.matches()`
hoạt động.

**Đã sửa** bằng `V4__sua_mat_khau_admin.sql`, hash mới sinh bằng
`crypt('Admin@123', gen_salt('bf', 10))` và đã đối chiếu lại: khớp `Admin@123`,
từ chối mật khẩu sai. Không sửa V3 vì Flyway ghi checksum của file đã chạy.

> **Bài học chung:** một credential chép từ tài liệu mà chưa ai đăng nhập thử
> thì chưa được coi là đúng. Sprint 1 nên có một test tự động cho việc này.

### E2. `@PreAuthorize` bị từ chối sẽ trả 500 thay vì 403 — ĐÃ SỬA

`@PreAuthorize` kiểm quyền lúc **gọi method**, tức bên trong DispatcherServlet.
`ExceptionTranslationFilter` của Spring Security nằm ở tầng filter, đã chạy
xong từ trước, nên không bắt được. Exception rơi thẳng vào
`GlobalExceptionHandler` và bị lưới `Exception.class` vợt mất → 500.

**Đã sửa.** Thêm `spring-boot-starter-security` sớm hơn Sprint 1 một nhịp, kèm
`security/config/SecurityConfig.java` để `@PreAuthorize` hoạt động được, và bật
handler ở `GlobalExceptionHandler` mục 4b.

`SecurityConfig` hiện **để `permitAll` toàn bộ** — nó chưa phải hàng rào bảo vệ.
Nó có mặt vì hai lý do, cả hai đều là "không có nó thì hỏng ngầm":

1. `@EnableMethodSecurity` là thứ bật `@PreAuthorize`. Thiếu annotation này,
   mọi `@PreAuthorize` bị bỏ qua **hoàn toàn trong im lặng** — nguy hiểm hơn
   hẳn việc chưa viết gì, vì nhìn code thì tưởng đã phân quyền.
2. Có lớp `AccessDeniedException` trong classpath thì handler mới biên dịch được.

Hai điểm phải nhớ khi Sprint 1 khoá endpoint lại:

- `.cors(Customizer.withDefaults())` là bắt buộc. Filter chain của Security chạy
  **trước** Spring MVC; không khai báo thì nó chặn preflight OPTIONS trước khi
  MVC kịp trả header CORS.
- `OPTIONS /**` phải luôn `permitAll`. Trình duyệt gửi preflight **không kèm**
  header `Authorization`, nên bắt xác thực ở đó là mọi lời gọi từ frontend chết
  ngay từ bước đầu.

Đã tắt `UserDetailsServiceAutoConfiguration` trong `application.yml`: có
starter-security mà chưa có `UserDetailsService` thì Spring Boot tự tạo user
`user` kèm mật khẩu ngẫu nhiên và in ra log mỗi lần khởi động — dễ bị nhầm là
mật khẩu admin thật. Sprint 1 viết `UserDetailsService` đọc từ DB thì bỏ dòng
exclude đó đi.

**Test chốt chặn:** `GlobalExceptionHandlerAccessDeniedTest` kiểm chứng cả
`AccessDeniedException` lẫn `AuthorizationDeniedException` (lớp con, chính là
thứ `@PreAuthorize` ném ra) đều thành 403 + `ACCESS_DENIED`. Đã thử gỡ handler
ra để chắc test không phải test giả: nó đỏ đúng như mong đợi,
`Status expected:<403> but was:<500>`.

Phân biệt: `AuthenticationException` (chưa đăng nhập, token hỏng) thì **không**
tới được `@RestControllerAdvice` — nó bị chặn ở tầng filter và xử lý bằng
`AuthenticationEntryPoint`.

### E3. `Content-Type` cứng ở axios sẽ làm hỏng import Excel — đã sửa

`services/http.ts` khai báo `headers: { 'Content-Type': 'application/json' }` ở
cấp instance, nên nó áp cho **mọi** request. Khi Sprint 4 gửi `FormData`, axios
lẽ ra tự đặt `multipart/form-data; boundary=...`; header cứng đè lên, server
mất `boundary` và không tách được file.

**Đã bỏ** dòng đó. Body là object thường thì axios tự đặt `application/json`.

### E4. `bocData` nói dối kiểu trả về — đã sửa

Hàm hứa trả `T` nhưng thân hàm là `return body.data as T`, trả `null` khi
backend gửi `data` rỗng. `as` vô hiệu hoá đúng thứ mà `strict` mode sinh ra để
chặn, và lỗi chỉ nổ vài lớp sau ở một dòng chẳng liên quan.

**Đã sửa:** `bocData` ném `ApiError` khi `data` rỗng. Endpoint không có dữ liệu
trả về (xoá, đổi trạng thái) dùng `httpVoid` — một export riêng, nhìn lời gọi
là biết ngay endpoint thuộc loại nào.

### E5. `@Min` trong `AppProperties.Jwt` không bao giờ chạy — đã sửa

Bean Validation **không** tự đệ quy vào object lồng nhau. Thiếu `@Valid` trên
component `jwt` thì mọi `@Min` bên trong chỉ là trang trí — và không có cảnh
báo nào cho biết ràng buộc đã bị bỏ qua.

**Đã thêm** `@Valid`. Sprint 1 thêm tiếp `@NotBlank` + độ dài tối thiểu cho
`secret`: application.yml đang để mặc định là chuỗi rỗng.

### E6. Dọn repo

| Việc | Vì sao |
|---|---|
| Xoá `tntt-docs/` | Trùng nội dung với `docs/` + `schema.sql` + `CLAUDE.md`, và `tntt-docs/CLAUDE.md` **đã lệch** với bản gốc. Hai nguồn sự thật cho cùng một tài liệu chắc chắn dẫn tới đọc nhầm bản cũ. |
| Đổi nhánh `master` → `main` | `CLAUDE.md` mục 5 viết "merge vào `main`". Đổi trước khi có remote thì rẻ, sau thì phiền. |
| Thêm Maven wrapper (`mvnw`) | Ghim phiên bản Maven trong repo: CI và máy cá nhân chạy y hệt nhau, máy mới không cần cài sẵn Maven. |
| Thêm `.github/workflows/ci.yml` | Quy định "merge qua PR" chỉ có giá trị khi PR được kiểm tra tự động. Backend `verify`, frontend `lint` + `build` (đã gồm `tsc` strict). |

---

## F. Sprint 2 — Tổ chức

### F1. Thiếu endpoint kích hoạt năm học — đã thêm

`docs/04` mục Tổ chức chỉ liệt kê:

```
POST  /nam-hoc
PATCH /nam-hoc/{id}/ket-thuc
```

Nhưng `V1__init_schema.sql` đặt `trang_thai` mặc định là `CHUAN_BI`, còn
`docs/02` bước 1 đòi phải có một năm học `DANG_HOAT_DONG`. **Không đặc tả nào
nối hai đầu lại** — theo đúng tài liệu thì năm học tạo ra rồi nằm mãi ở
`CHUAN_BI` và hệ thống không bao giờ chạy được.

Có hai cách vá:

| Cách | Vấn đề |
|---|---|
| `POST /nam-hoc` tạo thẳng ở `DANG_HOAT_DONG` | Tạo một bản ghi và đổi năm học của cả xứ đoàn trở thành cùng một thao tác. Admin chuẩn bị trước năm sau vào tháng 3 sẽ vô tình cắt ngang năm đang chạy. |
| Thêm `PATCH /nam-hoc/{id}/kich-hoat` | Thêm một endpoint ngoài đặc tả. |

**Đã chọn cách 2.** Đổi năm học là quyết định ảnh hưởng toàn hệ thống, phải là
một hành động có ý thức chứ không phải hệ quả phụ của việc tạo bản ghi. Máy
trạng thái thành:

```
CHUAN_BI ──kich-hoat──▶ DANG_HOAT_DONG ──ket-thuc──▶ DA_KET_THUC
```

Một chiều, không có đường lùi: mở lại năm đã kết thúc nghĩa là cho sửa điểm và
điểm danh của năm cũ, thứ `docs/02` bước 1 cấm.

### F2. `@CreatedDate` không chạy với `OffsetDateTime` — đã sửa

Bug này **có từ Sprint 0** nhưng nằm im vì lúc đó chưa entity nào được lưu
xuống DB. Lần `save()` đầu tiên (tạo năm học) mới lộ ra:

```
InvalidDataAccessApiUsageException: Cannot convert unsupported date type
java.time.LocalDateTime to java.time.OffsetDateTime
```

`BaseEntity` khai báo hai cột thời gian là `OffsetDateTime` (khớp `TIMESTAMPTZ`
của PostgreSQL). Nhưng mặc định Spring Data dùng `CurrentDateTimeProvider`, thứ
trả về `LocalDateTime`, và nó **không có** đường chuyển sang `OffsetDateTime` —
hợp lý, vì `LocalDateTime` không biết mình thuộc múi giờ nào, đoán bừa là sai.

**Đã sửa** bằng một bean `DateTimeProvider` trả thẳng `OffsetDateTime.now()`
trong `JpaAuditingConfig`, và trỏ tới nó qua
`@EnableJpaAuditing(dateTimeProviderRef = ...)`.

> Bài học: một đoạn cấu hình chưa từng chạy thì chưa được coi là đúng. Lần rà
> soát cuối Sprint 0 không bắt được lỗi này vì không có gì để bắt.

### F3. Phân quyền tạm mở — PHẢI siết ở Sprint 1

`docs/02` quy định "Tạo/sửa năm học, ngành, lớp" chỉ `ADMIN` được làm. Nhưng
Sprint 1 chưa xong nên chưa ai đăng nhập được: ghi `hasRole('ADMIN')` ngay bây
giờ thì mọi request đều 403 và màn hình vô dụng.

Các endpoint ghi tạm để `@PreAuthorize("permitAll()")`, khai báo **tường minh**
chứ không bỏ trống — bỏ trống thì không phân biệt được "đã cân nhắc" với
"bị quên". Tìm lại tất cả bằng:

```bash
grep -rn "SPRINT 1: doi quyen" backend/
```

### F4. `GET /lop/cua-toi` và "lọc theo quyền" — hoãn sang Sprint 3

`docs/04` liệt kê `GET /lop/cua-toi` (quyền `HUYNH_TRUONG`) và ghi `GET /lop`
là "tất cả (lọc theo quyền)". Cả hai đều cần biết **ai đang gọi** (Sprint 1) và
cần bảng `phan_cong` (Sprint 3). Hiện `GET /lop` trả mọi lớp của năm học được
chọn, không lọc gì.

Đây là món nợ có thật, không phải chuyện nhỏ: docs/02 quy định huynh trưởng chỉ
được xem lớp mình phụ trách. Ghi vào checklist Sprint 3.

### F5. Ba hàng rào ở `LopHocService` — không có trong đặc tả

Đặc tả chỉ mô tả CRUD. Ba quy tắc dưới đây được thêm vì thiếu chúng thì mất dữ
liệu thật, không phải chỉ khó chịu.

**a. Năm học `DA_KET_THUC` là chỉ đọc.** docs/02 bước 1 có nói, nhưng docs/99
mục D3 xếp việc ép quy tắc này sang Sprint 6. Thực tế phải làm ngay ở đây: nếu
không, admin sửa được lớp của năm cũ và làm lệch dữ liệu lịch sử. Ép ở tầng
service vì ràng buộc `CHECK` của PostgreSQL không tham chiếu được sang bảng khác.

**b. Không đổi `nam_hoc_id` của một lớp đã tồn tại.** Bảng `ghi_danh` có khoá
ngoại **ghép** trỏ vào cặp `(id, nam_hoc_id)` của `lop_hoc` (docs/99 mục B1).
Đổi năm học của lớp là mọi ghi danh cũ trỏ vào một cặp không còn tồn tại;
PostgreSQL sẽ từ chối và người dùng nhận một lỗi ràng buộc khó hiểu. Chặn sớm ở
service cho ra câu giải thích rõ ràng. Về nghiệp vụ cũng đúng: "Ấu 1A năm
2026-2027" và "Ấu 1A năm 2027-2028" là hai lớp khác nhau.

**c. Không xoá lớp đang có ghi danh.** Đây là hàng rào quan trọng nhất.
Migration V1 khai `ghi_danh.lop_id ... ON DELETE CASCADE`, và
`diem_danh.ghi_danh_id`, `diem_so.ghi_danh_id` cũng CASCADE tiếp. Một lệnh
`DELETE` lên lớp có 30 em sẽ xoá sạch 30 ghi danh, toàn bộ điểm danh cả năm và
toàn bộ điểm số — im lặng, không hỏi lại, không hoàn tác.

Đếm ghi danh bằng native query vì entity `GhiDanh` tới Sprint 5 mới có. Không
chờ được: cái CASCADE đã nằm trong DB từ V1 rồi.

> Cân nhắc cho Sprint 5: có nên đổi các khoá ngoại đó sang `ON DELETE RESTRICT`
> để chính DB từ chối, thay vì dựa vào tầng service nhớ kiểm tra? Cần một
> migration mới.

### F6. Đo thật bài toán N+1

Quan hệ `LopHoc → Nganh` và `LopHoc → NamHoc` khai `FetchType.LAZY`, và truy vấn
danh sách dùng `JOIN FETCH`. Đã đo bằng cách bật `logging.level.org.hibernate.SQL=DEBUG`
rồi gọi `GET /lop?namHocId=...` với 6 lớp thuộc 3 ngành:

```
Số câu SELECT sinh ra: 1
```

Nếu để mặc định (`@ManyToOne` là EAGER) hoặc LAZY mà quên `JOIN FETCH`, con số
sẽ là 1 + 6 + 6 = 13, và tăng tuyến tính theo số lớp. Với một xứ đoàn 40 lớp thì
đó là 81 câu truy vấn cho một lần mở màn hình.

---

## G. Sprint 1 — Xác thực và phân quyền

### G1. Bảng `refresh_token` — bổ sung, migration V5

`docs/04` quy định `POST /auth/logout` phải "thu hồi refresh token", nhưng
`schema.sql` gốc không có bảng nào để lưu token. Không có nơi lưu thì không thu
hồi được.

Hai cách:

| Cách | Vấn đề |
|---|---|
| Refresh token là JWT tự chứa | Không vô hiệu hoá được trước hạn. Kẻ lấy được token vẫn dùng đủ 7 ngày dù người dùng đã bấm đăng xuất VÀ đổi mật khẩu. |
| Lưu vào bảng, token là chuỗi ngẫu nhiên | Mỗi lần làm mới phải tra DB. |

**Đã chọn cách 2.** Refresh token chỉ dùng vài lần mỗi ngày nên chi phí tra DB
không đáng kể, còn khả năng thu hồi thì bắt buộc phải có với hệ thống giữ hồ sơ
trẻ em.

**Lưu bản băm SHA-256, không lưu token gốc.** Cùng lý do với mật khẩu: backup
thất lạc hay lộ quyền đọc DB thì kẻ đọc được vẫn không đăng nhập thay ai được.
Dùng SHA-256 chứ không BCrypt — ngược với mật khẩu — vì token đã là 32 byte
ngẫu nhiên, không có từ điển nào dò nổi, mà BCrypt thì cố tình chậm.

### G2. Hai loại token, hai thiết kế khác nhau

|  | Access token | Refresh token |
|---|---|---|
| Dạng | JWT có chữ ký | chuỗi ngẫu nhiên 256 bit |
| Sống | 30 phút | 7 ngày |
| Server lưu | không | có, dạng băm |
| Thu hồi được | **không** | **có** |
| Nằm ở đâu | biến JavaScript | cookie HttpOnly |

Access token không lưu ở server vì nó đi kèm MỌI request — bắt tra DB mỗi lần
là mất đúng cái lợi của JWT. Đổi lại nó không thu hồi được, nên chỉ sống 30
phút.

**Refresh token có XOAY VÒNG:** mỗi lần làm mới, token cũ bị thu hồi và cấp
token mới. Lợi ích không hiển nhiên: nếu token bị đánh cắp và kẻ trộm dùng
trước, thì lần làm mới kế tiếp của người dùng thật sẽ thất bại — họ bị đăng
xuất bất thường và **biết** có chuyện. Không xoay vòng thì cả hai bên cùng dùng
êm ru suốt 7 ngày.

### G3. Bắt đổi mật khẩu — ép ở BACKEND, không chỉ chuyển hướng ở frontend

`docs/02` bước 2: mật khẩu tạm phải đổi ở lần đăng nhập đầu. Nếu chỉ để frontend
chuyển hướng thì ai gọi thẳng API vẫn dùng được cả hệ thống bằng mật khẩu tạm —
mà mật khẩu tạm thường được nhắn qua Zalo và nằm lại đó mãi mãi.

`JwtAuthenticationFilter` chặn mọi đường dẫn trừ `/auth/me`,
`/auth/doi-mat-khau`, `/auth/logout` và `/health`, trả 403 với
`errorCode = CAN_DOI_MAT_KHAU`.

### G4. Không dùng `UserDetailsService` — có chủ đích

Checklist Sprint 1 ban đầu ghi "viết `UserDetailsService` đọc từ bảng
`nguoi_dung`". **Đã bỏ ý đó.**

`UserDetailsService` + `AuthenticationManager` là bộ đôi phục vụ form login theo
session: Spring gọi `loadUserByUsername`, so mật khẩu, tạo session. Hệ thống này
xác thực bằng JWT — `AuthService` so mật khẩu trực tiếp bằng `PasswordEncoder`,
còn danh tính từng request do `JwtAuthenticationFilter` dựng từ token. Thêm
`UserDetailsService` vào chỉ là một tầng gián tiếp không ai gọi tới.

Vì vậy dòng `spring.autoconfigure.exclude` cho
`UserDetailsServiceAutoConfiguration` được **giữ lại** (không có nó thì Spring
Boot tự tạo user `user` kèm mật khẩu ngẫu nhiên in ra log, dễ nhầm là mật khẩu
admin thật).

### G5. Thông báo đăng nhập thất bại phải GIỐNG HỆT nhau

"Email hoặc mật khẩu không đúng" dùng cho cả ba trường hợp: không có tài khoản,
sai mật khẩu, tài khoản bị khoá.

Tách ra thành "email không tồn tại" / "mật khẩu sai" là bất kỳ ai cũng dò được
danh sách email có thật trong hệ thống chỉ bằng cách thử. Với 150 huynh trưởng
thì đó là danh sách người thật kèm địa chỉ liên lạc thật.

Có test khoá chặt điều này: `AuthServiceTest.khongLoRaTaiKhoanNaoCoThat` so hai
chuỗi thông báo phải bằng nhau.

### G6. Frontend gộp chung một lần làm mới token

Màn hình điểm danh có thể bắn 5 request cùng lúc. Token vừa hết hạn thì cả 5
cùng nhận 401 và cùng gọi `/auth/refresh`. Vì backend XOAY VÒNG token, lần
refresh đầu thu hồi token cũ nên 4 lần sau thất bại — người dùng bị đăng xuất
oan giữa buổi điểm danh.

`services/http.ts` giữ một biến `dangLamMoi` chứa lời hứa của lần làm mới đang
chạy, để 5 request cùng chờ MỘT lần làm mới.

### G7. Món nợ đã trả

| Nợ | Trạng thái |
|---|---|
| 8 chỗ `@PreAuthorize("permitAll()")` tạm | Đã đổi: đọc là `isAuthenticated()`, ghi là `hasRole('ADMIN')` |
| `SecurityConfig` mở toàn bộ | Đã khoá, mặc định `anyRequest().authenticated()` |
| `auditorProvider()` luôn trả rỗng | Đã đọc từ `SecurityContextHolder`; `nguoi_tao_id` hết NULL |
| `app.jwt.secret` không ràng buộc | Đã thêm `@NotBlank` + tối thiểu 64 ký tự |
| Chưa có test mật khẩu admin | `MatKhauAdminTest` đọc thẳng file V4 và đối chiếu hash |

### G8. Vẫn còn nợ — Sprint 3

- `GET /lop/cua-toi` và lọc `GET /lop` theo phân công (mục F4). Sprint 1 đã cho
  biết "ai đang gọi", nhưng còn thiếu bảng `phan_cong`.
- Ma trận phân quyền ở `docs/02` phân biệt `KHOI_TRUONG` xem được ngành mình,
  `HUYNH_TRUONG` xem được lớp mình. Hiện mọi tài khoản đã đăng nhập đều đọc
  được toàn bộ danh sách lớp.
- Bảng `nhat_ky_he_thong` chưa được ghi (CLAUDE.md mục 6). `nguoi_tao_id` /
  `nguoi_cap_nhat_id` mới là một nửa yêu cầu.
- Chưa có tác vụ dọn `refresh_token` hết hạn. Bảng sẽ phình dần. Sprint 8.
