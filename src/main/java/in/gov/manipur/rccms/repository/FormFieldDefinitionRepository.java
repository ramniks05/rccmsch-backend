package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.FormFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FormFieldDefinition
 */
@Repository
public interface FormFieldDefinitionRepository extends JpaRepository<FormFieldDefinition, Long> {

    /**
     * Find all active fields for a case type, ordered by field group, then display order
     * Note: Group ordering is handled by joining with FormFieldGroup in the service layer
     */
    @Query("SELECT f FROM FormFieldDefinition f WHERE f.caseTypeId = :caseTypeId AND f.isActive = true ORDER BY f.fieldGroup ASC NULLS LAST, f.displayOrder ASC, f.id ASC")
    List<FormFieldDefinition> findActiveFieldsByCaseTypeId(@Param("caseTypeId") Long caseTypeId);

    /**
     * Find all fields (active and inactive) for a case type, ordered by field group, then display order
     * Note: Group ordering is handled by joining with FormFieldGroup in the service layer
     */
    @Query("SELECT f FROM FormFieldDefinition f WHERE f.caseTypeId = :caseTypeId ORDER BY f.fieldGroup ASC NULLS LAST, f.displayOrder ASC, f.id ASC")
    List<FormFieldDefinition> findAllFieldsByCaseTypeId(@Param("caseTypeId") Long caseTypeId);

    /**
     * Find field by case type and field name
     */
    Optional<FormFieldDefinition> findByCaseTypeIdAndFieldName(Long caseTypeId, String fieldName);

    /**
     * Check if field name exists for a case type
     */
    boolean existsByCaseTypeIdAndFieldName(Long caseTypeId, String fieldName);

    /**
     * Count active fields for a case type
     */
    long countByCaseTypeIdAndIsActiveTrue(Long caseTypeId);

    /**
     * Find fields by case type and group, ordered by display order
     */
    @Query("SELECT f FROM FormFieldDefinition f WHERE f.caseTypeId = :caseTypeId AND f.fieldGroup = :fieldGroup AND f.isActive = true ORDER BY f.displayOrder ASC")
    List<FormFieldDefinition> findByCaseTypeIdAndFieldGroup(@Param("caseTypeId") Long caseTypeId, @Param("fieldGroup") String fieldGroup);
}

