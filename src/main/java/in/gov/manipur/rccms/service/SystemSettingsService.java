package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.SystemSettingsDTO;
import in.gov.manipur.rccms.dto.UpdateSystemSettingsDTO;
import in.gov.manipur.rccms.dto.WhatsNewDTO;
import in.gov.manipur.rccms.entity.SystemSettings;
import in.gov.manipur.rccms.entity.WhatsNew;
import in.gov.manipur.rccms.repository.SystemSettingsRepository;
import in.gov.manipur.rccms.repository.WhatsNewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    private final WhatsNewRepository whatsNewRepository;

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
        // Convert empty strings to null to allow clearing fields
        if (dto.getLogoUrl() != null) {
            String value = dto.getLogoUrl().trim();
            settings.setLogoUrl(value.isEmpty() ? null : value);
        }
        if (dto.getLogoHeader() != null) {
            String value = dto.getLogoHeader().trim();
            settings.setLogoHeader(value.isEmpty() ? null : value);
        }

        if (dto.getSecondaryLogoUrl() != null) {
            String value = dto.getSecondaryLogoUrl().trim();
            settings.setSecondaryLogoUrl(value.isEmpty() ? null : value);
        }
        if (dto.getSecondaryLogoHeader() != null) {
            String value = dto.getSecondaryLogoHeader().trim();
            settings.setSecondaryLogoHeader(value.isEmpty() ? null : value);
        }

        if (dto.getSecondaryLogoSubHeader() != null) {
            String value = dto.getSecondaryLogoSubHeader().trim();
            settings.setSecondaryLogoSubHeader(value.isEmpty() ? null : value);
        }

        if (dto.getTertiaryLogoUrl() != null) {
            String value = dto.getTertiaryLogoUrl().trim();
            settings.setTertiaryLogoUrl(value.isEmpty() ? null : value);
        }
        if (dto.getTertiaryLogoHeader() != null) {
            String value = dto.getTertiaryLogoHeader().trim();
            settings.setTertiaryLogoHeader(value.isEmpty() ? null : value);
        }

        if (dto.getTertiaryLogoSubHeader() != null) {
            String value = dto.getTertiaryLogoSubHeader().trim();
            settings.setTertiaryLogoSubHeader(value.isEmpty() ? null : value);
        }

        if (dto.getMarqueeText() != null) {
            String value = dto.getMarqueeText().trim();
            settings.setMarqueeText(value.isEmpty() ? null : value);
        }
        if (dto.getLogoSubheader() != null) {
            String value = dto.getLogoSubheader().trim();
            settings.setLogoSubheader(value.isEmpty() ? null : value);
        }
        if (dto.getStateName() != null) {
            String value = dto.getStateName().trim();
            settings.setStateName(value.isEmpty() ? null : value);
        }
        if (dto.getFooterText() != null) {
            String value = dto.getFooterText().trim();
            settings.setFooterText(value.isEmpty() ? null : value);
        }
        if (dto.getFooterCopyright() != null) {
            String value = dto.getFooterCopyright().trim();
            settings.setFooterCopyright(value.isEmpty() ? null : value);
        }
        if (dto.getFooterAddress() != null) {
            String value = dto.getFooterAddress().trim();
            settings.setFooterAddress(value.isEmpty() ? null : value);
        }
        if (dto.getFooterEmail() != null) {
            String value = dto.getFooterEmail().trim();
            settings.setFooterEmail(value.isEmpty() ? null : value);
        }
        if (dto.getFooterPhone() != null) {
            String value = dto.getFooterPhone().trim();
            settings.setFooterPhone(value.isEmpty() ? null : value);
        }
        if (dto.getFooterWebsite() != null) {
            String value = dto.getFooterWebsite().trim();
            settings.setFooterWebsite(value.isEmpty() ? null : value);
        }

        if (dto.getBanners() != null) {
            settings.setBanners(dto.getBanners());
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
                .secondaryLogoUrl(settings.getSecondaryLogoUrl())
                .secondaryLogoHeader(settings.getSecondaryLogoHeader())
                .secondaryLogoSubHeader(settings.getSecondaryLogoSubHeader())
                .tertiaryLogoUrl(settings.getTertiaryLogoUrl())
                .tertiaryLogoHeader(settings.getTertiaryLogoHeader())
                .tertiaryLogoSubHeader(settings.getTertiaryLogoSubHeader())
                .logoSubheader(settings.getLogoSubheader())
                .banners(settings.getBanners())
                .marqueeText(settings.getMarqueeText())
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

    public WhatsNewDTO createWhatsNew(List<WhatsNewDTO> dto) {

        WhatsNew whatsNew = new WhatsNew();
        if (dto != null) {
            whatsNew.setWhatsNew(dto);
            whatsNew.setCreatedOn(LocalDateTime.now());
            whatsNew.setUpdatedOn(LocalDateTime.now());
        } else
            throw new RuntimeException("invalid parameter provided");

        WhatsNew savedWhatsNew = whatsNewRepository.save(whatsNew);
        return new WhatsNewDTO(savedWhatsNew);


    }

    public WhatsNewDTO updateWhatsNew(Long id, List<WhatsNewDTO> dto) {

        WhatsNew existingWhatsNew = whatsNewRepository.findById(id).
                orElseThrow(() -> new RuntimeException("Data not found with id" + " " + id));

        WhatsNew whatsNew = new WhatsNew();

        if (dto != null) {
            existingWhatsNew.setWhatsNew(dto);
            existingWhatsNew.setCreatedOn(LocalDateTime.now());
            existingWhatsNew.setUpdatedOn(LocalDateTime.now());

        } else
            throw new RuntimeException("invalid parameter provided");


        WhatsNew updateWhatsNew = whatsNewRepository.save(existingWhatsNew);
        return new WhatsNewDTO(updateWhatsNew);


    }

    public List<WhatsNewDTO> fetchWhatsNewList() {

        List<WhatsNew> whatsNewList = whatsNewRepository.findAll();
        if (whatsNewList.isEmpty()) {
            throw new RuntimeException("List is empty" + " " + whatsNewList.size());
        } else {
            return whatsNewList.stream().map(WhatsNewDTO::new).toList();
        }
    }


    public WhatsNewDTO deleteWhatsNew(Long id) {

        whatsNewRepository.deleteById(id);
        return new WhatsNewDTO();
    }
}

