package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.FormFieldGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FormFieldGroup
 */
@Repository
public interface FormFieldGroupRepository extends JpaRepository<FormFieldGroup, Long> {

    /**
     * Find all active groups for a case type, ordered by display order
     */
    @Query("SELECT g FROM FormFieldGroup g WHERE g.caseTypeId = :caseTypeId AND g.isActive = true ORDER BY g.displayOrder ASC, g.id ASC")
    List<FormFieldGroup> findActiveGroupsByCaseTypeId(@Param("caseTypeId") Long caseTypeId);

    /**
     * Find all groups (active and inactive) for a case type, ordered by display order
     */
    @Query("SELECT g FROM FormFieldGroup g WHERE g.caseTypeId = :caseTypeId ORDER BY g.displayOrder ASC, g.id ASC")
    List<FormFieldGroup> findAllGroupsByCaseTypeId(@Param("caseTypeId") Long caseTypeId);

    /**
     * Find group by case type and group code
     */
    Optional<FormFieldGroup> findByCaseTypeIdAndGroupCode(Long caseTypeId, String groupCode);

    /**
     * Check if group exists for a case type
     */
    boolean existsByCaseTypeIdAndGroupCode(Long caseTypeId, String groupCode);
}
