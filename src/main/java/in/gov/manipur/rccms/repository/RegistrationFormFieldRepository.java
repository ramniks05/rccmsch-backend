package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.RegistrationFormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RegistrationFormField
 */
@Repository
public interface RegistrationFormFieldRepository extends JpaRepository<RegistrationFormField, Long> {

    @Query("SELECT f FROM RegistrationFormField f WHERE f.registrationType = :type AND f.isActive = true ORDER BY f.displayOrder ASC, f.id ASC")
    List<RegistrationFormField> findActiveFieldsByType(@Param("type") RegistrationFormField.RegistrationType type);

    @Query("SELECT f FROM RegistrationFormField f WHERE f.registrationType = :type ORDER BY f.displayOrder ASC, f.id ASC")
    List<RegistrationFormField> findAllFieldsByType(@Param("type") RegistrationFormField.RegistrationType type);

    Optional<RegistrationFormField> findByRegistrationTypeAndFieldName(RegistrationFormField.RegistrationType type, String fieldName);

    @Query("SELECT DISTINCT f.fieldGroup FROM RegistrationFormField f WHERE f.registrationType = :type AND f.fieldGroup IS NOT NULL ORDER BY f.fieldGroup ASC")
    List<String> findDistinctFieldGroups(@Param("type") RegistrationFormField.RegistrationType type);
}
