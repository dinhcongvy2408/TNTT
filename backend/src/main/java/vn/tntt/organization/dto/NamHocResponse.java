package vn.tntt.organization.dto;

import vn.tntt.organization.entity.NamHoc;
import vn.tntt.organization.entity.TrangThaiNamHoc;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Dữ liệu năm học trả ra cho frontend.
 *
 * <p><b>Vì sao ánh xạ bằng tay chứ không dùng MapStruct?</b> MapStruct đã khai
 * báo sẵn trong pom, nhưng nó đáng dùng khi DTO có nhiều field, có object
 * lồng nhau, hoặc tên field hai bên lệch nhau — lúc đó viết tay vừa dài vừa
 * dễ sót. Ở đây chỉ 5 field trùng tên: một phương thức tĩnh 7 dòng thì đọc
 * hiểu ngay, còn MapStruct sinh code ở {@code target/} mà ta không nhìn thấy.
 * Sprint 4 (bảng {@code thieu_nhi}, ~15 cột) mới là chỗ MapStruct trả công.
 *
 * <p>Nguyên tắc bất di bất dịch của CLAUDE.md mục 5: <b>không bao giờ trả
 * entity ra API</b>. Entity gắn với session Hibernate; trả thẳng ra ngoài là
 * mở đường cho lazy loading chạy lúc Jackson serialize, sinh ra hàng loạt
 * query ngoài tầm kiểm soát.
 */
public record NamHocResponse(
        UUID id,
        String tenNamHoc,
        LocalDate ngayBatDau,
        LocalDate ngayKetThuc,
        TrangThaiNamHoc trangThai
) {

    public static NamHocResponse tu(NamHoc namHoc) {
        return new NamHocResponse(
                namHoc.getId(),
                namHoc.getTenNamHoc(),
                namHoc.getNgayBatDau(),
                namHoc.getNgayKetThuc(),
                namHoc.getTrangThai()
        );
    }
}
