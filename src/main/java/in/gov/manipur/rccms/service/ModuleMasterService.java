package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.entity.ModuleMaster;
import in.gov.manipur.rccms.repository.ModuleMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModuleMasterService {

    private static final String KIND_DOCUMENT = "DOCUMENT";
    private static final String KIND_BOTH = "BOTH";

    private final ModuleMasterRepository moduleMasterRepository;

    public String normalizeModuleCode(String moduleCode) {
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        return moduleCode.trim().toUpperCase();
    }

    public ModuleMaster requireActiveModule(String moduleCode) {
        String normalized = normalizeModuleCode(moduleCode);
        return moduleMasterRepository.findByCodeIgnoreCaseAndIsActiveTrue(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive module type: " + moduleCode));
    }

    public String requireActiveModuleCode(String moduleCode) {
        return requireActiveModule(moduleCode).getCode();
    }

    public String requireDocumentModuleCode(String moduleCode) {
        ModuleMaster module = requireActiveModule(moduleCode);
        String kind = module.getKind() == null ? "" : module.getKind().trim().toUpperCase();
        if (!KIND_DOCUMENT.equals(kind) && !KIND_BOTH.equals(kind)) {
            throw new IllegalArgumentException(
                    "Invalid document module type: " + moduleCode + ". Allowed kind: DOCUMENT or BOTH");
        }
        return module.getCode();
    }

    public List<ModuleMaster> getActiveModules() {
        return moduleMasterRepository.findByIsActiveTrueOrderByNameAsc();
    }
}
