package in.gov.manipur.rccms.repository;

import in.gov.manipur.rccms.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * System Settings Repository
 */
@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {
    
    /**
     * Find active system settings (singleton - should be only one)
     */
    Optional<SystemSettings> findByIsActiveTrue();
    
    /**
     * Find the first system settings record (for singleton pattern)
     */
    Optional<SystemSettings> findFirstByOrderByIdAsc();
}

