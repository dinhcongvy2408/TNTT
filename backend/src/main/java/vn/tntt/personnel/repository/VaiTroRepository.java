package vn.tntt.personnel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.tntt.personnel.entity.VaiTro;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VaiTroRepository extends JpaRepository<VaiTro, UUID> {
    Optional<VaiTro> findByMa(String ma);
}
