package vn.tntt.organization.dto;

import vn.tntt.organization.entity.Nganh;

import java.util.UUID;

public record NganhResponse(
        UUID id,
        String tenNganh,
        String maNganh,
        Short tuoiToiThieu,
        Short tuoiToiDa,
        Short thuTu
) {
    public static NganhResponse tu(Nganh nganh) {
        return new NganhResponse(
                nganh.getId(),
                nganh.getTenNganh(),
                nganh.getMaNganh(),
                nganh.getTuoiToiThieu(),
                nganh.getTuoiToiDa(),
                nganh.getThuTu()
        );
    }
}
