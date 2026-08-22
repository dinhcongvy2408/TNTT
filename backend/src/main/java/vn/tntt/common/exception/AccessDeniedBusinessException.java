package vn.tntt.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 403 — đã đăng nhập nhưng không được phép thao tác trên tài nguyên này.
 *
 * <p>Dùng cho kiểm tra quyền ở TẦNG SERVICE, VD: "huynh trưởng này không
 * được phân công lớp Ấu 1A trong năm học đang hoạt động". Loại kiểm tra
 * theo dữ liệu như vậy {@code @PreAuthorize} không làm được, vì nó chỉ
 * biết vai trò chứ không biết bản ghi.
 */
public class AccessDeniedBusinessException extends ApiException {

    public AccessDeniedBusinessException(String message, String errorCode) {
        super(HttpStatus.FORBIDDEN, errorCode, message);
    }
}
