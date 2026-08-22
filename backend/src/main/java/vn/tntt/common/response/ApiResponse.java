package vn.tntt.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Vỏ bọc chuẩn cho MỌI response của API, theo docs/04-api.md.
 *
 * <p>Vì sao dùng record thay vì class thường? Record là bất biến (immutable),
 * tự sinh constructor/getter/equals/hashCode. Response chỉ tạo ra rồi trả về,
 * không ai sửa nó giữa chừng — đúng bản chất record.
 *
 * <p>{@code @JsonInclude(NON_NULL)} để field null không lọt vào JSON, tránh
 * response rác kiểu {@code "fieldErrors": null} ở mọi lần gọi thành công.
 *
 * @param <T> kiểu dữ liệu nghiệp vụ trả về
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        Map<String, String> fieldErrors
) {

    /** Thành công, có dữ liệu. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    /** Thành công, có dữ liệu kèm thông điệp cho người dùng. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, null);
    }

    /** Thành công, không có dữ liệu trả về (VD: xoá, đổi trạng thái). */
    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, null, message, null, null);
    }

    /** Thất bại. */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode, null);
    }

    /** Thất bại do validate, kèm lỗi theo từng field. */
    public static <T> ApiResponse<T> error(String message, String errorCode,
                                           Map<String, String> fieldErrors) {
        return new ApiResponse<>(false, null, message, errorCode, fieldErrors);
    }
}
