package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.WorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Workflow History Repository
 */
@Repository
public interface WorkflowHistoryRepository extends JpaRepository<WorkflowHistory, Long> {

    @Query("SELECT wh FROM WorkflowHistory wh " +
           "LEFT JOIN FETCH wh.transition " +
           "LEFT JOIN FETCH wh.fromState " +
           "LEFT JOIN FETCH wh.toState " +
           "LEFT JOIN FETCH wh.performedByOfficer " +
           "LEFT JOIN FETCH wh.performedAtUnit " +
           "WHERE wh.caseId = :caseId ORDER BY wh.performedAt DESC")
    List<WorkflowHistory> findCaseHistory(@Param("caseId") Long caseId);

    /** Count history entries with metadata containing the given substring (e.g. NOTICE_ACCEPTED) for this case */
    @Query("SELECT COUNT(wh) FROM WorkflowHistory wh WHERE wh.caseId = :caseId AND wh.metadata LIKE CONCAT('%', :substring, '%')")
    long countByCaseIdAndMetadataContaining(@Param("caseId") Long caseId, @Param("substring") String substring);
}

