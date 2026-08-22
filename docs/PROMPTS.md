# PROMPTS.md — Bộ prompt dùng với Claude Code

## Cách dùng bộ tài liệu này

```bash
mkdir tntt && cd tntt
git init
# Chép CLAUDE.md, schema.sql, thư mục docs/ vào đây
git add . && git commit -m "docs: khởi tạo tài liệu dự án"

claude
```

Claude Code tự đọc `CLAUDE.md` ở thư mục gốc mỗi phiên. Các file trong `docs/` chỉ được đọc
khi bạn nhắc tới — nên trong prompt hãy chỉ rõ file cần đọc.

**Nguyên tắc quan trọng**: mỗi phiên chat làm **một sprint**. Đừng nhồi cả dự án vào một
phiên — context sẽ đầy và chất lượng giảm. Xong sprint thì `/clear` và mở phiên mới.

---

## Prompt 0 — Mở đầu (chạy một lần duy nhất)

```
Đọc CLAUDE.md, docs/01-tong-quan.md, docs/02-nghiep-vu.md, docs/03-du-lieu.md,
docs/04-api.md, docs/05-lo-trinh.md và schema.sql.

Sau khi đọc xong, ĐỪNG viết code. Thay vào đó hãy:
1. Tóm tắt lại dự án bằng 5 câu để tôi kiểm tra bạn hiểu đúng.
2. Chỉ ra những chỗ tài liệu còn mâu thuẫn, thiếu, hoặc có rủi ro kỹ thuật.
3. Đặt cho tôi tối đa 5 câu hỏi mà bạn cần tôi trả lời trước khi bắt đầu code.
4. Đề xuất thứ tự sprint — nếu bạn thấy lộ trình trong docs/05 chưa hợp lý thì nói thẳng.
```

---

## Prompt Sprint 0 — Khởi tạo dự án

```
Ta bắt đầu Sprint 0 theo docs/05-lo-trinh.md.

Mục tiêu: gọi được GET /api/v1/health từ trình duyệt, PostgreSQL chạy trong Docker,
frontend React hiển thị được trạng thái backend.

Yêu cầu:
- Spring Boot 3.x, Java 21, Maven
- Cấu trúc package theo mục 4 của CLAUDE.md
- docker-compose.yml: postgres:16 + pgadmin, có volume để không mất dữ liệu
- Flyway, dùng schema.sql làm V1__init.sql (giữ nguyên nội dung, chỉ đổi tên file)
- application.yml tách profile dev/prod, secret đọc từ biến môi trường
- ApiResponse<T> wrapper + GlobalExceptionHandler
- springdoc-openapi, chỉ bật ở profile dev
- Frontend: Vite + React + TypeScript + Tailwind, một trang gọi /health

QUAN TRỌNG — tôi đang học, nên hãy làm theo cách này:
- Làm từng bước một, dừng lại sau mỗi bước để tôi xem và hỏi.
- Trước khi tạo file cấu hình nào, giải thích ngắn gọn file đó làm gì.
- Với application.yml, giải thích từng nhóm cấu hình.
- Cuối cùng cho tôi checklist lệnh để tự chạy và kiểm tra.

Bắt đầu từ bước 1: khởi tạo cấu trúc thư mục và pom.xml.
```

---

## Prompt Sprint 1 — Auth & phân quyền

```
Sprint 1: xác thực và phân quyền. Đọc lại mục "Ma trận phân quyền" trong
docs/02-nghiep-vu.md và phần Auth trong docs/04-api.md.

Trước khi viết code, hãy giải thích cho tôi bằng lời (không code):
1. Filter chain của Spring Security xử lý một request như thế nào?
2. Vì sao dùng JWT thay vì session? Nhược điểm của JWT là gì?
3. Vì sao tách access token và refresh token? Vì sao refresh token nên nằm trong
   HttpOnly cookie chứ không phải localStorage?

Sau khi tôi xác nhận đã hiểu, ta mới code theo thứ tự:
Entity → Repository → JwtService → SecurityConfig → AuthService → AuthController → Frontend.

Lưu ý riêng của dự án này: một người có nhiều vai trò (quan hệ N-N), nên
UserDetails phải trả về nhiều GrantedAuthority.
```

---

## Prompt Sprint 4 — Import Excel (phần khó nhất)

```
Sprint 4, phần import Excel cho hồ sơ thiếu nhi. Đọc mục 3.3 trong docs/02-nghiep-vu.md.

Luồng 3 bước: tải file mẫu → upload và preview (chưa ghi DB) → xác nhận ghi thật.

Trước khi code, hãy phân tích cho tôi:
- Preview lưu ở đâu giữa 2 request? (session? Redis? bảng tạm? gửi lại từ client?)
  Nêu 3 phương án với trade-off, đề xuất phương án hợp lý nhất cho quy mô 1.000 dòng.
- Làm sao phát hiện trùng: trùng mã, và trùng "họ tên + ngày sinh"?
- 1.000 lần INSERT trong vòng lặp sẽ chậm thế nào, và batch insert giải quyết ra sao?

Sau khi tôi chọn phương án, ta code. Dùng Apache POI.
Nhớ: toàn bộ bước ghi phải nằm trong một transaction.
```

---

## Prompt Sprint 5 — Điểm danh (màn hình quan trọng nhất)

```
Sprint 5: điểm danh. Đây là màn hình 150 huynh trưởng dùng mỗi Chủ Nhật trên điện thoại,
ở nhà thờ, mạng có thể chậm. Ưu tiên UX hơn mọi thứ khác.

Đọc mục 4.1 trong docs/02-nghiep-vu.md và endpoint /diem-danh/batch trong docs/04-api.md.

Backend:
- API batch upsert, gọi lại cùng ngày là cập nhật chứ không tạo trùng.
- Ràng buộc unique (ghi_danh_id, ngay_diem_danh) đã có ở DB — nhưng hãy giải thích
  cho tôi cách xử lý khi 2 huynh trưởng cùng lớp bấm Lưu cùng lúc.
- Kiểm tra quyền ở tầng service: huynh trưởng chỉ điểm danh lớp mình được phân công
  trong năm học đang hoạt động. Đừng chỉ dựa vào @PreAuthorize.

Frontend:
- Mobile-first, một chạm để đánh dấu, không cần cuộn ngang.
- Hiển thị rõ đã lưu / chưa lưu.
- Nếu mất mạng giữa chừng thì sao? Đề xuất cách xử lý.

Bắt đầu bằng việc thiết kế DTO và luồng service, chưa viết code vội.
```

---

## Prompt Sprint 7 — WebSocket trực cổng

```
Sprint 7: module Ban Kỷ luật và phiếu ra cổng real-time.
Đọc mục 6 trong docs/02-nghiep-vu.md và phần WebSocket trong docs/04-api.md.

Trước khi code, giải thích cho tôi:
1. WebSocket khác HTTP thường ở điểm nào? STOMP nằm ở đâu trong bức tranh đó?
2. Xác thực WebSocket bằng JWT làm thế nào, vì không có header Authorization
   ở mỗi message như HTTP?
3. Nếu người trực cổng mất mạng 30 giây rồi kết nối lại, họ có mất thông báo không?
   Thiết kế thế nào để không mất?

Yêu cầu bắt buộc: PHẢI có fallback polling 10 giây khi socket đứt. Mạng ở nhà thờ
không ổn định, không được phụ thuộc hoàn toàn vào WebSocket.

Sau khi giải thích xong, code theo thứ tự: entity + máy trạng thái phiếu → service →
WebSocket config → controller → frontend màn hình trực cổng.
```

---

## Prompt Sprint 8 — Deploy

```
Sprint 8: đưa lên production trên VPS Vietnix (Ubuntu 22.04, 2GB RAM).
Đọc docs/05-lo-trinh.md mục Sprint 8.

Tôi CHƯA từng deploy bao giờ. Hãy hướng dẫn từng bước như cho người mới hoàn toàn,
và với mỗi lệnh Linux, giải thích lệnh đó làm gì trước khi tôi gõ.

Thứ tự:
1. Dockerfile multi-stage cho Spring Boot (giải thích vì sao multi-stage)
2. docker-compose.prod.yml
3. Bảo mật VPS: tạo user thường, khoá SSH bằng key, UFW, fail2ban
4. Nginx reverse proxy + Let's Encrypt
5. Deploy frontend lên Vercel, cấu hình CORS ở backend
6. GitHub Actions CI/CD, dùng GitHub Secrets cho khoá SSH
7. Script backup pg_dump hằng ngày, giữ 7 bản

Với 2GB RAM, hãy tính giúp tôi nên đặt -Xmx bao nhiêu cho JVM và vì sao.
```

---

## Prompt tiện dụng hằng ngày

**Review code tôi tự viết**
```
Tôi vừa tự viết <đường dẫn file>. Hãy review như một senior review junior:
chỉ ra chỗ sai, chỗ chưa đúng pattern của dự án theo CLAUDE.md, và giải thích tại sao.
ĐỪNG sửa hộ tôi — chỉ chỉ ra và gợi ý hướng, tôi sẽ tự sửa.
```

**Khi bị kẹt**
```
Tôi đang gặp lỗi này: <dán lỗi>
Đừng đưa ngay lời giải. Trước hết hãy hỏi tôi 2-3 câu để thu hẹp nguyên nhân,
rồi hướng dẫn tôi cách tự debug.
```

**Cuối mỗi sprint**
```
Sprint <N> đã xong. Hãy:
1. Cập nhật mục "Trạng thái hiện tại" trong CLAUDE.md.
2. Ghi lại vào docs/hoc-duoc/sprint-<N>.md: những gì tôi đã học, những lỗi đã mắc,
   và 3 câu hỏi phỏng vấn mà sprint này giúp tôi trả lời được.
3. Nếu có quyết định kiến trúc mới phát sinh, bổ sung vào CLAUDE.md.
```

**Cập nhật README cho CV**
```
Viết lại README.md ở gốc repo, hướng tới người đọc là nhà tuyển dụng:
mô tả bài toán thực tế, kiến trúc, tech stack, những thách thức kỹ thuật đã giải quyết,
hướng dẫn chạy local. Chèn chỗ trống để tôi thêm ảnh chụp màn hình.
Viết bằng tiếng Anh — repo này sẽ nằm trong CV.
```

---

## Ba điều cần nhớ

1. **Đừng để Claude viết hộ cả sprint.** Bạn sẽ có phần mềm chạy được nhưng không học
   được gì, và sẽ tắc ngay khi phỏng vấn hỏi "tại sao anh làm thế này".
2. **Mỗi phiên một sprint, xong thì `/clear`.** Context đầy làm chất lượng câu trả lời giảm rõ rệt.
3. **Cập nhật `CLAUDE.md` sau mỗi quyết định.** File đó là bộ nhớ dài hạn của dự án — nếu
   không cập nhật, phiên sau Claude sẽ đề xuất ngược lại với thứ bạn đã chốt.
