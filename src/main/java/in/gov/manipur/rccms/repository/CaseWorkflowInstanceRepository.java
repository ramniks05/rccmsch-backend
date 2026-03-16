package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseWorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Case Workflow Instance Repository
 */
@Repository
public interface CaseWorkflowInstanceRepository extends JpaRepository<CaseWorkflowInstance, Long> {
    
    Optional<CaseWorkflowInstance> findByCaseId(Long caseId);
    
    List<CaseWorkflowInstance> findByCurrentStateId(Long currentStateId);
    
    @Query("SELECT cwi FROM CaseWorkflowInstance cwi " +
           "LEFT JOIN FETCH cwi.caseEntity " +
           "WHERE cwi.assignedToOfficerId = :officerId " +
           "ORDER BY cwi.updatedAt DESC")
    List<CaseWorkflowInstance> findByAssignedToOfficerId(@Param("officerId") Long officerId);
    
    List<CaseWorkflowInstance> findByAssignedToUnitId(Long unitId);
    
    List<CaseWorkflowInstance> findByAssignedToRole(String roleCode);

    List<CaseWorkflowInstance> findByAssignedToRoleId(Long roleId);
    
    @Query("SELECT cwi FROM CaseWorkflowInstance cwi WHERE cwi.assignedToOfficerId = :officerId " +
           "AND cwi.assignedToRole = :roleCode ORDER BY cwi.updatedAt DESC")
    List<CaseWorkflowInstance> findByAssignedOfficerAndRole(
            @Param("officerId") Long officerId, 
            @Param("roleCode") String roleCode);

    @Query("SELECT cwi FROM CaseWorkflowInstance cwi WHERE cwi.assignedToOfficerId = :officerId " +
           "AND cwi.assignedToRoleId = :roleId ORDER BY cwi.updatedAt DESC")
    List<CaseWorkflowInstance> findByAssignedOfficerAndRoleId(
            @Param("officerId") Long officerId,
            @Param("roleId") Long roleId);
    
    @Query("SELECT cwi FROM CaseWorkflowInstance cwi WHERE cwi.assignedToUnitId = :unitId " +
           "AND cwi.currentStateId = :stateId")
    List<CaseWorkflowInstance> findByUnitAndState(@Param("unitId") Long unitId, @Param("stateId") Long stateId);
    
    /**
     * Find all workflow instances where case is not assigned to any officer
     */
    @Query("SELECT cwi FROM CaseWorkflowInstance cwi WHERE cwi.assignedToOfficerId IS NULL")
    List<CaseWorkflowInstance> findUnassignedCases();
    
    /**
     * Find unassigned cases by court
     */
    @Query("SELECT cwi FROM CaseWorkflowInstance cwi " +
           "WHERE cwi.assignedToOfficerId IS NULL " +
           "AND cwi.caseEntity.courtId = :courtId")
    List<CaseWorkflowInstance> findUnassignedCasesByCourt(@Param("courtId") Long courtId);
    
    /**
     * Find unassigned cases by unit
     */
    @Query("SELECT cwi FROM CaseWorkflowInstance cwi " +
           "WHERE cwi.assignedToOfficerId IS NULL " +
           "AND cwi.assignedToUnitId = :unitId")
    List<CaseWorkflowInstance> findUnassignedCasesByUnit(@Param("unitId") Long unitId);

    long countByCurrentState_IsFinalStateFalseAndCaseEntity_IsActiveTrue();

    long countByCurrentState_IsFinalStateTrueAndCaseEntity_IsActiveTrue();
}

