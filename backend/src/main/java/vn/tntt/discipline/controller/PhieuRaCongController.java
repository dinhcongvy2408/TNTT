package vn.tntt.discipline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import vn.tntt.discipline.dto.PhieuRaCongResponse;
import vn.tntt.discipline.dto.TaoPhieuRequest;
import vn.tntt.discipline.service.PhieuRaCongService;
import vn.tntt.security.service.NguoiDungDangDangNhap;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API phiếu ra cổng — docs/04 mục "Ban Kỷ luật và Trực cổng".
 *
 * <p>Quyền lấy đúng từ ma trận ở docs/02:
 * <ul>
 *   <li>Tạo phiếu — HUYNH_TRUONG, KHOI_TRUONG, ADMIN</li>
 *   <li>Xem màn hình trực và xác nhận — KY_LUAT, ADMIN</li>
 *   <li>Huỷ — người tạo hoặc ADMIN (kiểm theo dữ liệu, ở tầng service)</li>
 * </ul>
 *
 * <p><b>Chưa làm:</b> docs/04 ghi quyền xác nhận là "KY_LUAT (đang trực ca)".
 * Phần trong ngoặc cần đối chiếu {@code lich_truc} của hôm nay với tổ mà người
 * đó thuộc về. Hiện mọi tài khoản KY_LUAT đều xác nhận được. docs/99 mục H4.
 */
@RestController
@RequestMapping("/api/v1/phieu-ra-cong")
@RequiredArgsConstructor
@Tag(name = "Phiếu ra cổng", description = "Xin cho thiếu nhi về sớm và trực cổng")
public class PhieuRaCongController {

    private final PhieuRaCongService phieuRaCongService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HUYNH_TRUONG', 'KHOI_TRUONG', 'ADMIN')")
    @Operation(summary = "Giáo lý viên tạo phiếu xin cho em về sớm")
    public ApiResponse<PhieuRaCongResponse> tao(
            @Valid @RequestBody TaoPhieuRequest request,
            @AuthenticationPrincipal NguoiDungDangDangNhap nguoiDung) {
        return ApiResponse.ok(
                phieuRaCongService.taoPhieu(request, nguoiDung.id()),
                "Đã gửi phiếu tới cổng");
    }

    /**
     * Danh sách phiếu đang chờ — màn hình trực cổng gọi lúc mở, và gọi lại mỗi
     * 10 giây khi WebSocket đứt (fallback polling).
     */
    @GetMapping("/dang-cho")
    @PreAuthorize("hasAnyRole('KY_LUAT', 'ADMIN')")
    @Operation(summary = "Phiếu đang chờ ra cổng của năm học hiện tại")
    public ApiResponse<List<PhieuRaCongResponse>> dangCho() {
        return ApiResponse.ok(phieuRaCongService.dangCho());
    }

    @PatchMapping("/{id}/xac-nhan")
    @PreAuthorize("hasAnyRole('KY_LUAT', 'ADMIN')")
    @Operation(summary = "Người trực cổng xác nhận em đã ra về")
    public ApiResponse<PhieuRaCongResponse> xacNhan(
            @PathVariable UUID id,
            @AuthenticationPrincipal NguoiDungDangDangNhap nguoiDung) {
        return ApiResponse.ok(phieuRaCongService.xacNhan(id, nguoiDung.id()), "Đã xác nhận");
    }

    /**
     * Huỷ phiếu.
     *
     * <p>{@code @PreAuthorize} chỉ lọc thô "phải là người đã đăng nhập". Quy
     * tắc thật — "người tạo hoặc ADMIN" — phụ thuộc vào BẢN GHI cụ thể, thứ
     * annotation không nhìn thấy, nên nằm ở service.
     */
    @PatchMapping("/{id}/huy")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Huỷ phiếu, chỉ người tạo hoặc quản trị viên")
    public ApiResponse<PhieuRaCongResponse> huy(
            @PathVariable UUID id,
            @AuthenticationPrincipal NguoiDungDangDangNhap nguoiDung) {
        boolean laAdmin = nguoiDung.maVaiTro().contains("ADMIN");
        return ApiResponse.ok(phieuRaCongService.huy(id, nguoiDung.id(), laAdmin), "Đã huỷ phiếu");
    }

    @GetMapping("/lich-su")
    @PreAuthorize("hasAnyRole('KY_LUAT', 'ADMIN')")
    @Operation(summary = "Lịch sử phiếu trong một ngày")
    public ApiResponse<List<PhieuRaCongResponse>> lichSu(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay) {
        return ApiResponse.ok(phieuRaCongService.lichSu(ngay == null ? LocalDate.now() : ngay));
    }
}
