package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request body for POST /api/public/form-data-sources/external-api.
 * Frontend may send dataSource (string) and optionally runtimeParams (object) for dynamic query params.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiDataSourceRequestDTO {

    /** JSON string: type=API, apiConfigKey, dataEndpoint, valueField, labelField, queryParams, etc. */
    private String dataSource;

    /** Optional runtime query params (e.g. Nvcode, MUST). All keys are forwarded as query params with the same names. Supports multiple params for two-level dependency (e.g. GetOwnerDetailsByMustKhas: NVCODE + MUST). */
    private Map<String, Object> runtimeParams;
}
