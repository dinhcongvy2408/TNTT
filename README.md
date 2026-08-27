# Hệ thống Quản lý Xứ đoàn Thiếu Nhi Thánh Thể

Web application quản lý và vận hành Xứ đoàn Thiếu Nhi Thánh Thể của giáo xứ:
hồ sơ thiếu nhi, bí tích, điểm danh hằng tuần, điểm số, chuyển cấp, và phiếu
ra cổng real-time cho Ban Kỷ luật.

Dự án phi lợi nhuận, chạy production thật. Quy mô giai đoạn 1: ~1.000 thiếu nhi,
~150 huynh trưởng.

> **Trạng thái: Sprint 0 hoàn tất.** Backend + database + frontend đã chạy thông
> nhau qua `GET /api/v1/health`. Xem [Lộ trình](#8-lộ-trình).

---

## 1. Kiến trúc

```
   Điện thoại huynh trưởng
            │  HTTPS
            ▼
   ┌─────────────────┐        ┌──────────────────────┐       ┌──────────────┐
   │  React + Vite   │  REST  │   Spring Boot 3      │  JDBC │ PostgreSQL16 │
   │  TypeScript     │ ─────▶ │   Java 21            │ ────▶ │              │
   │  Tailwind v4    │  WS    │   Spring Security    │       │   Flyway     │
   │  (Vercel)       │ ◀────▶ │   (VPS Vietnix)      │       │              │
   └─────────────────┘        └──────────────────────┘       └──────────────┘
```

| Tầng | Công nghệ |
|---|---|
| Frontend | React 19 + Vite + TypeScript (strict) + Tailwind v4 + TanStack Query |
| Backend | Java 21 + Spring Boot 3.4 + Spring Data JPA |
| Database | PostgreSQL 16, migration bằng Flyway |
| Real-time | Spring WebSocket / STOMP (Sprint 7) |
| Hạ tầng | Docker Compose, Nginx, VPS Ubuntu |

**Monolith module hoá**, không phải microservices — ở quy mô này microservices
là over-engineering. Mỗi module là một package trong cùng một ứng dụng.

---

## 2. Yêu cầu môi trường

| Công cụ | Bản tối thiểu | Kiểm tra |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | không cần cài riêng — repo có `./mvnw` | `./mvnw -v` |
| Node.js | 20 | `node -v` |
| Docker Desktop | 24 | `docker -v` |

---

## 3. Chạy lần đầu

```bash
# 1. Chuẩn bị biến môi trường
cp .env.example .env          # rồi mở ra sửa mật khẩu

# 2. Bật database
docker compose up -d postgres
docker compose ps             # đợi tới khi cột STATUS ghi (healthy)

# 3. Bật backend  (cửa sổ terminal riêng)
cd backend
./mvnw spring-boot:run           # Windows: mvnw.cmd spring-boot:run

# 4. Bật frontend (cửa sổ terminal riêng)
cd frontend
npm install
npm run dev
```

Mở trình duyệt:

| Địa chỉ | Là gì |
|---|---|
| <http://localhost:5173/kiem-tra> | Giao diện — phải thấy Backend **UP**, Database **UP** |
| <http://localhost:8080/api/v1/health> | API trực tiếp |
| <http://localhost:8080/swagger-ui.html> | Tài liệu API (chỉ có ở profile `dev`) |
| <http://localhost:5050> | pgAdmin, chạy `docker compose up -d pgadmin` trước |

**Tài khoản mặc định**: `admin@xudoan.local` / `Admin@123`

Hệ thống bắt đổi mật khẩu ngay lần đăng nhập đầu, và việc bắt buộc đó ép ở
**backend** chứ không chỉ chuyển hướng ở giao diện — gọi thẳng API cũng không
lách được (`docs/99` mục G3).

> Hash mật khẩu này nằm công khai trong repo (migration V4), coi như đã lộ.
> Trước khi deploy production phải viết một migration mới đặt hash thật.

---

## 4. Một điểm dễ vấp: file `.env` và Spring Boot

`docker compose` **có** đọc `.env`. Spring Boot thì **không**.

Khi bạn chạy `./mvnw spring-boot:run`, backend lấy giá trị từ phần
`${BIEN:giá_trị_mặc_định}` trong `application.yml`. Các giá trị mặc định ở đó
đã để khớp sẵn với `.env.example`, nên chạy local là thông.

Nếu bạn đổi mật khẩu trong `.env`, phải đổi **cả hai chỗ**, hoặc truyền biến
môi trường vào cho Maven:

```bash
POSTGRES_PASSWORD=mat_khau_moi ./mvnw spring-boot:run
```

Trên production thì không có vấn đề này — biến môi trường do Docker Compose
truyền thẳng vào container backend.

> Cổng PostgreSQL mặc định của dự án là **5433**, không phải 5432, để tránh đụng
> với PostgreSQL bạn có thể đã cài sẵn trên máy.

---

## 5. Cấu trúc thư mục

```
tntt/
├── backend/
│   └── src/main/
│       ├── java/vn/tntt/
│       │   ├── common/          # Sprint 0 — ApiResponse, exception, config, BaseEntity
│       │   ├── security/        # Sprint 1 — JWT, filter chain, RBAC
│       │   ├── organization/    # Sprint 2 — NamHoc, Nganh, LopHoc
│       │   ├── personnel/       # Sprint 3 — NguoiDung, VaiTro, PhanCong
│       │   ├── student/         # Sprint 4 — ThieuNhi, BiTich
│       │   ├── enrollment/      # Sprint 5 — GhiDanh
│       │   ├── attendance/      # Sprint 5 — DiemDanh
│       │   ├── grading/         # Sprint 6 — DiemSo, chuyển cấp
│       │   └── discipline/      # Sprint 7 — ToTruc, PhieuRaCong, WebSocket
│       └── resources/
│           ├── application.yml          # cấu hình chung
│           ├── application-dev.yml      # dev: Swagger bật, log chi tiết
│           ├── application-prod.yml     # prod: Swagger tắt, không log SQL
│           └── db/migration/            # Flyway
├── frontend/
│   └── src/
│       ├── services/            # LỚP DUY NHẤT gọi API
│       ├── features/<module>/   # mỗi màn hình một thư mục
│       ├── components/          # component dùng lại
│       └── lib/                 # cấu hình dùng chung
├── docs/                        # tài liệu nghiệp vụ — ĐỌC TRƯỚC KHI CODE
├── schema.sql                   # DDL tham chiếu (bản chạy thật ở db/migration)
├── docker-compose.yml
└── CLAUDE.md                    # bộ nhớ dài hạn của dự án
```

Mỗi module backend theo cấu trúc:
`controller/ service/ repository/ entity/ dto/ mapper/`

---

## 6. Quy ước bắt buộc

**Đặt tên**
- Bảng và cột DB: tiếng Việt không dấu, `snake_case` — `thieu_nhi`, `ngay_rua_toi`.
  Để ban điều hành mở DB ra cũng đọc hiểu.
- Class và biến Java: tiếng Anh, `PascalCase`/`camelCase` — ánh xạ qua
  `@Column(name = "...")`.

**Backend**
- Controller → Service → Repository. Controller không chứa business logic.
- Không bao giờ trả Entity ra API, luôn qua DTO.
- Mọi endpoint khai báo quyền bằng `@PreAuthorize`, **và** kiểm tra lại phạm vi
  dữ liệu ở tầng service.
- Exception xử lý tập trung ở `GlobalExceptionHandler`.

**Frontend**
- TypeScript strict mode.
- Gọi API chỉ qua `src/services/`. Component không import `axios`.
- State server dùng TanStack Query, state UI dùng `useState`/`useContext`.

**Git** — Conventional Commits: `feat:` `fix:` `refactor:` `docs:` `chore:`.
Mỗi feature một nhánh, merge vào `main` qua PR.

---

## 7. Bảo mật — đây là hồ sơ trẻ em

Không phải hình thức. Đây là dữ liệu cá nhân của người dưới 18 tuổi.

- Mật khẩu hash BCrypt, không bao giờ log.
- **Không log** số điện thoại phụ huynh và ngày sinh. Vì vậy `application-prod.yml`
  ép `org.hibernate.SQL: OFF`, và `GlobalExceptionHandler` không trả message của
  lỗi hệ thống ra client.
- Mọi thao tác thêm/sửa/xoá hồ sơ thiếu nhi ghi vào `nhat_ky_he_thong` (audit log).
- Hồ sơ thiếu nhi **soft delete** (`da_xoa`), không xoá cứng — có thể cần tra
  cứu bí tích nhiều năm sau.
- Swagger UI **tắt** ở production.
- Backup DB hằng ngày, giữ 7 bản gần nhất (Sprint 8).

---

## 8. Lộ trình

| Sprint | Nội dung | Trạng thái |
|:--:|---|:--:|
| 0 | Nền móng: Docker, Flyway, health check, ApiResponse | ✅ |
| 1 | Auth & phân quyền (JWT, RBAC) | ⬜ |
| 2 | Tổ chức: năm học, ngành, lớp | ⬜ |
| 3 | Nhân sự & phân công | ⬜ |
| 4 | Hồ sơ thiếu nhi + bí tích + import Excel | ⬜ |
| 5 | Ghi danh & điểm danh (màn hình quan trọng nhất) | ⬜ |
| 6 | Điểm số & chuyển cấp | ⬜ |
| 7 | Ban Kỷ luật & phiếu ra cổng (WebSocket) | ⬜ |
| 8 | Deploy production | ⬜ |

Chi tiết từng sprint: [docs/05-lo-trinh.md](docs/05-lo-trinh.md)

---

## 9. Tài liệu

Đọc file tương ứng **trước khi** implement một module:

| File | Nội dung |
|---|---|
| [docs/01-tong-quan.md](docs/01-tong-quan.md) | Vấn đề, mục tiêu, phạm vi, vai trò người dùng |
| [docs/02-nghiep-vu.md](docs/02-nghiep-vu.md) | Quy trình nghiệp vụ + ma trận phân quyền |
| [docs/03-du-lieu.md](docs/03-du-lieu.md) | ERD và lý do đằng sau từng quyết định thiết kế |
| [docs/04-api.md](docs/04-api.md) | Đặc tả API |
| [docs/05-lo-trinh.md](docs/05-lo-trinh.md) | Roadmap chia sprint |
| [docs/99-sai-lech-tai-lieu.md](docs/99-sai-lech-tai-lieu.md) | Chỗ code khác tài liệu và vì sao |

---

## 10. Lệnh hay dùng

```bash
# Backend
cd backend
./mvnw spring-boot:run                 # chạy dev
./mvnw test                            # chạy test
./mvnw clean package                   # đóng gói .jar
#   Dùng ./mvnw (mvnw.cmd trên Windows) chứ không phải mvn: wrapper ghim
#   đúng phiên bản Maven trong repo, nên máy bạn và CI chạy y hệt nhau.

# Frontend
cd frontend
npm run dev                            # dev server, có hot reload
npm run build                          # build production (tsc + vite)
npm run lint

# Database
docker compose up -d postgres          # bật
docker compose logs -f postgres        # xem log
docker compose down                    # tắt, GIỮ dữ liệu
docker compose down -v                 # tắt và XOÁ SẠCH dữ liệu

# Vào thẳng psql
docker exec -it tntt-postgres psql -U tntt -d tntt

# Xem Flyway đã chạy migration nào
docker exec tntt-postgres psql -U tntt -d tntt \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

> Trên Windows, terminal có thể hiện tiếng Việt bị vỡ ở log Maven. Đó chỉ là
> codepage của cửa sổ console, không phải file bị sai encoding. Chạy
> `chcp 65001` trước nếu muốn đọc cho rõ.
