package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.ModuleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleMasterRepository extends JpaRepository<ModuleMaster, Long> {
    Optional<ModuleMaster> findByCodeIgnoreCase(String code);
    Optional<ModuleMaster> findByCodeIgnoreCaseAndIsActiveTrue(String code);
    List<ModuleMaster> findByIsActiveTrueOrderByNameAsc();
}
