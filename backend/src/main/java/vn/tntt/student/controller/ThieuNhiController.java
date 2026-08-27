package vn.tntt.student.controller;

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
import vn.tntt.common.response.PageResponse;
import vn.tntt.student.dto.LuuThieuNhiRequest;
import vn.tntt.student.dto.ThieuNhiResponse;
import vn.tntt.student.service.ThieuNhiService;

import java.util.UUID;

/**
 * API hồ sơ thiếu nhi — docs/04 mục "Thiếu nhi".
 *
 * <p><b>Về phân quyền.</b> docs/02 phân biệt ba mức: ADMIN xem toàn xứ,
 * KHOI_TRUONG xem ngành mình, HUYNH_TRUONG xem lớp mình. Việc lọc theo ngành
 * và lớp cần bảng {@code phan_cong} của Sprint 3, chưa có. Hiện mọi tài khoản
 * đã đăng nhập đều ĐỌC được toàn bộ danh sách, còn SỬA và XOÁ thì đã giới hạn
 * đúng theo ma trận (ADMIN và KHOI_TRUONG). Ghi ở docs/99 mục H3.
 */
@RestController
@RequestMapping("/api/v1/thieu-nhi")
@RequiredArgsConstructor
@Tag(name = "Thiếu nhi", description = "Hồ sơ thiếu nhi")
public class ThieuNhiController {

    private final ThieuNhiService thieuNhiService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách hồ sơ, có phân trang và tìm theo tên hoặc mã")
    public ApiResponse<PageResponse<ThieuNhiResponse>> danhSach(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "0") int page,
            // 20 dòng một trang: đủ cho một màn hình điện thoại cuộn vài lần,
            // và đủ nhỏ để tải nhanh trên mạng 3G ở nhà thờ.
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(thieuNhiService.tim(tuKhoa, page, Math.min(size, 100)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Chi tiết một hồ sơ")
    public ApiResponse<ThieuNhiResponse> xem(@PathVariable UUID id) {
        return ApiResponse.ok(thieuNhiService.xem(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'KHOI_TRUONG')")
    @Operation(summary = "Tạo hồ sơ mới, mã do hệ thống sinh")
    public ApiResponse<ThieuNhiResponse> tao(@Valid @RequestBody LuuThieuNhiRequest request) {
        return ApiResponse.ok(thieuNhiService.taoMoi(request), "Đã tạo hồ sơ");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KHOI_TRUONG')")
    @Operation(summary = "Sửa hồ sơ")
    public ApiResponse<ThieuNhiResponse> sua(
            @PathVariable UUID id,
            @Valid @RequestBody LuuThieuNhiRequest request) {
        return ApiResponse.ok(thieuNhiService.capNhat(id, request), "Đã cập nhật hồ sơ");
    }

    /** Xoá MỀM — hồ sơ vẫn còn trong DB, chỉ ẩn khỏi mọi truy vấn nghiệp vụ. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xoá mềm hồ sơ, dữ liệu vẫn giữ lại")
    public ApiResponse<Void> xoa(@PathVariable UUID id) {
        thieuNhiService.xoa(id);
        return ApiResponse.ok("Đã xoá hồ sơ");
    }
}
