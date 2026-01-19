package in.gov.manipur.rccms.config;

import in.gov.manipur.rccms.entity.SystemSettings;
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
            log.info("No system settings found, creating default settings");
            
            SystemSettings settings = new SystemSettings();
            settings.setLogoUrl("/assets/images/logo.png");
            settings.setLogoHeader("Revenue & Settlement Department");
            settings.setLogoSubheader("Government of Manipur");
            settings.setStateName("Manipur");
            settings.setFooterText("Revenue & Settlement Department, Government of Manipur");
            settings.setFooterCopyright("© 2024 Government of Manipur. All rights reserved.");
            settings.setFooterAddress("Imphal, Manipur, India");
            settings.setFooterEmail("info@manipur.gov.in");
            settings.setFooterPhone("+91-XXX-XXXXXXX");
            settings.setFooterWebsite("https://manipur.gov.in");
            settings.setIsActive(true);
            
            systemSettingsRepository.save(settings);
            log.info("Default system settings created successfully");
        } else {
            log.info("System settings already exist, skipping initialization");
        }
    }
}

