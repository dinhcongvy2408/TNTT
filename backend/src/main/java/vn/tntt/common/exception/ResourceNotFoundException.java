package vn.tntt.common.exception;

import org.springframework.http.HttpStatus;

/** 404 — không tìm thấy bản ghi. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    /**
     * Tiện dụng: {@code new ResourceNotFoundException("Lớp học", id)}
     * → "Không tìm thấy Lớp học với id 3f2a...".
     */
    public static ResourceNotFoundException of(String tenDoiTuong, Object id) {
        return new ResourceNotFoundException(
                "Không tìm thấy %s với id %s".formatted(tenDoiTuong, id));
    }
}
