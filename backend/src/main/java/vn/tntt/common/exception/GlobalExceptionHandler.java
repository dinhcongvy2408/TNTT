package vn.tntt.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import vn.tntt.common.response.ApiResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bắt mọi exception rơi ra khỏi controller và biến thành {@link ApiResponse}
 * đúng định dạng ở docs/04-api.md.
 *
 * <p>Nhờ lớp này, code trong service chỉ việc {@code throw} rồi quên đi —
 * không controller nào phải viết try/catch. Đó là lý do CLAUDE.md yêu cầu
 * "exception xử lý tập trung qua @RestControllerAdvice".
 *
 * <p><b>Bảo mật:</b> đây là dữ liệu trẻ em. Ta KHÔNG bao giờ đưa message của
 * lỗi hệ thống ra ngoài — nó có thể chứa câu SQL kèm số điện thoại phụ huynh
 * hay ngày sinh. Lỗi 500 chỉ trả một câu chung chung, chi tiết ghi vào log.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------------------------------------------
    // 1. Exception nghiệp vụ do chính ta ném ra
    // ---------------------------------------------------------------

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        log.warn("Lỗi nghiệp vụ [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    // ---------------------------------------------------------------
    // 2. Lỗi validate của Bean Validation (@Valid trên @RequestBody)
    // ---------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // LinkedHashMap để giữ nguyên thứ tự field, frontend hiển thị ổn định
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            // putIfAbsent: một field có thể vi phạm nhiều rule, chỉ lấy lỗi đầu tiên
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Dữ liệu gửi lên không hợp lệ", "VALIDATION_ERROR", fieldErrors));
    }

    /** JSON gửi lên sai cú pháp, hoặc sai kiểu (VD: ngày "31/02/2026"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Không đọc được request body: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Nội dung gửi lên không đọc được. Kiểm tra lại định dạng JSON và kiểu dữ liệu.",
                "MALFORMED_REQUEST"));
    }

    /** Thiếu query param bắt buộc, VD: gọi /diem-danh mà không truyền lopId. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Thiếu tham số bắt buộc: " + ex.getParameterName(), "MISSING_PARAMETER"));
    }

    /** Sai kiểu tham số trên URL, VD: /lop/abc trong khi id là UUID. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(
                "Tham số '%s' sai định dạng".formatted(ex.getName()), "INVALID_PARAMETER"));
    }

    // ---------------------------------------------------------------
    // 3. Lỗi ràng buộc từ tầng DB
    // ---------------------------------------------------------------

    /**
     * Vi phạm UNIQUE / FOREIGN KEY / CHECK ở PostgreSQL.
     *
     * <p>Ta VẪN cần handler này dù đã kiểm tra trùng ở service, vì hai huynh
     * trưởng bấm Lưu cùng lúc thì kiểm tra ở service (đọc rồi mới ghi) không
     * chặn được — chỉ ràng buộc UNIQUE của DB mới là chốt chặn cuối cùng.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage();
        // Log kèm chi tiết để ta debug, nhưng KHÔNG trả nguyên văn ra ngoài
        log.warn("Vi phạm ràng buộc DB: {}", rootMessage);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(dienGiaiRangBuoc(rootMessage), "DATA_INTEGRITY_VIOLATION"));
    }

    /**
     * Dịch tên ràng buộc trong schema sang câu tiếng Việt cho người dùng cuối.
     * Mỗi khi thêm UNIQUE mới trong migration, nhớ thêm một nhánh ở đây.
     */
    private String dienGiaiRangBuoc(String rootMessage) {
        if (rootMessage == null) {
            return "Dữ liệu vi phạm ràng buộc của hệ thống";
        }
        return switch (timTenRangBuoc(rootMessage)) {
            case "uq_nam_hoc_dang_hoat_dong" ->
                    "Đã có một năm học đang hoạt động. Hãy kết thúc năm học cũ trước.";
            case "uq_lop_ten_nam"       -> "Tên lớp này đã tồn tại trong năm học đã chọn";
            case "uq_diem_danh_ngay"    -> "Buổi điểm danh này đã được lưu trước đó";
            case "uq_bi_tich_moi_loai"  -> "Thiếu nhi này đã có bản ghi cho bí tích đó";
            case "uq_ghi_danh"          -> "Thiếu nhi này đã được ghi danh vào lớp đó";
            case "uq_ghi_danh_dang_hoc" -> "Thiếu nhi này đã có một lớp đang học trong năm học này";
            case "uq_phieu_dang_cho"    -> "Thiếu nhi này đang có một phiếu ra cổng chờ xác nhận";
            default                     -> "Dữ liệu bị trùng hoặc vi phạm ràng buộc của hệ thống";
        };
    }

    /** Rút tên constraint từ câu lỗi của PostgreSQL: ... constraint "uq_xyz". */
    private String timTenRangBuoc(String rootMessage) {
        String moc = "constraint " + '"';
        int start = rootMessage.indexOf(moc);
        if (start < 0) {
            return "";
        }
        start += moc.length();
        int end = rootMessage.indexOf('"', start);
        return end < 0 ? "" : rootMessage.substring(start, end);
    }

    // ---------------------------------------------------------------
    // 4. Không tìm thấy đường dẫn
    // ---------------------------------------------------------------

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Không tìm thấy đường dẫn yêu cầu", "ENDPOINT_NOT_FOUND"));
    }
    // ---------------------------------------------------------------
    // 4b. Lỗi phân quyền của Spring Security
    // ---------------------------------------------------------------

    /**
     * 403 — đã xác thực nhưng không đủ quyền, do {@code @PreAuthorize} chặn.
     *
     * <p><b>Vì sao handler này bắt buộc phải có.</b> {@code @PreAuthorize}
     * kiểm quyền ở lúc GỌI METHOD, tức là BÊN TRONG DispatcherServlet. Lúc
     * đó {@code ExceptionTranslationFilter} của Spring Security — thứ bình
     * thường biến {@code AccessDeniedException} thành 403 — đã chạy xong từ
     * trước và nằm ngoài, nên nó không bắt được gì cả.
     *
     * <p>Exception vì thế rơi thẳng vào {@code @RestControllerAdvice} này.
     * Không có handler riêng thì lưới {@code Exception.class} ở mục 5 vợt
     * mất, và "bạn không có quyền" biến thành 500 "Hệ thống gặp sự cố":
     * frontend không phân biệt được thiếu quyền với server sập, còn log thì
     * đầy ERROR kèm stacktrace cho một tình huống hoàn toàn bình thường.
     *
     * <p><b>Phạm vi.</b> {@code AuthorizationDeniedException} của Spring
     * Security 6 là lớp con của {@code AccessDeniedException}, nên một
     * handler đủ cho cả hai. Còn {@code AuthenticationException} (chưa đăng
     * nhập, token hỏng) KHÔNG tới được đây — nó bị chặn ở tầng filter và sẽ
     * do {@code AuthenticationEntryPoint} xử lý ở Sprint 1.
     *
     * <p>Ta KHÔNG đưa {@code ex.getMessage()} ra ngoài: câu đó nêu đích danh
     * biểu thức quyền đã thất bại, tức là tiết lộ cấu trúc phân quyền cho
     * người vừa bị từ chối.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Từ chối truy cập: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        "Bạn không có quyền thực hiện thao tác này", "ACCESS_DENIED"));
    }

    // ---------------------------------------------------------------
    // 5. Lưới an toàn cuối cùng
    // ---------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Log FULL stacktrace để điều tra; chỉ ghi method + URI, KHÔNG ghi body
        // (body có thể chứa số điện thoại phụ huynh / ngày sinh của trẻ).
        log.error("Lỗi không lường trước tại {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Hệ thống gặp sự cố. Vui lòng thử lại hoặc báo quản trị viên.",
                        "INTERNAL_ERROR"));
    }
}
