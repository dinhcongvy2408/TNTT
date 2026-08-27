package vn.tntt.discipline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Sinh lịch trực luân phiên (docs/02 mục 6.1: "Tổ A trực tuần 1, Tổ B tuần 2").
 *
 * @param toTrucIds thứ tự trong danh sách CHÍNH LÀ thứ tự luân phiên
 * @param tuNgay    ngày trực đầu tiên
 * @param denNgay   sinh lịch tới hết ngày này
 * @param caTruc    tên ca, VD "Thánh lễ thiếu nhi 7h30"
 */
public record TaoLuanPhienRequest(
        @NotNull UUID namHocId,
        @NotEmpty(message = "Phải chọn ít nhất một tổ") List<UUID> toTrucIds,
        @NotNull(message = "Thiếu ngày bắt đầu") LocalDate tuNgay,
        @NotNull(message = "Thiếu ngày kết thúc") LocalDate denNgay,
        @NotBlank(message = "Phải nhập tên ca trực") String caTruc
) {
}
