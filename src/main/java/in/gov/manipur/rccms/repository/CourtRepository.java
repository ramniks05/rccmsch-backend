package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.Court;
import in.gov.manipur.rccms.entity.CourtLevel;
import in.gov.manipur.rccms.entity.CourtType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Court Repository
 * JPA repository for Court entity
 */
@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {

    /**
     * Find court by code
     */
    Optional<Court> findByCourtCode(String courtCode);

    /**
     * Check if court exists by code
     */
    boolean existsByCourtCode(String courtCode);

    /**
     * Find all active courts
     */
    List<Court> findByIsActiveTrueOrderByCourtLevelAscCourtNameAsc();

    /**
     * Find all active courts by level
     */
    List<Court> findByCourtLevelAndIsActiveTrueOrderByCourtNameAsc(CourtLevel courtLevel);

    /**
     * Find all active courts by type
     */
    List<Court> findByCourtTypeAndIsActiveTrueOrderByCourtNameAsc(CourtType courtType);

    /**
     * Find all active courts by level and type
     */
    List<Court> findByCourtLevelAndCourtTypeAndIsActiveTrueOrderByCourtNameAsc(CourtLevel courtLevel, CourtType courtType);

    /**
     * Find all active courts by unit ID
     */
    List<Court> findByUnitIdAndIsActiveTrueOrderByCourtNameAsc(Long unitId);

    /**
     * Find all active courts by unit ID and level
     */
    List<Court> findByUnitIdAndCourtLevelAndIsActiveTrueOrderByCourtNameAsc(Long unitId, CourtLevel courtLevel);

    /**
     * Find all active courts by unit ID and type
     */
    List<Court> findByUnitIdAndCourtTypeAndIsActiveTrueOrderByCourtNameAsc(Long unitId, CourtType courtType);

    /**
     * Find all active courts by unit ID, level, and type
     */
    List<Court> findByUnitIdAndCourtLevelAndCourtTypeAndIsActiveTrueOrderByCourtNameAsc(Long unitId, CourtLevel courtLevel, CourtType courtType);

    /**
     * Find courts available for a case nature
     * Based on court level and court types (from case nature)
     */
    @Query("SELECT c FROM Court c WHERE c.courtLevel = :courtLevel " +
           "AND c.courtType IN :courtTypes " +
           "AND c.isActive = true " +
           "AND (c.unitId = :unitId OR c.unitId IN (SELECT au.unitId FROM AdminUnit au WHERE au.parentUnitId = :unitId OR au.parentUnitId IN (SELECT au2.unitId FROM AdminUnit au2 WHERE au2.parentUnitId = :unitId))) " +
           "ORDER BY c.courtName ASC")
    List<Court> findAvailableCourtsForCaseNature(
            @Param("courtLevel") CourtLevel courtLevel,
            @Param("courtTypes") List<CourtType> courtTypes,
            @Param("unitId") Long unitId
    );

    /**
     * Find courts by unit hierarchy (unit and its parent units)
     */
    @Query("SELECT c FROM Court c WHERE c.isActive = true " +
           "AND (c.unitId = :unitId " +
           "OR c.unitId IN (SELECT au.unitId FROM AdminUnit au WHERE au.parentUnitId = :unitId) " +
           "OR c.unitId IN (SELECT au2.unitId FROM AdminUnit au2 WHERE au2.parentUnitId IN (SELECT au.unitId FROM AdminUnit au WHERE au.parentUnitId = :unitId))) " +
           "ORDER BY c.courtLevel ASC, c.courtName ASC")
    List<Court> findCourtsByUnitHierarchy(@Param("unitId") Long unitId);
}
