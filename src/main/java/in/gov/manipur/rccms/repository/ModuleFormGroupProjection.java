package in.gov.manipur.rccms.repository;

/**
 * Projection for distinct module forms (admin-created forms under any module).
 * One row per (caseNatureId, caseTypeId, moduleType) with id = min field id in that group.
 */
public interface ModuleFormGroupProjection {
    Long getId();
    Long getCaseNatureId();
    Long getCaseTypeId();
    String getModuleType();
    String getNatureName();
    String getTypeName();
}
