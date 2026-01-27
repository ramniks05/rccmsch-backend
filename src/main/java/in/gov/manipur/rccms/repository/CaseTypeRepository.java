package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.entity.CourtLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Case Type Repository (Previously CaseNatureRepository)
 * JPA repository for CaseType entity
 */
@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, Long> {

    /**
     * Find case type by code
     */
    Optional<CaseType> findByTypeCode(String typeCode);

    /**
     * Find case type by code and case nature ID
     */
    Optional<CaseType> findByTypeCodeAndCaseNatureId(String typeCode, Long caseNatureId);

    /**
     * Find all active case types by case nature ID
     */
    List<CaseType> findByCaseNatureIdAndIsActiveTrueOrderByDisplayOrderAscTypeNameAsc(Long caseNatureId);

    /**
     * Find all active case types by case nature ID (with eager fetch of CaseNature)
     */
    @Query("SELECT ct FROM CaseType ct JOIN FETCH ct.caseNature WHERE ct.caseNatureId = :caseNatureId AND ct.isActive = true ORDER BY ct.displayOrder ASC, ct.typeName ASC")
    List<CaseType> findByCaseNatureIdAndIsActiveTrueWithCaseNature(@Param("caseNatureId") Long caseNatureId);

    /**
     * Find all active case types
     */
    List<CaseType> findByIsActiveTrueOrderByDisplayOrderAscTypeNameAsc();

    /**
     * Find all active case types (with eager fetch of CaseNature)
     */
    @Query("SELECT ct FROM CaseType ct JOIN FETCH ct.caseNature WHERE ct.isActive = true ORDER BY ct.displayOrder ASC, ct.typeName ASC")
    List<CaseType> findAllActiveWithCaseNature();

    /**
     * Find all active case types by court level
     */
    List<CaseType> findByCourtLevelAndIsActiveTrueOrderByDisplayOrderAscTypeNameAsc(CourtLevel courtLevel);

    /**
     * Find all active case types by from level (for appeals)
     */
    List<CaseType> findByFromLevelAndIsActiveTrueOrderByDisplayOrderAscTypeNameAsc(CourtLevel fromLevel);

    /**
     * Find all active appeals
     */
    List<CaseType> findByIsAppealTrueAndIsActiveTrueOrderByAppealOrderAscTypeNameAsc();

    /**
     * Check if case type exists by code and case nature
     */
    boolean existsByTypeCodeAndCaseNatureId(String typeCode, Long caseNatureId);
}
