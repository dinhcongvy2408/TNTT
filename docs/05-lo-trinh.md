# 05 — Lộ trình phát triển (vừa học vừa làm)

Mỗi sprint là **1–2 tuần** làm ngoài giờ. Nguyên tắc: **kết thúc mỗi sprint phải có thứ chạy
được và demo được**, không để dồn 3 tháng rồi mới thấy màn hình đầu tiên.

---

## Sprint 0 — Nền móng (tuần 1)
**Mục tiêu**: gọi được `GET /api/v1/health` từ trình duyệt, có DB chạy trong Docker.

- [ ] Khởi tạo repo GitHub, `.gitignore`, `README.md`
- [ ] Spring Boot 3 + Java 21, cấu trúc package theo module
- [ ] `docker-compose.yml`: PostgreSQL 16 + pgAdmin
- [ ] Flyway migration `V1__init.sql`
- [ ] Global exception handler + `ApiResponse<T>` wrapper
- [ ] Swagger UI
- [ ] React + Vite + TypeScript + Tailwind, gọi thử `/health`

**Học được**: cấu trúc Spring Boot, Docker Compose, migration là gì và tại sao không dùng
`ddl-auto: update`.

---

## Sprint 1 — Xác thực và phân quyền (tuần 2–3)
**Mục tiêu**: đăng nhập được, mỗi vai trò thấy menu khác nhau.

- [ ] Entity `NguoiDung`, `VaiTro`, `NguoiDungVaiTro`
- [ ] Spring Security config, BCrypt
- [ ] JWT: access token + refresh token
- [ ] `/auth/login`, `/auth/refresh`, `/auth/me`, `/auth/doi-mat-khau`
- [ ] `@PreAuthorize` trên controller
- [ ] Frontend: trang login, lưu token, `AuthContext`, protected route
- [ ] Seed sẵn 1 tài khoản ADMIN qua migration

**Học được**: filter chain của Spring Security, sự khác nhau giữa authentication và
authorization, tại sao không lưu JWT trong localStorage.

**Tự kiểm tra**: giải thích được điều gì xảy ra từ lúc bấm nút Login đến lúc thấy dashboard?

---

## Sprint 2 — Tổ chức (tuần 4)
**Mục tiêu**: Admin tạo được năm học, ngành, lớp.

- [ ] CRUD `NamHoc`, `Nganh`, `LopHoc`
- [ ] Ràng buộc: chỉ một năm học `DANG_HOAT_DONG`
- [ ] `BaseEntity` + JPA Auditing
- [ ] Frontend: màn hình quản lý năm học và lớp

**Học được**: pattern Controller → Service → Repository, DTO/Mapper (MapStruct), validation
bằng Bean Validation.

---

## Sprint 3 — Nhân sự và phân công (tuần 5)
**Mục tiêu**: tạo được 150 tài khoản huynh trưởng và phân công lớp.

- [ ] CRUD `NguoiDung`, gán vai trò
- [ ] `PhanCong` theo năm học
- [ ] Import huynh trưởng từ Excel (làm nhỏ trước, để tập dượt cho sprint 4)
- [ ] Frontend: danh sách người dùng có tìm kiếm, phân trang, gán vai trò

**Học được**: phân trang với Spring Data, `Specification` để lọc động, Apache POI cơ bản.

---

## Sprint 4 — Hồ sơ thiếu nhi (tuần 6–7) ⭐ sprint nặng nhất
**Mục tiêu**: import 1.000 hồ sơ thật từ file Excel của giáo xứ.

- [ ] Entity `ThieuNhi` (soft delete) + `BiTich` (1-N)
- [ ] Sinh mã thiếu nhi tự động
- [ ] CRUD + tìm kiếm full-text tiếng Việt
- [ ] Import Excel 3 bước: tải mẫu → preview + báo lỗi → xác nhận ghi
- [ ] Audit log `NhatKyHeThong`
- [ ] Frontend: danh sách, chi tiết hồ sơ, timeline bí tích, màn hình import

**Học được**: transaction, batch insert, xử lý file upload, `@Transactional` hoạt động thế nào.

**Cạm bẫy**: đừng insert từng dòng một trong vòng lặp — 1.000 lần round-trip DB sẽ rất chậm.
Dùng `saveAll` với `hibernate.jdbc.batch_size`.

---

## Sprint 5 — Ghi danh và điểm danh (tuần 8–9)
**Mục tiêu**: huynh trưởng điểm danh được trên điện thoại vào Chủ Nhật.

- [ ] `GhiDanh`, xếp lớp hàng loạt
- [ ] `DiemDanh` với API batch upsert
- [ ] Ràng buộc unique chống trùng
- [ ] Kiểm tra quyền theo lớp ở tầng service
- [ ] Frontend **mobile-first**: màn hình điểm danh một chạm, hoạt động khi mạng chậm
- [ ] Thống kê chuyên cần theo lớp

**Học được**: thiết kế API batch, idempotency, tối ưu UX cho mobile.

**Đây là màn hình quan trọng nhất của cả hệ thống** — 150 người dùng nó mỗi tuần. Đầu tư
thời gian vào đây nhiều hơn các màn hình khác.

---

## Sprint 6 — Điểm số và chuyển cấp (tuần 10)
- [ ] `DiemSo` batch, tự tính điểm trung bình
- [ ] Cấu hình ngưỡng đạt (bảng `cau_hinh` key-value)
- [ ] Preview chuyển cấp + thực hiện trong một transaction
- [ ] Báo cáo tổng kết, xuất Excel

**Học được**: xử lý nghiệp vụ phức tạp trong transaction, tại sao cần preview trước khi
thực hiện thao tác hàng loạt không thể hoàn tác.

---

## Sprint 7 — Ban Kỷ luật và trực cổng (tuần 11–12) ⭐ điểm nhấn CV
- [ ] `ToTruc`, `ThanhVienToTruc`, `LichTruc`, tạo lịch luân phiên
- [ ] `PhieuRaCong` CRUD + máy trạng thái
- [ ] Spring WebSocket + STOMP, xác thực bằng JWT
- [ ] Frontend: màn hình tạo phiếu (giáo lý viên) + Live Dashboard (trực cổng)
- [ ] Âm thanh thông báo + fallback polling 10 giây khi socket đứt
- [ ] Job cuối ngày tự huỷ phiếu chưa xác nhận

**Học được**: WebSocket vs HTTP, STOMP, xử lý real-time và fallback khi mạng không ổn định.

---

## Sprint 8 — Đưa lên production (tuần 13)
- [ ] Dockerfile multi-stage cho Spring Boot
- [ ] `docker-compose.prod.yml`: app + postgres + nginx
- [ ] Thuê VPS Vietnix Ubuntu, cấu hình firewall (UFW), fail2ban, tắt SSH password
- [ ] Nginx reverse proxy + Let's Encrypt (certbot)
- [ ] Deploy frontend lên Vercel, trỏ API sang domain backend
- [ ] GitHub Actions: push `main` → build .jar → deploy VPS qua SSH
- [ ] Script backup PostgreSQL hằng ngày (`pg_dump` + cron), giữ 7 bản
- [ ] Cấu hình log rotation

**Học được**: toàn bộ chuỗi CI/CD, bảo mật server cơ bản, quản lý secret bằng GitHub Secrets.

---

## Sau khi lên production

| Việc | Ưu tiên |
|---|---|
| Chạy thử với 1 lớp thật trong 2 tuần trước khi mở rộng toàn xứ | Cao |
| Tập huấn cho huynh trưởng (video 5 phút + tờ hướng dẫn 1 trang) | Cao |
| Trang báo cáo tổng quan cho Cha xứ / Ban điều hành | Trung bình |
| Viết test: unit test cho service, integration test với Testcontainers | Trung bình |
| PWA để cài lên màn hình chính điện thoại | Thấp |
| Multi-tenant cho xứ đoàn khác | Thấp |

---

## Lời khuyên khi vừa học vừa làm

1. **Đừng học hết rồi mới làm.** Học đúng thứ cần cho sprint hiện tại.
2. **Commit nhỏ và thường xuyên.** Lịch sử commit sạch là thứ nhà tuyển dụng nhìn vào.
3. **Viết README tử tế.** Có ảnh chụp màn hình, có sơ đồ kiến trúc, có hướng dẫn chạy local.
4. **Ghi nhật ký học tập.** Mỗi sprint viết một file `docs/hoc-duoc/sprint-N.md` ghi lại
   cái gì khó, sai ở đâu, sửa thế nào. Sau này chính là kho câu chuyện để trả lời phỏng vấn.
5. **Đừng cầu toàn ở sprint đầu.** Code xấu nhưng chạy được, dùng được, rồi refactor sau —
   tốt hơn code đẹp mà 3 tháng chưa demo được gì.
