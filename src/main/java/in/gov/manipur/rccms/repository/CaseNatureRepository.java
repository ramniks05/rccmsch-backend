package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseNature;
import in.gov.manipur.rccms.entity.CourtLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Case Nature Repository
 * JPA repository for CaseNature entity
 */
@Repository
public interface CaseNatureRepository extends JpaRepository<CaseNature, Long> {

    /**
     * Find case nature by code
     */
    Optional<CaseNature> findByNatureCode(String natureCode);

    /**
     * Find case nature by code and case type ID
     */
    Optional<CaseNature> findByNatureCodeAndCaseTypeId(String natureCode, Long caseTypeId);

    /**
     * Find all active case natures by case type ID
     */
    List<CaseNature> findByCaseTypeIdAndIsActiveTrueOrderByDisplayOrderAscNatureNameAsc(Long caseTypeId);

    /**
     * Find all active case natures
     */
    List<CaseNature> findByIsActiveTrueOrderByDisplayOrderAscNatureNameAsc();

    /**
     * Find all active case natures by court level
     */
    List<CaseNature> findByCourtLevelAndIsActiveTrueOrderByDisplayOrderAscNatureNameAsc(CourtLevel courtLevel);

    /**
     * Find all active case natures by from level (for appeals)
     */
    List<CaseNature> findByFromLevelAndIsActiveTrueOrderByDisplayOrderAscNatureNameAsc(CourtLevel fromLevel);

    /**
     * Find all active appeals
     */
    List<CaseNature> findByIsAppealTrueAndIsActiveTrueOrderByAppealOrderAscNatureNameAsc();

    /**
     * Check if case nature exists by code and case type
     */
    boolean existsByNatureCodeAndCaseTypeId(String natureCode, Long caseTypeId);
}
