# 04 — Đặc tả API

Base URL: `/api/v1`

## Quy ước chung

**Response thành công**
```json
{ "success": true, "data": { ... }, "message": null }
```

**Response lỗi**
```json
{
  "success": false,
  "data": null,
  "message": "Bạn không có quyền truy cập lớp này",
  "errorCode": "FORBIDDEN_CLASS_ACCESS",
  "fieldErrors": { "ngaySinh": "Ngày sinh không được ở tương lai" }
}
```

**Phân trang**: `?page=0&size=20&sort=hoTen,asc` → trả về `data.content`, `data.totalElements`,
`data.totalPages`.

**Mã HTTP**: 200 OK, 201 Created, 400 Bad Request (validate), 401 Unauthorized (chưa đăng nhập),
403 Forbidden (không đủ quyền), 404 Not Found, 409 Conflict (trùng dữ liệu), 422 (nghiệp vụ sai).

**Xác thực**: `Authorization: Bearer <access_token>`. Access token sống 30 phút,
refresh token 7 ngày lưu trong HttpOnly cookie.

---

## Auth

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/auth/login` | công khai | Đăng nhập, trả access + refresh token |
| POST | `/auth/refresh` | công khai | Làm mới access token |
| POST | `/auth/logout` | đã đăng nhập | Thu hồi refresh token |
| GET | `/auth/me` | đã đăng nhập | Thông tin tài khoản + vai trò + lớp phụ trách |
| POST | `/auth/doi-mat-khau` | đã đăng nhập | Đổi mật khẩu |

---

## Tổ chức

| Method | Endpoint | Quyền |
|---|---|---|
| GET | `/nam-hoc` | tất cả |
| GET | `/nam-hoc/hien-tai` | tất cả |
| POST | `/nam-hoc` | ADMIN |
| PATCH | `/nam-hoc/{id}/ket-thuc` | ADMIN |
| GET | `/nganh` | tất cả |
| POST | `/nganh` | ADMIN |
| GET | `/lop?namHocId=&nganhId=` | tất cả (lọc theo quyền) |
| GET | `/lop/cua-toi` | HUYNH_TRUONG |
| POST | `/lop` | ADMIN |
| PUT | `/lop/{id}` | ADMIN |
| DELETE | `/lop/{id}` | ADMIN |

---

## Nhân sự

| Method | Endpoint | Quyền |
|---|---|---|
| GET | `/nguoi-dung?q=&vaiTro=&page=` | ADMIN |
| POST | `/nguoi-dung` | ADMIN |
| PUT | `/nguoi-dung/{id}` | ADMIN |
| PATCH | `/nguoi-dung/{id}/vai-tro` | ADMIN |
| PATCH | `/nguoi-dung/{id}/vo-hieu-hoa` | ADMIN |
| POST | `/nguoi-dung/{id}/reset-mat-khau` | ADMIN |
| GET | `/phan-cong?namHocId=` | ADMIN, KHOI_TRUONG |
| POST | `/phan-cong` | ADMIN |
| DELETE | `/phan-cong/{id}` | ADMIN |

---

## Thiếu nhi

| Method | Endpoint | Quyền | Ghi chú |
|---|---|---|---|
| GET | `/thieu-nhi?q=&lopId=&nganhId=&page=` | theo phạm vi | tìm theo tên/mã |
| GET | `/thieu-nhi/{id}` | theo phạm vi | kèm bí tích + lịch sử lớp |
| POST | `/thieu-nhi` | ADMIN, KHOI_TRUONG | |
| PUT | `/thieu-nhi/{id}` | ADMIN, KHOI_TRUONG | |
| DELETE | `/thieu-nhi/{id}` | ADMIN | soft delete |
| GET | `/thieu-nhi/mau-excel` | ADMIN | tải file mẫu |
| POST | `/thieu-nhi/import/preview` | ADMIN | upload → trả preview + lỗi |
| POST | `/thieu-nhi/import/xac-nhan` | ADMIN | ghi thật vào DB |

**Bí tích**

| Method | Endpoint | Quyền |
|---|---|---|
| GET | `/thieu-nhi/{id}/bi-tich` | theo phạm vi |
| POST | `/thieu-nhi/{id}/bi-tich` | ADMIN, KHOI_TRUONG |
| PUT | `/bi-tich/{id}` | ADMIN, KHOI_TRUONG |
| DELETE | `/bi-tich/{id}` | ADMIN |

---

## Ghi danh

| Method | Endpoint | Quyền |
|---|---|---|
| GET | `/lop/{lopId}/ghi-danh` | theo phạm vi |
| POST | `/ghi-danh` | ADMIN, KHOI_TRUONG |
| POST | `/ghi-danh/hang-loat` | ADMIN | xếp nhiều em vào một lớp |
| PATCH | `/ghi-danh/{id}/chuyen-lop` | ADMIN, KHOI_TRUONG |
| PATCH | `/ghi-danh/{id}/trang-thai` | ADMIN, KHOI_TRUONG |

---

## Điểm danh

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| GET | `/diem-danh?lopId=&ngay=` | theo lớp | lấy bảng điểm danh của buổi |
| POST | `/diem-danh/batch` | theo lớp | lưu cả lớp một lần (upsert) |
| GET | `/diem-danh/thong-ke?lopId=&tuNgay=&denNgay=` | theo lớp | tỉ lệ chuyên cần |

**Body của `/diem-danh/batch`**
```json
{
  "lopId": "uuid",
  "ngayDiemDanh": "2026-09-06",
  "items": [
    { "ghiDanhId": "uuid", "diLe": true, "diHoc": true, "coPhep": false, "ghiChu": null },
    { "ghiDanhId": "uuid", "diLe": false, "diHoc": true, "coPhep": true, "ghiChu": "Đi trễ" }
  ]
}
```

---

## Điểm số và chuyển cấp

| Method | Endpoint | Quyền |
|---|---|---|
| GET | `/diem-so?lopId=` | theo lớp |
| POST | `/diem-so/batch` | theo lớp |
| GET | `/tong-ket?namHocId=&lopId=` | ADMIN, KHOI_TRUONG |
| POST | `/chuyen-cap/preview` | ADMIN |
| POST | `/chuyen-cap/thuc-hien` | ADMIN |

---

## Ban Kỷ luật và Trực cổng

| Method | Endpoint | Quyền |
|---|---|---|
| GET | `/to-truc` | ADMIN, KY_LUAT |
| POST | `/to-truc` | ADMIN |
| POST | `/to-truc/{id}/thanh-vien` | ADMIN |
| GET | `/lich-truc?namHocId=&tuNgay=&denNgay=` | ADMIN, KY_LUAT |
| POST | `/lich-truc` | ADMIN |
| POST | `/lich-truc/tao-luan-phien` | ADMIN |
| GET | `/lich-truc/hom-nay` | KY_LUAT |
| POST | `/phieu-ra-cong` | HUYNH_TRUONG, KHOI_TRUONG, ADMIN |
| GET | `/phieu-ra-cong/dang-cho` | KY_LUAT, ADMIN |
| PATCH | `/phieu-ra-cong/{id}/xac-nhan` | KY_LUAT (đang trực ca), ADMIN |
| PATCH | `/phieu-ra-cong/{id}/huy` | người tạo, ADMIN |
| GET | `/phieu-ra-cong/lich-su?ngay=` | ADMIN, KY_LUAT |

**WebSocket**
- Endpoint: `/ws` (SockJS), xác thực bằng JWT trong STOMP CONNECT header.
- Subscribe: `/topic/phieu-ra-cong/{namHocId}`
- Payload đẩy về:
```json
{
  "type": "PHIEU_MOI" | "DA_XAC_NHAN" | "DA_HUY",
  "phieu": {
    "id": "uuid",
    "tenThanh": "Giuse",
    "hoTen": "Nguyễn Văn A",
    "tenLop": "Ấu 1A",
    "lyDo": "Ốm, phụ huynh xin về sớm",
    "nguoiTao": "Maria Trần Thị B",
    "thoiGianTao": "2026-09-06T08:15:00+07:00",
    "trangThai": "CHO_RA_CONG"
  }
}
```

---

## Báo cáo

| Method | Endpoint | Quyền |
|---|---|---|
| GET | `/bao-cao/danh-sach-lop/{lopId}/excel` | theo lớp |
| GET | `/bao-cao/so-diem-danh?lopId=&thang=` | theo lớp |
| GET | `/bao-cao/bang-diem?lopId=` | theo lớp |
| GET | `/bao-cao/ket-qua-nam-hoc?namHocId=` | ADMIN |
| GET | `/bao-cao/chua-lanh-bi-tich?loai=` | ADMIN |
| GET | `/bao-cao/chuyen-can?namHocId=` | ADMIN |

Tất cả endpoint xuất Excel dùng Apache POI, trả `Content-Type:
application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

---

## Tài liệu API tự sinh
Dùng **springdoc-openapi** → Swagger UI tại `/swagger-ui.html`. Chỉ bật ở môi trường `dev`,
tắt ở `prod`.
