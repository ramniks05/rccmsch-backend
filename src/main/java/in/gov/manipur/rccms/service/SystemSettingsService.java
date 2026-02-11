package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.DocumentsAvailable;
import in.gov.manipur.rccms.entity.SystemSettings;
import in.gov.manipur.rccms.entity.WhatsNew;
import in.gov.manipur.rccms.repository.DocumentAvailableRepository;
import in.gov.manipur.rccms.repository.SystemSettingsRepository;
import in.gov.manipur.rccms.repository.WhatsNewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    private final DocumentAvailableRepository documentAvailableRepository;
    private final ObjectMapper mapper;

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

    public List<WhatsNewDTO> createWhatsNew(JsonNode dto) {

        List<WhatsNewDTO> list = new ArrayList<>();

        // Step 1: find max existing itemId
        int maxId = whatsNewRepository.findAll()
                .stream()
                .filter(e -> e.getWhatsNewJson() != null)
                .flatMap(e -> e.getWhatsNewJson().stream())
                .mapToInt(WhatsNewDTO::getItemId)
                .max()
                .orElse(0);   // if no data → start from 0

        AtomicInteger counter = new AtomicInteger(maxId + 1);

        if (dto.isArray()) {
            list = mapper.convertValue(dto, new TypeReference<>() {
            });
        } else {

            list.add(mapper.convertValue(dto, WhatsNewDTO.class));
        }

        WhatsNew whatsNew = new WhatsNew();

        // Step 2: assign new incremental ids
        list.forEach(e -> e.setItemId(counter.getAndIncrement()));
        whatsNew.setWhatsNewJson(list);
        whatsNew.setUpdatedOn(LocalDateTime.now());
        WhatsNew savedWhatsNew = whatsNewRepository.save(whatsNew);
        return new WhatsNewDTO(savedWhatsNew).getWhatsNewDTOS();

    }

    public WhatsNewDTO updateWhatsNew(Long whatsNewId, Integer itemId, WhatsNewDTO dto) {

        WhatsNew existingWhatsNew = whatsNewRepository.findById(whatsNewId).
                orElseThrow(() -> new RuntimeException("Data not found with id" + " " + whatsNewId));

        List<WhatsNewDTO> updatedList = existingWhatsNew.getWhatsNewJson()
                .stream()
                .map(item -> {
                    if (item.getItemId().equals(itemId)) {
                        item.setTitle(dto.getTitle());
                        item.setPdfUrl(dto.getPdfUrl());
                        item.setPublishedDate(dto.getPublishedDate());
                    }
                    return item;
                })
                .toList();

        existingWhatsNew.setUpdatedOn(LocalDateTime.now());
        existingWhatsNew.setWhatsNewJson(updatedList);

        WhatsNew updateWhatsNew = whatsNewRepository.save(existingWhatsNew);
        return new WhatsNewDTO(updateWhatsNew);
    }

    public List<WhatsNewDTO> fetchWhatsNewList() {

        return whatsNewRepository.findAll()
                .stream()
                .flatMap(entity ->
                        entity.getWhatsNewJson()
                                .stream()
                                .map(item -> {
                                    WhatsNewDTO dto = new WhatsNewDTO();

                                    dto.setWhatsNewId(entity.getWhatsNewId()); // ⭐ parent id
                                    dto.setItemId(item.getItemId());
                                    dto.setTitle(item.getTitle());
                                    dto.setPdfUrl(item.getPdfUrl());
                                    dto.setPublishedDate(item.getPublishedDate());

                                    return dto;
                                })
                )
                .toList();
    }

    public WhatsNewDTO deleteWhatsNew(Long whatsNewId, Integer itemId) {

        WhatsNew entity = whatsNewRepository.findById(whatsNewId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        // delete whole whatsNew batch
        if (itemId == null) {
            whatsNewRepository.delete(entity);
            return new WhatsNewDTO();
        }

        // delete only one item
        List<WhatsNewDTO> updated = entity.getWhatsNewJson();

        updated.removeIf(item -> item.getItemId().equals(itemId));

        entity.setWhatsNewJson(updated);

        return new WhatsNewDTO(whatsNewRepository.save(entity));

    }

    public DocumentAvailableDTO uploadAvailableDocument(DocumentUploadRequest request) {

        try {

            String filePath;

            MultipartFile file = request.getFile();

            // ✅ Case 1 → File uploaded
            if (file != null && !file.isEmpty()) {

                String folder = "uploads/documents/";
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

                Path path = Paths.get(folder + fileName);

                Files.createDirectories(path.getParent());
                Files.write(path, file.getBytes());

                filePath = "/uploads/documents/" + fileName;
            }

            // ✅ Case 2 → Only URL provided
            else if (request.getFilePath() != null && !request.getFilePath().isBlank()) {
                filePath = request.getFilePath();
            } else {
                throw new IllegalArgumentException("Either file or URL must be provided");
            }

            DocumentsAvailable doc = new DocumentsAvailable();
            doc.setTitle(request.getTitle());
            doc.setFilePath(filePath);
            doc.setPublishedOn(LocalDate.now());

            DocumentsAvailable saved = documentAvailableRepository.save(doc);

            return new DocumentAvailableDTO(saved);

        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }

    public List<DocumentAvailableDTO> fetchDocumentList() {

        return documentAvailableRepository.findAll()
                .stream()
                .map(DocumentAvailableDTO::new)
                .toList();
    }

    public DocumentAvailableDTO deleteAvailableDocument(Long documentId) {
        DocumentsAvailable doc = documentAvailableRepository.findById(documentId)
                .orElseThrow();

        Path path = Paths.get("uploads/documents/" + doc.getTitle());

        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
        documentAvailableRepository.delete(doc);
        return new DocumentAvailableDTO();
    }
}


