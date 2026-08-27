package vn.tntt.discipline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tntt.common.response.ApiResponse;
import vn.tntt.discipline.dto.LichTrucResponse;
import vn.tntt.discipline.dto.TaoLuanPhienRequest;
import vn.tntt.discipline.dto.ToTrucResponse;
import vn.tntt.discipline.service.ToTrucService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** API tổ trực và lịch trực — docs/04 mục "Ban Kỷ luật và Trực cổng". */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Tổ trực", description = "Tổ trực cổng và lịch trực luân phiên")
public class ToTrucController {

    private final ToTrucService toTrucService;

    public record TaoToRequest(
            @NotBlank(message = "Phải nhập tên tổ") String tenTo,
            String moTa) {
    }

    public record ThemThanhVienRequest(UUID nguoiDungId) {
    }

    // ------------------------- Tổ trực -------------------------

    @GetMapping("/to-truc")
    @PreAuthorize("hasAnyRole('ADMIN', 'KY_LUAT')")
    @Operation(summary = "Danh sách tổ trực kèm thành viên")
    public ApiResponse<List<ToTrucResponse>> danhSachTo() {
        return ApiResponse.ok(toTrucService.danhSachTo());
    }

    @PostMapping("/to-truc")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo tổ trực mới")
    public ApiResponse<ToTrucResponse> taoTo(@Valid @RequestBody TaoToRequest request) {
        return ApiResponse.ok(toTrucService.taoTo(request.tenTo(), request.moTa()), "Đã tạo tổ");
    }

    @PostMapping("/to-truc/{id}/thanh-vien")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thêm huynh trưởng vào tổ")
    public ApiResponse<ToTrucResponse> themThanhVien(
            @PathVariable UUID id, @RequestBody ThemThanhVienRequest request) {
        return ApiResponse.ok(
                toTrucService.themThanhVien(id, request.nguoiDungId()), "Đã thêm thành viên");
    }

    @DeleteMapping("/to-truc/{id}/thanh-vien/{nguoiDungId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gỡ thành viên khỏi tổ")
    public ApiResponse<Void> xoaThanhVien(
            @PathVariable UUID id, @PathVariable UUID nguoiDungId) {
        toTrucService.xoaThanhVien(id, nguoiDungId);
        return ApiResponse.ok("Đã gỡ thành viên");
    }

    // ------------------------- Lịch trực -------------------------

    @GetMapping("/lich-truc")
    @PreAuthorize("hasAnyRole('ADMIN', 'KY_LUAT')")
    @Operation(summary = "Lịch trực trong một khoảng ngày")
    public ApiResponse<List<LichTrucResponse>> lich(
            @RequestParam UUID namHocId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return ApiResponse.ok(toTrucService.lichTrong(namHocId, tuNgay, denNgay));
    }

    @GetMapping("/lich-truc/hom-nay")
    @PreAuthorize("hasAnyRole('ADMIN', 'KY_LUAT')")
    @Operation(summary = "Ai đang trực hôm nay")
    public ApiResponse<List<LichTrucResponse>> homNay() {
        return ApiResponse.ok(toTrucService.lichHomNay());
    }

    @PostMapping("/lich-truc/tao-luan-phien")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sinh lịch trực luân phiên theo tuần, bỏ qua ca đã có")
    public ApiResponse<List<LichTrucResponse>> taoLuanPhien(
            @Valid @RequestBody TaoLuanPhienRequest request) {
        List<LichTrucResponse> daTao = toTrucService.taoLuanPhien(request);
        return ApiResponse.ok(daTao, "Đã sinh %d ca trực".formatted(daTao.size()));
    }
}
