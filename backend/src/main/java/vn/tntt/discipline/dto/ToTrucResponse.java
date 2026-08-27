package vn.tntt.discipline.dto;

import vn.tntt.discipline.entity.ToTruc;
import vn.tntt.personnel.entity.NguoiDung;

import java.util.List;
import java.util.UUID;

public record ToTrucResponse(
        UUID id,
        String tenTo,
        String moTa,
        List<ThanhVien> thanhVien
) {
    public record ThanhVien(UUID id, String hoTen) {
    }

    public static ToTrucResponse tu(ToTruc t) {
        return new ToTrucResponse(
                t.getId(), t.getTenTo(), t.getMoTa(),
                t.getThanhVien().stream()
                        .sorted(java.util.Comparator.comparing(NguoiDung::getHoTen))
                        .map(n -> new ThanhVien(n.getId(), n.tenDayDu()))
                        .toList());
    }
}
