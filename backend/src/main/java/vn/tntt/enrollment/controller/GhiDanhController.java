package vn.tntt.enrollment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tntt.common.response.ApiResponse;
import vn.tntt.enrollment.dto.GhiDanhRequest;
import vn.tntt.enrollment.dto.GhiDanhResponse;
import vn.tntt.enrollment.entity.TrangThaiGhiDanh;
import vn.tntt.enrollment.service.GhiDanhService;

import java.util.List;
import java.util.UUID;

/** API ghi danh — docs/04 mục "Ghi danh". */
@RestController
@RequestMapping("/api/v1/ghi-danh")
@RequiredArgsConstructor
@Tag(name = "Ghi danh", description = "Đưa thiếu nhi vào lớp của một năm học")
public class GhiDanhController {

    private final GhiDanhService ghiDanhService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sĩ số một lớp, chỉ các em đang học")
    public ApiResponse<List<GhiDanhResponse>> danhSachLop(@RequestParam UUID lopId) {
        return ApiResponse.ok(ghiDanhService.danhSachLop(lopId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'KHOI_TRUONG')")
    @Operation(summary = "Ghi danh một em vào lớp")
    public ApiResponse<GhiDanhResponse> ghiDanh(@Valid @RequestBody GhiDanhRequest request) {
        return ApiResponse.ok(
                ghiDanhService.ghiDanh(request.thieuNhiId(), request.lopId()),
                "Đã ghi danh");
    }

    @PatchMapping("/{id}/trang-thai")
    @PreAuthorize("hasAnyRole('ADMIN', 'KHOI_TRUONG')")
    @Operation(summary = "Đổi trạng thái: nghỉ học, chuyển xứ, hoàn thành")
    public ApiResponse<GhiDanhResponse> doiTrangThai(
            @PathVariable UUID id,
            @RequestParam TrangThaiGhiDanh trangThai) {
        return ApiResponse.ok(ghiDanhService.doiTrangThai(id, trangThai), "Đã cập nhật");
    }
}
