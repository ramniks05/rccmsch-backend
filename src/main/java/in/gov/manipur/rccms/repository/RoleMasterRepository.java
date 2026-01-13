package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.RoleMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Role Master Repository
 * JPA repository for RoleMaster entity
 */
@Repository
public interface RoleMasterRepository extends JpaRepository<RoleMaster, Long> {

    /**
     * Find role by code
     */
    Optional<RoleMaster> findByRoleCode(String roleCode);

    /**
     * Check if role exists by code
     */
    boolean existsByRoleCode(String roleCode);

    /**
     * Find all roles by unit level
     */
    List<RoleMaster> findByUnitLevel(AdminUnit.UnitLevel unitLevel);

    /**
     * Find all roles ordered by role code
     */
    List<RoleMaster> findAllByOrderByRoleCodeAsc();
}

