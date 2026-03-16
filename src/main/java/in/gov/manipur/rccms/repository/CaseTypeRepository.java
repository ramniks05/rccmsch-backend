package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Case Type Repository (Previously CaseNatureRepository)
 * JPA repository for CaseType entity
 */
@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, Long> {

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
     * Find all active case types (with eager fetch of CaseNature)
     */
    @Query("SELECT ct FROM CaseType ct JOIN FETCH ct.caseNature WHERE ct.isActive = true ORDER BY ct.displayOrder ASC, ct.typeName ASC")
    List<CaseType> findAllActiveWithCaseNature();

    /**
     * Check if case type exists by code and case nature
     */
    boolean existsByTypeCodeAndCaseNatureId(String typeCode, Long caseNatureId);
}
