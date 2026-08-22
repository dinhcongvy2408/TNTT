package vn.tntt.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 409 — dữ liệu trùng (VD: tên lớp đã tồn tại trong năm học này,
 * email đã có người dùng).
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        this(message, "CONFLICT");
    }

    public ConflictException(String message, String errorCode) {
        super(HttpStatus.CONFLICT, errorCode, message);
    }
}
