# 01 — Tổng quan dự án

## Tên dự án
Hệ thống Quản lý và Vận hành Xứ đoàn Thiếu Nhi Thánh Thể (TNTT)

## Người thực hiện
Đinh Công Vỹ — vai trò: lập trình viên duy nhất, đồng thời là người học.

## Bối cảnh nghiệp vụ

Xứ đoàn Thiếu Nhi Thánh Thể là tổ chức sinh hoạt và học giáo lý của thiếu nhi trong một
giáo xứ Công giáo. Cơ cấu chia theo độ tuổi thành các **Ngành**:

| Ngành | Độ tuổi |
|---|---|
| Chiên Con | 4 – 6 |
| Ấu Nhi | 7 – 9 |
| Thiếu Nhi | 10 – 12 |
| Nghĩa Sĩ | 13 – 15 |
| Hiệp Sĩ | 16 – 18 |

Mỗi Ngành chia thành nhiều **Lớp** (VD: Ấu 1A, Ấu 1B, Thiếu 2). Mỗi lớp do một hoặc hai
**Huynh trưởng / Giáo lý viên** phụ trách. Sinh hoạt chính diễn ra vào **Chủ Nhật**: dự Thánh
lễ thiếu nhi rồi học giáo lý.

## Vấn đề hiện tại (Pain points)

1. **Sổ giấy và file Excel rời rạc** — mỗi huynh trưởng giữ một file, cuối năm ban điều hành
   phải gộp thủ công, sai sót và thất lạc dữ liệu qua các năm.
2. **Tra cứu bí tích khó** — khi thiếu nhi chuyển xứ hoặc chuẩn bị lãnh nhận bí tích mới,
   phải lục sổ giấy để tìm ngày Rửa Tội, Rước Lễ, Thêm Sức.
3. **Tổng kết cuối năm tốn thời gian** — tính điểm trung bình, xét đạt/không đạt, lập danh
   sách chuyển cấp cho hàng nghìn em đều làm tay.
4. **An ninh cổng thiếu kiểm soát** — thiếu nhi ra về giữa giờ chỉ báo miệng, người trực cổng
   không có cách xác minh, không lưu lại thời điểm ra về.

## Mục tiêu giải pháp

Xây dựng một Web Application chuyên biệt cho mô hình Xứ đoàn, cho phép:

- Quản lý trọn vòng đời học tập của thiếu nhi từ Chiên Con đến Hiệp Sĩ.
- Tự động hoá điểm danh, tính điểm, xét chuyển cấp.
- Lưu trữ chính xác và lâu dài lịch sử bí tích.
- Số hoá quy trình trực cổng và phiếu ra về theo thời gian thực.

## Phạm vi

### Trong phạm vi (giai đoạn 1)
- Quản lý năm học, ngành, lớp
- Quản lý tài khoản huynh trưởng và phân quyền theo vai trò
- Hồ sơ thiếu nhi + lịch sử bí tích + import hàng loạt từ Excel
- Ghi danh thiếu nhi vào lớp theo năm học
- Điểm danh hằng tuần (đi lễ / đi học / có phép)
- Nhập điểm HK1, HK2, tính điểm trung bình
- Xét và thực hiện chuyển cấp cuối năm
- Ban kỷ luật: tổ trực, lịch trực cổng, phiếu ra cổng real-time
- Báo cáo và xuất Excel

### Ngoài phạm vi (giai đoạn 1)
- Tài khoản cho phụ huynh và thiếu nhi
- Ứng dụng mobile native
- Thanh toán học phí / quỹ
- Multi-tenant cho nhiều xứ đoàn (nhưng thiết kế DB phải để đường mở rộng)
- Gửi SMS/Zalo cho phụ huynh

## Các vai trò người dùng

| Vai trò | Mã | Phạm vi |
|---|---|---|
| Quản trị viên / Ban điều hành | `ADMIN` | Toàn xứ đoàn: mọi quyền |
| Trưởng ngành / Khối trưởng | `KHOI_TRUONG` | Chỉ các lớp thuộc ngành mình phụ trách |
| Huynh trưởng / Giáo lý viên | `HUYNH_TRUONG` | Chỉ lớp mình được phân công |
| Thành viên Ban Kỷ luật | `KY_LUAT` | Màn hình trực cổng, xác nhận phiếu ra cổng |

Một người có thể mang **nhiều vai trò** (VD: vừa là Huynh trưởng lớp Ấu 1A vừa thuộc Ban Kỷ
luật) — vì vậy quan hệ người dùng ↔ vai trò là N-N, không phải một cột enum.

## Tiêu chí thành công

- 150 huynh trưởng điểm danh được trên điện thoại trong dưới 2 phút mỗi lớp.
- Import 1.000 hồ sơ từ file Excel trong dưới 30 giây.
- Tra cứu bí tích của một em bất kỳ trong dưới 3 giây.
- Chuyển cấp toàn xứ đoàn cuối năm bằng một thao tác của Admin.
- Chi phí hạ tầng dưới 200.000đ/tháng.
