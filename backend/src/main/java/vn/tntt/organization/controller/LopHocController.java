package vn.tntt.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tntt.common.response.ApiResponse;
import vn.tntt.organization.dto.LopHocResponse;
import vn.tntt.organization.dto.LuuLopHocRequest;
import vn.tntt.organization.service.LopHocService;

import java.util.List;
import java.util.UUID;

/**
 * API lớp học — docs/04 mục "Tổ chức".
 *
 * <p><b>Chưa làm ở đây:</b> {@code GET /lop/cua-toi} (lớp được phân công cho
 * người đang đăng nhập). Endpoint đó cần biết "ai đang gọi", nên phải chờ
 * Sprint 1, và cần bảng {@code phan_cong} của Sprint 3. Ghi ở docs/99 mục F4.
 *
 * <p>docs/04 cũng ghi {@code GET /lop} là "tất cả (lọc theo quyền)" — phần
 * "lọc theo quyền" (huynh trưởng chỉ thấy lớp mình) cũng thuộc Sprint 3.
 */
@RestController
@RequestMapping("/api/v1/lop")
@RequiredArgsConstructor
@Tag(name = "Lớp học", description = "Lớp học trong một năm học")
public class LopHocController {

    private final LopHocService lopHocService;

    /**
     * {@code namHocId} bắt buộc, {@code nganhId} tuỳ chọn.
     *
     * <p>Thiếu {@code namHocId} thì {@code GlobalExceptionHandler} trả 400
     * kèm câu "Thiếu tham số bắt buộc: namHocId" — xem handler
     * {@code MissingServletRequestParameterException}.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách lớp của một năm học")
    public ApiResponse<List<LopHocResponse>> danhSach(
            @RequestParam UUID namHocId,
            @RequestParam(required = false) UUID nganhId) {
        return ApiResponse.ok(lopHocService.layTheoNamHoc(namHocId, nganhId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo lớp học mới")
    public ApiResponse<LopHocResponse> tao(@Valid @RequestBody LuuLopHocRequest request) {
        return ApiResponse.ok(lopHocService.taoMoi(request), "Đã tạo lớp học");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sửa lớp học, không đổi được năm học")
    public ApiResponse<LopHocResponse> sua(
            @PathVariable UUID id,
            @Valid @RequestBody LuuLopHocRequest request) {
        return ApiResponse.ok(lopHocService.capNhat(id, request), "Đã cập nhật lớp học");
    }

    /**
     * Xoá lớp.
     *
     * <p>Trả 200 kèm {@code ApiResponse} chứ không phải 204 No Content: quy
     * ước ở docs/04 là MỌI response đều có vỏ {@code ApiResponse}, còn 204
     * theo chuẩn HTTP thì không được có thân. Nhất quán một kiểu vỏ đáng giá
     * hơn việc dùng đúng mã 204 ở đúng một endpoint.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoá lớp học, chặn nếu lớp đã có thiếu nhi ghi danh")
    public ApiResponse<Void> xoa(@PathVariable UUID id) {
        lopHocService.xoa(id);
        return ApiResponse.ok("Đã xoá lớp học");
    }
}
