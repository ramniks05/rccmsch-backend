package in.gov.manipur.rccms.config;

import in.gov.manipur.rccms.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * System Settings Data Initializer
 * Initializes default system settings on application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Run early, before other initializers
public class SystemSettingsDataInitializer implements CommandLineRunner {

    private final SystemSettingsRepository systemSettingsRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing System Settings...");
        
        // Check if settings already exist
        if (systemSettingsRepository.findByIsActiveTrue().isEmpty()) {
            log.info("No system settings found - System settings should be configured via admin APIs for Chandigarh");
            log.info("Skipping default system settings initialization - please configure through admin panel");
            // System settings will be configured through admin APIs based on Chandigarh's requirements
        } else {
            log.info("System settings already exist, skipping initialization");
        }
    }
}

