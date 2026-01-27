package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseNature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Case Nature Repository (Previously CaseTypeRepository)
 * JPA repository for CaseNature entity
 */
@Repository
public interface CaseNatureRepository extends JpaRepository<CaseNature, Long> {

    /**
     * Find case nature by code
     */
    Optional<CaseNature> findByCode(String code);

    /**
     * Find case nature by name
     */
    Optional<CaseNature> findByName(String name);

    /**
     * Check if case nature exists by code
     */
    boolean existsByCode(String code);

    /**
     * Check if case nature exists by name
     */
    boolean existsByName(String name);

    /**
     * Find all active case natures
     */
    List<CaseNature> findByIsActiveTrueOrderByNameAsc();

    /**
     * Find all case natures ordered by name
     */
    List<CaseNature> findAllByOrderByNameAsc();
}
