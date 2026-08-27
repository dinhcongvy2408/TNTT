package vn.tntt.organization.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tntt.organization.entity.Nganh;

import java.util.List;
import java.util.UUID;

@Repository
public interface NganhRepository extends JpaRepository<Nganh, UUID> {

    /** Sắp theo thứ tự chuyển cấp: Chiên Con trước, Hiệp Sĩ sau. */
    List<Nganh> findAllByOrderByThuTuAsc();

    boolean existsByMaNganh(String maNganh);

    boolean existsByTenNganh(String tenNganh);

    boolean existsByThuTu(Short thuTu);
}
