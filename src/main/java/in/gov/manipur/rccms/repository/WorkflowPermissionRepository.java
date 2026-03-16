package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.WorkflowPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Workflow Permission Repository
 */
@Repository
public interface WorkflowPermissionRepository extends JpaRepository<WorkflowPermission, Long> {
    
    List<WorkflowPermission> findByTransitionIdAndIsActiveTrue(Long transitionId);
    
    List<WorkflowPermission> findByTransitionId(Long transitionId);
    
    @Query("SELECT wp FROM WorkflowPermission wp WHERE wp.transitionId = :transitionId " +
           "AND wp.roleCode = :roleCode AND wp.isActive = true " +
           "AND (wp.unitLevel = :unitLevel OR wp.unitLevel IS NULL)")
    List<WorkflowPermission> findPermissionsForTransitionAndRole(
            @Param("transitionId") Long transitionId,
            @Param("roleCode") String roleCode,
            @Param("unitLevel") AdminUnit.UnitLevel unitLevel);
    
    @Query("SELECT wp FROM WorkflowPermission wp WHERE wp.transitionId = :transitionId " +
           "AND wp.roleCode = :roleCode AND wp.isActive = true")
    List<WorkflowPermission> findPermissionsForTransitionAndRoleAnyLevel(
            @Param("transitionId") Long transitionId,
            @Param("roleCode") String roleCode);

    @Query("SELECT wp FROM WorkflowPermission wp WHERE wp.transitionId = :transitionId " +
           "AND wp.roleId = :roleId AND wp.isActive = true " +
           "AND (wp.unitLevel = :unitLevel OR wp.unitLevel IS NULL)")
    List<WorkflowPermission> findPermissionsForTransitionAndRoleId(
            @Param("transitionId") Long transitionId,
            @Param("roleId") Long roleId,
            @Param("unitLevel") AdminUnit.UnitLevel unitLevel);

    @Query("SELECT wp FROM WorkflowPermission wp WHERE wp.transitionId = :transitionId " +
           "AND wp.roleId = :roleId AND wp.isActive = true")
    List<WorkflowPermission> findPermissionsForTransitionAndRoleIdAnyLevel(
            @Param("transitionId") Long transitionId,
            @Param("roleId") Long roleId);
    
    boolean existsByTransitionIdAndRoleCodeAndUnitLevelAndIsActiveTrue(
            Long transitionId, String roleCode, AdminUnit.UnitLevel unitLevel);

    /** Same as above but excludes the given permission id (for update: avoid counting current row). */
    @Query("SELECT COUNT(wp) > 0 FROM WorkflowPermission wp WHERE wp.transitionId = :transitionId " +
           "AND wp.roleCode = :roleCode AND wp.isActive = true " +
           "AND (wp.unitLevel = :unitLevel OR (:unitLevel IS NULL AND wp.unitLevel IS NULL)) " +
           "AND wp.id <> :excludeId")
    boolean existsByTransitionIdAndRoleCodeAndUnitLevelAndIsActiveTrueAndIdNot(
            @Param("transitionId") Long transitionId,
            @Param("roleCode") String roleCode,
            @Param("unitLevel") AdminUnit.UnitLevel unitLevel,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(wp) > 0 FROM WorkflowPermission wp WHERE wp.transitionId = :transitionId " +
           "AND wp.roleCode = :roleCode AND wp.isActive = true " +
           "AND (wp.unitLevel = :unitLevel OR (:unitLevel IS NULL AND wp.unitLevel IS NULL))")
    boolean existsPermissionForTransitionAndRole(
            @Param("transitionId") Long transitionId,
            @Param("roleCode") String roleCode,
            @Param("unitLevel") AdminUnit.UnitLevel unitLevel);
}

