package vn.tntt.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 422 — request đúng cú pháp nhưng vi phạm quy tắc nghiệp vụ.
 *
 * <p>Phân biệt với 400: 400 là "dữ liệu gửi lên sai định dạng" (thiếu field,
 * ngày sai kiểu). 422 là "định dạng đúng nhưng không được phép về nghiệp vụ",
 * VD: điểm danh cho ngày cách đây 30 ngày, hoặc mở năm học thứ hai khi đã có
 * một năm học đang hoạt động.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        this(message, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessRuleException(String message, String errorCode) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, errorCode, message);
    }
}
