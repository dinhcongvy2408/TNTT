package vn.tntt.organization.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.tntt.common.response.ApiResponse;
import vn.tntt.organization.dto.NganhResponse;
import vn.tntt.organization.dto.TaoNganhRequest;
import vn.tntt.organization.service.NganhService;

import java.util.List;

/** API ngành — docs/04 mục "Tổ chức". */
@RestController
@RequestMapping("/api/v1/nganh")
@RequiredArgsConstructor
@Tag(name = "Ngành", description = "Ngành của xứ đoàn, dữ liệu gốc")
public class NganhController {

    private final NganhService nganhService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Danh sách ngành, sắp theo thứ tự chuyển cấp")
    public ApiResponse<List<NganhResponse>> danhSach() {
        return ApiResponse.ok(nganhService.layTatCa());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo ngành mới, hiếm dùng vì 5 ngành chuẩn đã có sẵn")
    public ApiResponse<NganhResponse> tao(@Valid @RequestBody TaoNganhRequest request) {
        return ApiResponse.ok(nganhService.taoMoi(request), "Đã tạo ngành");
    }
}
