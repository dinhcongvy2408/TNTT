package vn.tntt.discipline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.tntt.common.entity.BaseEntity;
import vn.tntt.personnel.entity.NguoiDung;

import java.util.HashSet;
import java.util.Set;

/**
 * Một tổ trực cổng, VD "Tổ Kỷ Luật 1" (docs/02 mục 6.1).
 *
 * <p>Bảng nối {@code thanh_vien_to_truc} có thêm cột {@code la_to_truong}, tức
 * là nó mang dữ liệu riêng chứ không thuần tuý là quan hệ N-N. Ở lát cắt này
 * ta chỉ cần biết "ai thuộc tổ nào" nên ánh xạ {@code @ManyToMany} là đủ; cột
 * {@code la_to_truong} vẫn nằm trong DB với giá trị mặc định false.
 *
 * <p>Khi nào cần đọc hay đặt {@code la_to_truong} thì phải tách bảng nối thành
 * entity riêng — đó là dấu hiệu nhận biết: bảng nối có thuộc tính của riêng nó
 * thì nó là một thực thể, không phải một quan hệ.
 */
@Entity
@Table(name = "to_truc")
@Getter
@Setter
@NoArgsConstructor
public class ToTruc extends BaseEntity {

    @Column(name = "ten_to", nullable = false, length = 80, unique = true)
    private String tenTo;

    @Column(name = "mo_ta", columnDefinition = "text")
    private String moTa;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "thanh_vien_to_truc",
            joinColumns = @JoinColumn(name = "to_truc_id"),
            inverseJoinColumns = @JoinColumn(name = "nguoi_dung_id"))
    private Set<NguoiDung> thanhVien = new HashSet<>();
}
