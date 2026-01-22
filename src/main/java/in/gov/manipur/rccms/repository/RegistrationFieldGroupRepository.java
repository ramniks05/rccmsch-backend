package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.RegistrationFieldGroup;
import in.gov.manipur.rccms.entity.RegistrationFormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for RegistrationFieldGroup
 */
@Repository
public interface RegistrationFieldGroupRepository extends JpaRepository<RegistrationFieldGroup, Long> {

    @Query("SELECT g FROM RegistrationFieldGroup g WHERE g.registrationType = :type AND g.isActive = true ORDER BY g.displayOrder ASC, g.id ASC")
    List<RegistrationFieldGroup> findActiveGroupsByType(@Param("type") RegistrationFormField.RegistrationType type);

    @Query("SELECT g FROM RegistrationFieldGroup g WHERE g.registrationType = :type ORDER BY g.displayOrder ASC, g.id ASC")
    List<RegistrationFieldGroup> findAllGroupsByType(@Param("type") RegistrationFormField.RegistrationType type);

    Optional<RegistrationFieldGroup> findByRegistrationTypeAndGroupCode(RegistrationFormField.RegistrationType type, String groupCode);
}
