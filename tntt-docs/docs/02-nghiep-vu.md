# 02 — Quy trình nghiệp vụ

Hệ thống vận hành theo trục **Năm học**, luồng logic **từ trên xuống** (Top-Down).

---

## Bước 1 — Thiết lập nền móng

Không có gì hoạt động được nếu hệ thống chưa biết đang ở năm học nào.

1. **Tạo Năm học** (VD: `2026-2027`). Mọi dữ liệu vận hành (ghi danh, điểm danh, điểm số,
   lịch trực) đều gắn với một năm học.
   - Chỉ **một** năm học ở trạng thái `DANG_HOAT_DONG` tại một thời điểm.
   - Năm học cũ chuyển sang `DA_KET_THUC` → dữ liệu chỉ đọc, không sửa được nữa.
2. **Tạo Ngành** — Chiên Con, Ấu Nhi, Thiếu Nhi, Nghĩa Sĩ, Hiệp Sĩ. Mỗi ngành có khoảng
   tuổi và thứ tự (`thu_tu`) để phục vụ chuyển cấp.
3. **Tạo Lớp học** trong năm học đó, mỗi lớp thuộc một ngành (VD: Ấu 1A thuộc ngành Ấu Nhi).

**Quy tắc**: tên lớp là duy nhất trong phạm vi một năm học.

---

## Bước 2 — Quản trị nhân sự và phân quyền

1. **Tạo hồ sơ Huynh trưởng**: Tên Thánh, Họ tên, Ngày sinh, SĐT, Email.
2. **Cấp tài khoản**: username là email hoặc SĐT. Mật khẩu tạm do hệ thống sinh, bắt buộc
   đổi ở lần đăng nhập đầu tiên.
3. **Gán vai trò** (một người có thể nhiều vai trò).
4. **Phân công lớp**: gán Huynh trưởng A làm chủ nhiệm/phụ tá lớp Ấu 1A của năm 2026-2027.
   Phân công gắn với **năm học**, sang năm phân công lại.
5. **Phân công ngành**: gán Khối trưởng phụ trách một ngành trong năm học đó.

### Ma trận phân quyền

| Hành động | ADMIN | KHOI_TRUONG | HUYNH_TRUONG | KY_LUAT |
|---|:--:|:--:|:--:|:--:|
| Tạo/sửa năm học, ngành, lớp | ✅ | ❌ | ❌ | ❌ |
| Tạo tài khoản, gán vai trò | ✅ | ❌ | ❌ | ❌ |
| Xem hồ sơ thiếu nhi toàn xứ | ✅ | ❌ | ❌ | ❌ |
| Xem hồ sơ thiếu nhi trong ngành mình | ✅ | ✅ | ❌ | ❌ |
| Xem hồ sơ thiếu nhi lớp mình | ✅ | ✅ | ✅ | ❌ |
| Sửa hồ sơ thiếu nhi | ✅ | ✅ | ❌ | ❌ |
| Import Excel | ✅ | ❌ | ❌ | ❌ |
| Điểm danh lớp mình | ✅ | ✅ | ✅ | ❌ |
| Nhập điểm lớp mình | ✅ | ✅ | ✅ | ❌ |
| Xét & thực hiện chuyển cấp | ✅ | ❌ | ❌ | ❌ |
| Tạo phiếu ra cổng | ✅ | ✅ | ✅ | ❌ |
| Xem màn hình trực cổng | ✅ | ❌ | ❌ | ✅ |
| Xác nhận đã ra cổng | ✅ | ❌ | ❌ | ✅ |
| Xem báo cáo toàn xứ | ✅ | ❌ | ❌ | ❌ |

**Nguyên tắc thực thi**: kiểm tra quyền ở tầng **Service**, không chỉ ẩn nút trên UI.
Với `HUYNH_TRUONG`, mọi truy vấn phải kèm điều kiện "lớp này có được phân công cho tôi
trong năm học đang hoạt động không".

---

## Bước 3 — Số hoá hồ sơ thiếu nhi

Đây là bước nặng nhất: 1.000 hồ sơ.

### 3.1 Hồ sơ gốc (bất biến theo năm)
- Mã thiếu nhi (VD: `TN2026001`) — duy nhất, tự sinh
- Tên Thánh, Họ và Tên, Ngày sinh, Giới tính
- Tên Bố, Tên Mẹ, SĐT phụ huynh, Địa chỉ
- Giáo họ (nếu xứ chia theo giáo họ)

### 3.2 Lịch sử bí tích
Mỗi bí tích là **một dòng riêng** (quan hệ 1-N), không nhét chung một bảng phẳng, vì:
- Có em lãnh nhận ở xứ khác → cần ghi nơi cử hành riêng cho từng bí tích.
- Ngoài Rửa Tội / Rước Lễ / Thêm Sức còn có Xưng Tội lần đầu, Bao Đồng.
- Có em chuyển đến giữa chừng chưa có bí tích nào.

Loại bí tích: `RUA_TOI`, `XUNG_TOI_LAN_DAU`, `RUOC_LE_LAN_DAU`, `THEM_SUC`, `BAO_DONG`.
Mỗi loại lưu: ngày cử hành, nơi cử hành, cha chủ sự, tên người đỡ đầu, số sổ.

### 3.3 Import hàng loạt từ Excel
Bắt buộc phải có, không thể bắt ban điều hành nhập tay 1.000 dòng.

Luồng:
1. Admin tải **file mẫu Excel** từ hệ thống (có sẵn header chuẩn và sheet hướng dẫn).
2. Điền dữ liệu, upload lên web.
3. Hệ thống **validate trước, chưa lưu**: trả về bảng preview đánh dấu dòng lỗi
   (thiếu tên, sai định dạng ngày, trùng mã, trùng họ tên + ngày sinh).
4. Admin xem preview, sửa file hoặc chọn bỏ qua dòng lỗi.
5. Xác nhận → hệ thống ghi vào DB trong **một transaction**.
6. Trả về báo cáo: bao nhiêu dòng thành công, bao nhiêu bị bỏ qua và lý do.

### 3.4 Ghi danh (Enrollment)
Đẩy thiếu nhi vào lớp của năm học hiện tại. Đây là bảng trung gian N-N vì một em qua nhiều
năm sẽ học nhiều lớp. Trạng thái ghi danh: `DANG_HOC`, `CHUYEN_XU`, `NGHI_HOC`, `HOAN_THANH`.

**Quy tắc**: một thiếu nhi chỉ có tối đa một ghi danh `DANG_HOC` trong cùng một năm học.

---

## Bước 4 — Vận hành hằng tuần

Đây là luồng 150 huynh trưởng dùng mỗi Chủ Nhật, trên điện thoại.

### 4.1 Điểm danh
1. Huynh trưởng đăng nhập → hệ thống tự nhận diện lớp được phân công.
2. Hiện danh sách thiếu nhi của lớp, mặc định "có mặt".
3. Với từng em, đánh dấu ba trường độc lập:
   - `di_le` — có dự Thánh lễ không
   - `di_hoc` — có học giáo lý không
   - `co_phep` — vắng có phép hay không phép
4. Ghi chú tự do (tuỳ chọn).
5. Bấm Lưu → ghi cả buổi trong một lần gọi API (batch), không gọi API từng em.

**Quy tắc**:
- Mỗi lớp chỉ một bản ghi điểm danh cho mỗi ngày (unique `(ghi_danh_id, ngay_diem_danh)`).
- Gọi lại API cùng ngày = cập nhật, không tạo trùng.
- Chỉ được điểm danh cho ngày hiện tại hoặc trong vòng 7 ngày trước (tránh sửa lịch sử xa).
- Sau khi năm học kết thúc, không sửa được nữa.

### 4.2 Nhập điểm
- Nhập điểm HK1 và HK2 theo lớp, dạng bảng, lưu batch.
- Hệ thống tự tính `diem_tb = (diem_hk1 + diem_hk2 * 2) / 3` (hệ số HK2 nhân đôi — **xác nhận
  lại công thức với ban điều hành trước khi code cứng**; nên để công thức cấu hình được).

---

## Bước 5 — Tổng kết và chuyển cấp

Cuối năm học (khoảng tháng 5):

1. **Xét kết quả**: hệ thống tính `diem_tb` và `ti_le_chuyen_can` (số buổi có mặt / tổng buổi).
   Tiêu chí đạt mặc định: `diem_tb >= 5.0` **và** `ti_le_chuyen_can >= 70%`.
   Ngưỡng này phải cấu hình được, không hardcode.
2. **Xuất báo cáo** danh sách Đạt / Phải học lại, có thể tải Excel.
3. **Chuyển cấp**: Admin bấm một nút.
   - Hệ thống tạo năm học mới (nếu chưa có) và các lớp tương ứng.
   - Em Đạt ở lớp Ấu 1 → ghi danh vào lớp Ấu 2 của năm mới.
   - Em Không đạt → ghi danh lại vào lớp cùng cấp của năm mới.
   - Em ở lớp cuối ngành Hiệp Sĩ và Đạt → trạng thái `HOAN_THANH`.
   - Toàn bộ chạy trong **một transaction**, có màn hình preview trước khi xác nhận,
     và ghi log để có thể rollback thủ công.
4. Năm học cũ chuyển sang `DA_KET_THUC`, chỉ đọc.

---

## Bước 6 — Ban Kỷ luật và An ninh cổng

### 6.1 Thiết lập
1. Admin tạo các **Tổ trực** (VD: "Tổ Kỷ Luật 1", "Tổ Ấu Nhi").
2. Gán huynh trưởng vào tổ (N-N).
3. Lập **lịch trực luân phiên**: Tổ A trực Chủ Nhật tuần 1, Tổ B tuần 2...
   Mỗi bản ghi lịch trực gồm: tổ, ngày trực, ca trực (VD: "Thánh lễ thiếu nhi 7h30").

### 6.2 Luồng phiếu ra cổng (Exit Pass)

```
[Giáo lý viên]                [Server]                  [Người trực cổng]
     |                            |                            |
  Em A ốm, phụ huynh xin về sớm   |                            |
     |-- POST /exit-passes ------>|                            |
     |                       lưu DB (CHO_RA_CONG)              |
     |                            |-- WebSocket broadcast ---->|
     |                            |                     🔔 "Em Nguyễn Văn A
     |                            |                        (Ấu 1A) được về.
     |                            |                        Lý do: Ốm"
     |                            |                            |
     |                            |          Phụ huynh đến đón, bấm Xác nhận
     |                            |<-- PATCH /exit-passes/{id}/confirm --|
     |                       ghi thoi_gian_ra_cong             |
     |                       trạng thái = DA_RA_CONG           |
     |                            |-- WebSocket update ------->|
```

**Trạng thái phiếu**: `CHO_RA_CONG` → `DA_RA_CONG` | `HUY`

**Quy tắc**:
- Chỉ người có vai trò trực trong ca hiện tại mới thấy màn hình trực cổng và xác nhận được.
- Phiếu chỉ có hiệu lực trong ngày tạo, cuối ngày tự chuyển `HUY` nếu chưa xác nhận.
- Một em không thể có hai phiếu `CHO_RA_CONG` cùng lúc.
- Lưu vĩnh viễn lịch sử: ai tạo, lý do, ai xác nhận, thời điểm ra cổng.

### 6.3 Kỹ thuật real-time
- Spring WebSocket với STOMP over SockJS (fallback cho mạng yếu).
- Topic: `/topic/exit-pass/{namHocId}` — broadcast cho mọi client đang mở màn hình trực cổng.
- Frontend dùng `@stomp/stompjs` + `sockjs-client`.
- **Bắt buộc có fallback**: nếu WebSocket đứt, màn hình trực cổng tự polling mỗi 10 giây.
  Mạng ở nhà thờ không ổn định, không được phụ thuộc hoàn toàn vào socket.
- Xác thực WebSocket bằng chính JWT (gửi trong header lúc CONNECT).

---

## Báo cáo cần có

| Báo cáo | Đối tượng | Xuất Excel |
|---|---|---|
| Danh sách thiếu nhi theo lớp | HT, KT, Admin | ✅ |
| Sổ điểm danh theo lớp / tháng | HT, KT, Admin | ✅ |
| Bảng điểm tổng kết theo lớp | HT, KT, Admin | ✅ |
| Danh sách đạt / học lại toàn xứ | Admin | ✅ |
| Danh sách thiếu nhi chưa lãnh nhận bí tích X | Admin | ✅ |
| Lịch sử phiếu ra cổng theo ngày | Admin, Ban KL | ✅ |
| Thống kê chuyên cần toàn xứ theo tuần | Admin | ❌ |
