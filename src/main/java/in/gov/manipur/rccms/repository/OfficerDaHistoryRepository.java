package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.OfficerDaHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Officer DA History Repository
 * JPA repository for OfficerDaHistory entity (Posting History)
 */
@Repository
public interface OfficerDaHistoryRepository extends JpaRepository<OfficerDaHistory, Long> {

    /**
     * Find posting by UserID (generated format: ROLE_CODE@COURT_CODE)
     */
    @Query("SELECT p FROM OfficerDaHistory p WHERE p.postingUserid = :userid")
    Optional<OfficerDaHistory> findByPostingUserid(@Param("userid") String userid);

    /**
     * Find active posting by UserID (with eager fetching of court, unit, and officer)
     * Handles both court-based and unit-based postings
     */
    @Query("SELECT p FROM OfficerDaHistory p " +
           "LEFT JOIN FETCH p.court c " +
           "LEFT JOIN FETCH c.unit cu " +
           "LEFT JOIN FETCH cu.parentUnit " +
           "LEFT JOIN FETCH p.unit u " +
           "LEFT JOIN FETCH u.parentUnit " +
           "LEFT JOIN FETCH p.officer " +
           "WHERE p.postingUserid = :userid AND p.isCurrent = true")
    Optional<OfficerDaHistory> findByPostingUseridAndIsCurrentTrue(@Param("userid") String userid);

    /**
     * Find active posting by court and role
     */
    Optional<OfficerDaHistory> findByCourtIdAndRoleCodeAndIsCurrentTrue(Long courtId, String roleCode);

    /**
     * Find all active postings by court
     */
    List<OfficerDaHistory> findByCourtIdAndIsCurrentTrue(Long courtId);

    /**
     * Find all postings by officer (person)
     */
    List<OfficerDaHistory> findByOfficerIdOrderByFromDateDesc(Long officerId);

    /**
     * Find all active postings by officer
     */
    List<OfficerDaHistory> findByOfficerIdAndIsCurrentTrue(Long officerId);

    /**
     * Find all postings by court
     */
    List<OfficerDaHistory> findByCourtIdOrderByFromDateDesc(Long courtId);

    /**
     * Find all postings by role code
     */
    List<OfficerDaHistory> findByRoleCodeAndIsCurrentTrueOrderByFromDateDesc(String roleCode);

    /**
     * Check if active posting exists for court and role
     */
    boolean existsByCourtIdAndRoleCodeAndIsCurrentTrue(Long courtId, String roleCode);

    /**
     * Find postings that need to be closed (for transfer logic)
     */
    @Query("SELECT p FROM OfficerDaHistory p WHERE p.courtId = :courtId AND p.roleCode = :roleCode AND p.isCurrent = true")
    List<OfficerDaHistory> findActivePostingsByCourtAndRole(@Param("courtId") Long courtId, @Param("roleCode") String roleCode);

    /**
     * Find all active postings
     */
    List<OfficerDaHistory> findByIsCurrentTrueOrderByFromDateDesc();

    /**
     * Find all active postings by unit (through court - court-based postings)
     */
    @Query("SELECT p FROM OfficerDaHistory p WHERE p.court.unitId = :unitId AND p.isCurrent = true")
    List<OfficerDaHistory> findActivePostingsByUnit(@Param("unitId") Long unitId);

    /**
     * Find active unit-based postings by unit ID
     */
    List<OfficerDaHistory> findByUnitIdAndIsCurrentTrue(Long unitId);

    /**
     * Find active unit-based posting by unit and role
     */
    Optional<OfficerDaHistory> findByUnitIdAndRoleCodeAndIsCurrentTrue(Long unitId, String roleCode);

    /**
     * Find active postings by unit and role (for closing existing postings)
     */
    @Query("SELECT p FROM OfficerDaHistory p WHERE p.unitId = :unitId AND p.roleCode = :roleCode AND p.isCurrent = true")
    List<OfficerDaHistory> findActivePostingsByUnitAndRole(@Param("unitId") Long unitId, @Param("roleCode") String roleCode);

    /**
     * Find field officers (unit-based postings) available to a court
     * Searches unit hierarchy: finds unit-based officers in units under the court's unit
     */
    @Query("SELECT p FROM OfficerDaHistory p " +
           "WHERE p.isCurrent = true " +
           "AND p.courtId IS NULL " +
           "AND p.roleCode = :roleCode " +
           "AND (p.unit.unitId = :unitId " +
           "     OR p.unit.parentUnitId = :unitId " +
           "     OR EXISTS (SELECT 1 FROM AdminUnit au WHERE au.unitId = :unitId AND au.parentUnitId = p.unit.unitId))")
    List<OfficerDaHistory> findFieldOfficersForCourt(@Param("unitId") Long unitId, @Param("roleCode") String roleCode);

    /**
     * Find all unit-based postings (field officers) by role
     */
    @Query("SELECT p FROM OfficerDaHistory p WHERE p.courtId IS NULL AND p.roleCode = :roleCode AND p.isCurrent = true ORDER BY p.fromDate DESC")
    List<OfficerDaHistory> findUnitBasedPostingsByRole(@Param("roleCode") String roleCode);
}

