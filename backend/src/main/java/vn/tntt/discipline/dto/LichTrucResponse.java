package vn.tntt.discipline.dto;

import vn.tntt.discipline.entity.LichTruc;

import java.time.LocalDate;
import java.util.UUID;

public record LichTrucResponse(
        UUID id,
        UUID toTrucId,
        String tenTo,
        LocalDate ngayTruc,
        String caTruc,
        String ghiChu
) {
    public static LichTrucResponse tu(LichTruc l) {
        return new LichTrucResponse(
                l.getId(), l.getToTruc().getId(), l.getToTruc().getTenTo(),
                l.getNgayTruc(), l.getCaTruc(), l.getGhiChu());
    }
}
