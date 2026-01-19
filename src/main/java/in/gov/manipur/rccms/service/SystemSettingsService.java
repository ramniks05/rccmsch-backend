package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.SystemSettingsDTO;
import in.gov.manipur.rccms.dto.UpdateSystemSettingsDTO;
import in.gov.manipur.rccms.entity.SystemSettings;
import in.gov.manipur.rccms.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * System Settings Service
 * Manages system-wide settings like logo, header, footer, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingsService {

    private final SystemSettingsRepository systemSettingsRepository;

    /**
     * Get current system settings
     * Returns active settings or creates default if none exist
     */
    public SystemSettingsDTO getSystemSettings() {
        log.info("Getting system settings");
        
        SystemSettings settings = systemSettingsRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    log.info("No active settings found, creating default settings");
                    return createDefaultSettings();
                });
        
        return mapToDTO(settings);
    }

    /**
     * Update system settings
     * Updates existing settings or creates new if none exist
     */
    public SystemSettingsDTO updateSystemSettings(UpdateSystemSettingsDTO dto) {
        log.info("Updating system settings");
        
        SystemSettings settings = systemSettingsRepository.findByIsActiveTrue()
                .orElseGet(() -> {
                    log.info("No active settings found, creating new settings");
                    SystemSettings newSettings = new SystemSettings();
                    newSettings.setIsActive(true);
                    return systemSettingsRepository.save(newSettings);
                });
        
        // Update only provided fields (partial update)
        if (dto.getLogoUrl() != null) {
            settings.setLogoUrl(dto.getLogoUrl());
        }
        if (dto.getLogoHeader() != null) {
            settings.setLogoHeader(dto.getLogoHeader());
        }
        if (dto.getLogoSubheader() != null) {
            settings.setLogoSubheader(dto.getLogoSubheader());
        }
        if (dto.getStateName() != null) {
            settings.setStateName(dto.getStateName());
        }
        if (dto.getFooterText() != null) {
            settings.setFooterText(dto.getFooterText());
        }
        if (dto.getFooterCopyright() != null) {
            settings.setFooterCopyright(dto.getFooterCopyright());
        }
        if (dto.getFooterAddress() != null) {
            settings.setFooterAddress(dto.getFooterAddress());
        }
        if (dto.getFooterEmail() != null) {
            settings.setFooterEmail(dto.getFooterEmail());
        }
        if (dto.getFooterPhone() != null) {
            settings.setFooterPhone(dto.getFooterPhone());
        }
        if (dto.getFooterWebsite() != null) {
            settings.setFooterWebsite(dto.getFooterWebsite());
        }
        
        SystemSettings updated = systemSettingsRepository.save(settings);
        log.info("System settings updated successfully");
        
        return mapToDTO(updated);
    }

    /**
     * Create default system settings
     */
    private SystemSettings createDefaultSettings() {
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
        
        return systemSettingsRepository.save(settings);
    }

    /**
     * Map entity to DTO
     */
    private SystemSettingsDTO mapToDTO(SystemSettings settings) {
        return SystemSettingsDTO.builder()
                .id(settings.getId())
                .logoUrl(settings.getLogoUrl())
                .logoHeader(settings.getLogoHeader())
                .logoSubheader(settings.getLogoSubheader())
                .stateName(settings.getStateName())
                .footerText(settings.getFooterText())
                .footerCopyright(settings.getFooterCopyright())
                .footerAddress(settings.getFooterAddress())
                .footerEmail(settings.getFooterEmail())
                .footerPhone(settings.getFooterPhone())
                .footerWebsite(settings.getFooterWebsite())
                .isActive(settings.getIsActive())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}

