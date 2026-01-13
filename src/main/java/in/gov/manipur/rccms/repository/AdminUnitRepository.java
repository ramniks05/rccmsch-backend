package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.AdminUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Administrative Unit Repository
 * JPA repository for AdminUnit entity
 */
@Repository
public interface AdminUnitRepository extends JpaRepository<AdminUnit, Long> {

    /**
     * Find admin unit by code
     */
    Optional<AdminUnit> findByUnitCode(String unitCode);

    /**
     * Find admin unit by LGD code
     */
    Optional<AdminUnit> findByLgdCode(Long lgdCode);

    /**
     * Check if admin unit exists by code
     */
    boolean existsByUnitCode(String unitCode);

    /**
     * Check if admin unit exists by LGD code
     */
    boolean existsByLgdCode(Long lgdCode);

    /**
     * Find all admin units by level
     */
    List<AdminUnit> findByUnitLevel(AdminUnit.UnitLevel unitLevel);

    /**
     * Find all active admin units by level
     */
    List<AdminUnit> findByUnitLevelAndIsActiveTrue(AdminUnit.UnitLevel unitLevel);

    /**
     * Find all admin units by parent unit ID
     */
    List<AdminUnit> findByParentUnitId(Long parentUnitId);

    /**
     * Find all active admin units by parent unit ID
     */
    List<AdminUnit> findByParentUnitIdAndIsActiveTrue(Long parentUnitId);

    /**
     * Find all active admin units
     */
    List<AdminUnit> findByIsActiveTrueOrderByUnitLevelAscUnitNameAsc();

    /**
     * Find root units (State level - no parent)
     */
    @Query("SELECT au FROM AdminUnit au WHERE au.parentUnit IS NULL AND au.isActive = true ORDER BY au.unitName")
    List<AdminUnit> findRootUnits();

    /**
     * Find all units ordered by level and name
     */
    List<AdminUnit> findAllByOrderByUnitLevelAscUnitNameAsc();
}

