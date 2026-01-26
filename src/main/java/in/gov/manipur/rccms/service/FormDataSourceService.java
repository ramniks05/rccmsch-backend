package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.AdminUnitDTO;
import in.gov.manipur.rccms.dto.CaseNatureDTO;
import in.gov.manipur.rccms.dto.CaseTypeDTO;
import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.Court;
import in.gov.manipur.rccms.entity.CourtLevel;
import in.gov.manipur.rccms.repository.AdminUnitRepository;
import in.gov.manipur.rccms.repository.CaseNatureRepository;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import in.gov.manipur.rccms.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Form Data Source Service
 * Provides data for dynamic dropdowns in forms
 * Supports ADMIN_UNITS, COURTS, ACTS, CASE_NATURES, CASE_TYPES, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FormDataSourceService {

    private final AdminUnitRepository adminUnitRepository;
    private final CourtRepository courtRepository;
    private final CaseNatureRepository caseNatureRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final ActService actService;

    /**
     * Get admin units by level and optional parent
     * Used for hierarchical dropdowns (State -> District -> Sub-Division -> Circle)
     */
    public List<Map<String, Object>> getAdminUnits(String level, Long parentId) {
        log.info("Getting admin units: level={}, parentId={}", level, parentId);

        try {
            CourtLevel courtLevel = CourtLevel.valueOf(level.toUpperCase());
            AdminUnit.UnitLevel unitLevel = convertCourtLevelToUnitLevel(courtLevel);

            List<AdminUnit> units;
            if (parentId != null) {
                // Filter by parent and level
                units = adminUnitRepository.findByParentUnitIdAndIsActiveTrue(parentId)
                        .stream()
                        .filter(unit -> unit.getUnitLevel() == unitLevel)
                        .collect(Collectors.toList());
            } else {
                units = adminUnitRepository.findByUnitLevelAndIsActiveTrue(unitLevel);
            }

            return units.stream()
                    .map(unit -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", unit.getUnitId());
                        map.put("code", unit.getUnitCode());
                        map.put("name", unit.getUnitName());
                        map.put("level", unit.getUnitLevel().toString());
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            log.error("Invalid level: {}", level);
            throw new IllegalArgumentException("Invalid admin unit level: " + level);
        }
    }

    /**
     * Get courts by level and optional unit
     */
    public List<Map<String, Object>> getCourts(String courtLevel, Long unitId) {
        log.info("Getting courts: level={}, unitId={}", courtLevel, unitId);

        try {
            CourtLevel level = CourtLevel.valueOf(courtLevel.toUpperCase());
            List<Court> courts;

            if (unitId != null) {
                courts = courtRepository.findByCourtLevelAndUnitIdAndIsActiveTrueOrderByCourtNameAsc(level, unitId);
            } else {
                courts = courtRepository.findByCourtLevelAndIsActiveTrueOrderByCourtNameAsc(level);
            }

            return courts.stream()
                    .map(court -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", court.getId());
                        map.put("code", court.getCourtCode());
                        map.put("name", court.getCourtName());
                        map.put("level", court.getCourtLevel().toString());
                        map.put("type", court.getCourtType().toString());
                        map.put("unitId", court.getUnitId() != null ? court.getUnitId() : 0L);
                        map.put("unitName", court.getUnit() != null ? court.getUnit().getUnitName() : "");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            log.error("Invalid court level: {}", courtLevel);
            throw new IllegalArgumentException("Invalid court level: " + courtLevel);
        }
    }

    /**
     * Get all active acts
     */
    public List<Map<String, Object>> getActs() {
        log.info("Getting all active acts");
        return actService.getActiveActs().stream()
                .map(act -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", act.getId());
                    map.put("code", act.getActCode());
                    map.put("name", act.getActName());
                    map.put("year", act.getActYear());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all active case natures
     */
    public List<Map<String, Object>> getCaseNatures() {
        log.info("Getting all active case natures");
        return caseNatureRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(cn -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", cn.getId());
                    map.put("code", cn.getCode());
                    map.put("name", cn.getName());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get case types by case nature
     */
    public List<Map<String, Object>> getCaseTypes(Long caseNatureId) {
        log.info("Getting case types for case nature: {}", caseNatureId);
        return caseTypeRepository.findByCaseNatureIdAndIsActiveTrueOrderByDisplayOrderAscTypeNameAsc(caseNatureId)
                .stream()
                .map(ct -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", ct.getId());
                    map.put("code", ct.getTypeCode());
                    map.put("name", ct.getTypeName());
                    map.put("caseNatureId", ct.getCaseNatureId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Convert CourtLevel to AdminUnit.UnitLevel
     */
    private AdminUnit.UnitLevel convertCourtLevelToUnitLevel(CourtLevel courtLevel) {
        switch (courtLevel) {
            case CIRCLE:
                return AdminUnit.UnitLevel.CIRCLE;
            case SUB_DIVISION:
                return AdminUnit.UnitLevel.SUB_DIVISION;
            case DISTRICT:
                return AdminUnit.UnitLevel.DISTRICT;
            case STATE:
                return AdminUnit.UnitLevel.STATE;
            default:
                throw new IllegalArgumentException("Unsupported court level: " + courtLevel);
        }
    }
}
