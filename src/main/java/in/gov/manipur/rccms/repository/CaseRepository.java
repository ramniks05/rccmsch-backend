package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.Projection.CalendarHearingProjection;
import in.gov.manipur.rccms.Projection.CauseListProjection;
import in.gov.manipur.rccms.Projection.OfficerCaseStatsProjection;
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

    List<Case> findByStatusOrderByApplicationDateDesc(String status);

    @Query("SELECT c FROM Case c WHERE c.applicantId = :applicantId AND c.isActive = true ORDER BY c.applicationDate DESC")
    List<Case> findActiveCasesByApplicant(@Param("applicantId") Long applicantId);

    long countByIsActiveTrue();

    long countByStatusInAndIsActiveTrue(java.util.List<String> statuses);

    @Query("SELECT c FROM Case c WHERE c.hearingDate = :date AND c.isActive = true ORDER BY c.courtId, c.caseNumber")
    List<Case> findByHearingDateAndIsActiveTrue(@Param("date") LocalDate date);

    @Query("SELECT c FROM Case c WHERE c.courtId = :courtId AND c.hearingDate BETWEEN :startDate AND :endDate AND c.isActive = true ORDER BY c.hearingDate, c.caseNumber")
    List<Case> findByCourtIdAndHearingDateBetweenAndIsActiveTrue(
            @Param("courtId") Long courtId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
                       SELECT c.hearingDate as hearingDate, c.court.courtName as courtName, COUNT(c) as totalCases FROM Case c WHERE c.hearingDate BETWEEN :startDate AND :endDate
            AND c.isActive = true AND (:courtId IS NULL OR c.courtId = :courtId) GROUP BY c.hearingDate, c.court.courtName ORDER BY c.hearingDate""")
    List<CalendarHearingProjection> findMonthlyHearings(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("courtId") Long courtId
    );

    @Query("""
            SELECT  c.court.courtName as courtName, c.court.address as courtAddress, COUNT(c) as totalCases, c.hearingDate as hearingDate FROM Case c WHERE c.isActive = true
            AND (:courtId IS NULL OR c.court.id = :courtId) AND c.hearingDate is not NULL GROUP BY c.court.courtName, c.court.address, c.hearingDate
            ORDER BY c.hearingDate DESC""")
    List<CauseListProjection> getCauseList(@Param("courtId") Long courtId);

    @Query(value = """
            SELECT 
                o.full_name AS name,
                ohd.role_code AS designation,
                au.unit_name AS district,
                COUNT(DISTINCT CASE WHEN ws.is_final_state = false THEN c.id END) AS pending,
                COUNT(DISTINCT CASE WHEN ws.is_final_state = true THEN c.id END) AS disposed,
                COUNT(DISTINCT c.id) AS totalCases
            FROM workflow_history wh
            JOIN cases c ON c.id = wh.case_id
            JOIN case_workflow_instance cwi ON cwi.case_id = c.id
            JOIN workflow_state ws ON ws.id = cwi.current_state_id
            JOIN officers o ON o.id = wh.performed_by_officer_id
            JOIN officer_da_history ohd ON ohd.officer_id = o.id
            LEFT JOIN admin_unit au ON au.unit_id = wh.performed_at_unit_id
            WHERE c.is_active = true
            GROUP BY o.id, o.full_name, ohd.role_code, au.unit_name
            """, nativeQuery = true)
    List<OfficerCaseStatsProjection> getOfficerCaseStats();


}

