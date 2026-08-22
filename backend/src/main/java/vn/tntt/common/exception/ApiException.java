package vn.tntt.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Lớp cha của mọi exception nghiệp vụ trong hệ thống.
 *
 * <p>Ý tưởng: exception tự mang theo mã HTTP và {@code errorCode} của nó.
 * Nhờ vậy {@link GlobalExceptionHandler} chỉ cần MỘT handler duy nhất cho
 * cả họ này, thay vì viết một {@code @ExceptionHandler} cho từng loại lỗi.
 *
 * <p>{@code errorCode} là chuỗi ổn định để frontend so sánh và dịch sang
 * thông điệp riêng — KHÔNG được so sánh bằng {@code message} tiếng Việt,
 * vì message có thể đổi câu chữ bất cứ lúc nào.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
