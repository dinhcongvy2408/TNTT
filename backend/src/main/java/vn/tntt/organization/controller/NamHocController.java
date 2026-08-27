package vn.tntt.organization.controller;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tntt.common.response.ApiResponse;
import vn.tntt.organization.dto.NamHocResponse;
import vn.tntt.organization.dto.TaoNamHocRequest;
import vn.tntt.organization.service.NamHocService;

import java.util.UUID;

/**
 * API năm học — docs/04 mục "Tổ chức".
 *
 * <p>Controller ở đây mỏng đúng như CLAUDE.md mục 5 yêu cầu: nhận request,
 * gọi service, bọc kết quả vào {@link ApiResponse}. Không một dòng if nghiệp
 * vụ nào. Nhờ vậy nghiệp vụ test được mà không cần dựng tầng HTTP.
 *
 * <p><b>VỀ PHÂN QUYỀN — ĐỌC KỸ.</b> docs/02 quy định "Tạo/sửa năm học" chỉ
 * ADMIN được làm. Nhưng Sprint 1 (đăng nhập) chưa xong, hiện chưa ai đăng
 * nhập được, nên nếu ghi {@code hasRole('ADMIN')} ngay bây giờ thì mọi
 * request đều bị từ chối 403 và màn hình không dùng được.
 *
 * <p>Vì vậy các endpoint ghi tạm để {@code permitAll()} kèm dấu
 * {@code SPRINT 1} bên dưới. Ta khai báo TƯỜNG MINH chứ không bỏ trống
 * annotation — bỏ trống thì không ai biết endpoint này đã được cân nhắc hay
 * bị quên. Tìm lại bằng: {@code grep -rn "SPRINT 1: doi quyen" backend/}
 */
@RestController
@RequestMapping("/api/v1/nam-hoc")
@RequiredArgsConstructor
@Tag(name = "Năm học", description = "Quản lý năm học của xứ đoàn")
public class NamHocController {

    private final NamHocService namHocService;

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Danh sách năm học, mới nhất trước")
    public ApiResponse<java.util.List<NamHocResponse>> danhSach() {
        return ApiResponse.ok(namHocService.layTatCa());
    }

    /**
     * Năm học đang hoạt động.
     *
     * <p>Trả {@code data: null} kèm {@code success: true} khi chưa kích hoạt
     * năm nào — KHÔNG trả 404. Lý do: 404 nghĩa là "đường dẫn hoặc bản ghi
     * bạn hỏi không tồn tại", còn ở đây câu trả lời hợp lệ chỉ đơn giản là
     * "chưa có". Frontend phân biệt được hai chuyện đó.
     */
    @GetMapping("/hien-tai")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Năm học đang hoạt động, null nếu chưa có")
    public ApiResponse<NamHocResponse> hienTai() {
        return namHocService.layNamHocHienTai()
                .map(ApiResponse::ok)
                .orElseGet(() -> new ApiResponse<>(
                        true, null, "Chưa có năm học nào đang hoạt động", null, null));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // SPRINT 1: doi quyen -> @PreAuthorize("hasRole('ADMIN')")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Tạo năm học mới, trạng thái CHUAN_BI")
    public ApiResponse<NamHocResponse> tao(@Valid @RequestBody TaoNamHocRequest request) {
        return ApiResponse.ok(namHocService.taoMoi(request), "Đã tạo năm học");
    }

    /**
     * Đưa năm học vào vận hành.
     *
     * <p>Endpoint này KHÔNG có trong docs/04 — xem docs/99 mục F1 để biết vì
     * sao phải thêm.
     */
    @PatchMapping("/{id}/kich-hoat")
    // SPRINT 1: doi quyen -> @PreAuthorize("hasRole('ADMIN')")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Kích hoạt năm học: CHUAN_BI sang DANG_HOAT_DONG")
    public ApiResponse<NamHocResponse> kichHoat(@PathVariable UUID id) {
        return ApiResponse.ok(namHocService.kichHoat(id), "Đã kích hoạt năm học");
    }

    @PatchMapping("/{id}/ket-thuc")
    // SPRINT 1: doi quyen -> @PreAuthorize("hasRole('ADMIN')")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Kết thúc năm học, dữ liệu thành chỉ đọc")
    public ApiResponse<NamHocResponse> ketThuc(@PathVariable UUID id) {
        return ApiResponse.ok(namHocService.ketThuc(id), "Đã kết thúc năm học");
    }
}
