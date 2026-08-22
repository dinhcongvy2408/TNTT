# CLAUDE.md — Hệ thống Quản lý Xứ đoàn Thiếu Nhi Thánh Thể

> File này là bộ nhớ dài hạn của dự án. Claude Code đọc file này ở mỗi phiên làm việc.
> Khi có quyết định kiến trúc mới, cập nhật vào đây.

## 1. Bối cảnh

Tôi (Đinh Công Vỹ) xây dựng một web application quản lý Xứ đoàn Thiếu Nhi Thánh Thể (TNTT)
cho giáo xứ. Dự án phi lợi nhuận, chạy production thật.

- **Quy mô giai đoạn 1**: ~1.000 thiếu nhi, ~150 huynh trưởng/giáo lý viên.
- **Quy mô mở rộng**: 2.000–3.000 hồ sơ, có thể mở cho xứ đoàn khác dùng chung.
- **Người dùng của web**: chỉ huynh trưởng/giáo lý viên và ban điều hành. Thiếu nhi và
  phụ huynh KHÔNG đăng nhập ở giai đoạn 1.
- **Thiết bị chính**: điện thoại (huynh trưởng điểm danh trên điện thoại vào Chủ Nhật).
  Mobile-first là bắt buộc, không phải tuỳ chọn.

## 2. Mục tiêu kép — quan trọng khi bạn hỗ trợ tôi

Đây là dự án **vừa học vừa làm**. Tôi đang học Full-stack. Vì vậy:

- **KHÔNG** viết trọn một feature lớn rồi đưa tôi copy-paste.
- Làm từng lớp nhỏ, giải thích *tại sao* chọn cách đó trước khi viết code.
- Khi có nhiều cách làm, nêu 2 phương án + trade-off, để tôi chọn.
- Sau mỗi module, gợi ý cho tôi 1–2 câu hỏi tự kiểm tra xem tôi đã hiểu chưa.
- Nếu tôi viết code sai pattern, hãy nói thẳng và chỉ ra chỗ sai, đừng im lặng sửa hộ.
- Ưu tiên code dễ đọc hơn code "thông minh".

## 3. Tech stack đã chốt

| Tầng | Công nghệ | Ghi chú |
|---|---|---|
| Frontend | ReactJS (Vite) + TypeScript + Tailwind CSS | SPA, mobile-first |
| Hosting FE | Vercel hoặc Cloudflare Pages | miễn phí, tự động build từ GitHub |
| Backend | Java 21 + Spring Boot 3.x | RESTful API |
| Bảo mật | Spring Security + JWT (access + refresh token) | RBAC |
| ORM | Spring Data JPA (Hibernate) | |
| Migration | Flyway | KHÔNG dùng `ddl-auto: update` ở production |
| Database | PostgreSQL 16 | |
| Real-time | Spring WebSocket (STOMP/SockJS) | cho module trực cổng |
| Đóng gói | Docker + Docker Compose | |
| Reverse proxy | Nginx + Let's Encrypt | trên VPS |
| Hạ tầng | VPS Vietnix (Ubuntu), đặt tại Việt Nam | dữ liệu tự quản lý |
| CI/CD | GitHub Actions | build .jar → deploy VPS |

**Đã cân nhắc và loại bỏ**: MySQL (chọn PostgreSQL), Thymeleaf server-side rendering
(chọn tách FE/BE), Next.js SSR (chưa cần, SPA đủ dùng), microservices thật sự
(over-engineering ở quy mô này — dùng **monolith module hoá**).

## 4. Cấu trúc repo

```
tntt/
├── backend/                 # Spring Boot
│   └── src/main/java/vn/tntt/
│       ├── common/          # exception, response wrapper, util, config
│       ├── security/        # JWT, filter, RBAC
│       ├── organization/    # NamHoc, Nganh, LopHoc
│       ├── personnel/       # NguoiDung, VaiTro, PhanCong
│       ├── student/         # ThieuNhi, BiTich
│       ├── enrollment/      # GhiDanh (Lop_ThieuNhi)
│       ├── attendance/      # DiemDanh
│       ├── grading/         # DiemSo, xét chuyển cấp
│       └── discipline/      # ToTruc, LichTruc, PhieuRaCong, WebSocket
├── frontend/                # React + Vite + TS
├── docs/                    # tài liệu nghiệp vụ (đọc trước khi code)
├── docker-compose.yml
└── CLAUDE.md
```

Mỗi module backend theo cấu trúc: `controller/ service/ repository/ entity/ dto/ mapper/`.

Mỗi package module có `package-info.java` ghi rõ nó thuộc sprint nào và phụ trách
bảng nào — đọc file đó trước khi thêm code vào module.

## 5. Quy ước code

**Backend**
- Package gốc: `vn.tntt`
- Tên **bảng và cột DB**: tiếng Việt không dấu, `snake_case` (`thieu_nhi`, `ngay_rua_toi`)
  → để ban điều hành đọc DB cũng hiểu.
- Tên **class/biến Java**: tiếng Anh, `PascalCase`/`camelCase` (`Student`, `baptismDate`)
  → chuẩn nghề nghiệp, dễ đưa vào CV. Ánh xạ qua `@Column(name = "...")`.
- Controller KHÔNG chứa business logic. Controller → Service → Repository.
- Không bao giờ trả Entity ra API. Luôn qua DTO.
- Response chuẩn: `{ "success": bool, "data": ..., "message": string }`.
- Exception xử lý tập trung qua `@RestControllerAdvice`.
- Mọi endpoint đều phải khai báo quyền rõ ràng bằng `@PreAuthorize`.

**Frontend**
- TypeScript strict mode.
- Gọi API qua một lớp `services/` duy nhất, không `fetch` rải rác trong component.
- State server dùng TanStack Query; state UI dùng `useState`/`useContext`.
- Component đặt trong `features/<tên-module>/`.

**Chung**
- Commit theo Conventional Commits: `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`.
- Mỗi feature một nhánh, merge vào `main` qua PR.

## 6. Nguyên tắc bảo mật — dữ liệu trẻ em

Đây là hồ sơ cá nhân của người dưới 18 tuổi. Bắt buộc:

- Mật khẩu hash bằng BCrypt, không bao giờ log ra.
- Không log số điện thoại phụ huynh, ngày sinh vào application log.
- Mọi thao tác thêm/sửa/xoá hồ sơ thiếu nhi phải ghi vào bảng `nhat_ky_he_thong` (audit log).
- Phân quyền kiểm tra ở tầng service, không chỉ ở tầng UI.
- Backup DB tự động hằng ngày, giữ 7 bản gần nhất.
- Soft delete cho hồ sơ thiếu nhi (`da_xoa boolean`), không xoá cứng.

## 7. Tài liệu tham chiếu

Trước khi implement một module, đọc file tương ứng trong `docs/`:

- `docs/01-tong-quan.md` — vấn đề, mục tiêu, phạm vi
- `docs/02-nghiep-vu.md` — quy trình nghiệp vụ chi tiết + ma trận phân quyền
- `docs/03-du-lieu.md` — ERD và mô tả bảng
- `docs/04-api.md` — đặc tả API
- `docs/05-lo-trinh.md` — roadmap chia sprint
- `schema.sql` — DDL PostgreSQL (bản **tham chiếu**; bản chạy thật là
  `backend/src/main/resources/db/migration/`)
- `docs/99-sai-lech-tai-lieu.md` — **đọc file này khi thấy code khác tài liệu**.
  `schema.sql` gốc có 2 câu PostgreSQL từ chối thi hành, đã sửa trong migration.

## 8. Trạng thái hiện tại

> Cập nhật mỗi khi hoàn thành một sprint.

- [x] **Sprint 0 — HOÀN TẤT** (23/08/2026): repo, Docker Compose (Postgres 16 ở cổng
      **5433**), Flyway V1–V3, `ApiResponse` + `GlobalExceptionHandler`, Swagger,
      React + Vite + Tailwind v4 + TanStack Query, `GET /api/v1/health` chạy thông
      cả chuỗi React → Vite proxy → Spring → PostgreSQL.
- [ ] Sprint 1 — Auth & phân quyền
- [ ] Sprint 2 — Tổ chức (năm học, ngành, lớp)
- [ ] Sprint 3 — Nhân sự & phân công
- [ ] Sprint 4 — Hồ sơ thiếu nhi + bí tích + import Excel
- [ ] Sprint 5 — Ghi danh & điểm danh
- [ ] Sprint 6 — Điểm số & chuyển cấp
- [ ] Sprint 7 — Ban kỷ luật & phiếu ra cổng (WebSocket)
- [ ] Sprint 8 — Deploy production
