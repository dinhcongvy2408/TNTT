package vn.tntt.discipline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * @param thieuNhiId em nào được về
 * @param lyDo       bắt buộc — người trực cổng và phụ huynh đều cần biết vì
 *                   sao em được về sớm, và đây là dấu vết truy trách nhiệm
 *                   nếu sau này có chuyện
 */
public record TaoPhieuRequest(
        @NotNull(message = "Phải chọn thiếu nhi") UUID thieuNhiId,

        @NotBlank(message = "Phải ghi lý do")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        String lyDo
) {
}
