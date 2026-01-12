package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Case Type Repository
 * JPA repository for CaseType entity
 */
@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, Long> {

    /**
     * Find case type by code
     */
    Optional<CaseType> findByCode(String code);

    /**
     * Find case type by name
     */
    Optional<CaseType> findByName(String name);

    /**
     * Check if case type exists by code
     */
    boolean existsByCode(String code);

    /**
     * Check if case type exists by name
     */
    boolean existsByName(String name);

    /**
     * Find all active case types
     */
    List<CaseType> findByIsActiveTrueOrderByNameAsc();

    /**
     * Find all case types ordered by name
     */
    List<CaseType> findAllByOrderByNameAsc();
}

