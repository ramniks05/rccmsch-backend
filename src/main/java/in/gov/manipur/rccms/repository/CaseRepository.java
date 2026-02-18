package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Case Repository
 */
@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {

    Optional<Case> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);

    List<Case> findByApplicantIdOrderByApplicationDateDesc(Long applicantId);

    List<Case> findByUnitIdOrderByApplicationDateDesc(Long unitId);

    List<Case> findByCaseTypeIdOrderByApplicationDateDesc(Long caseTypeId);

    List<Case> findByStatusOrderByApplicationDateDesc(String status);

    @Query("SELECT c FROM Case c WHERE c.unitId = :unitId AND c.status = :status ORDER BY c.applicationDate DESC")
    List<Case> findByUnitIdAndStatus(@Param("unitId") Long unitId, @Param("status") String status);

    @Query("SELECT c FROM Case c WHERE c.applicationDate BETWEEN :startDate AND :endDate ORDER BY c.applicationDate DESC")
    List<Case> findByApplicationDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT c FROM Case c WHERE c.applicantId = :applicantId AND c.isActive = true ORDER BY c.applicationDate DESC")
    List<Case> findActiveCasesByApplicant(@Param("applicantId") Long applicantId);

    long countByIsActiveTrue();

    long countByStatusAndIsActiveTrue(String status);

    @Query("SELECT c FROM Case c WHERE c.hearingDate = :date AND c.isActive = true ORDER BY c.courtId, c.caseNumber")
    List<Case> findByHearingDateAndIsActiveTrue(@Param("date") LocalDate date);

    @Query("SELECT c FROM Case c WHERE c.courtId = :courtId AND c.hearingDate BETWEEN :startDate AND :endDate AND c.isActive = true ORDER BY c.hearingDate, c.caseNumber")
    List<Case> findByCourtIdAndHearingDateBetweenAndIsActiveTrue(
            @Param("courtId") Long courtId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
                       SELECT c.hearingDate, c.court.courtName, COUNT(c) FROM Case c WHERE c.hearingDate BETWEEN :startDate AND :endDate
            AND c.isActive = true AND (:courtId IS NULL OR c.courtId = :courtId) GROUP BY c.hearingDate, c.court.courtName ORDER BY c.hearingDate""")
    List<Object[]> findMonthlyHearings(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("courtId") Long courtId
    );

}

