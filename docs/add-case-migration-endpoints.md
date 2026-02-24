# How to Add Case Migration Endpoints to AdminController

Since AdminController was modified, here's how to add the migration endpoints:

## Add to AdminController.java

Add these imports:
```java
import in.gov.manipur.rccms.service.CaseMigrationService;
import java.util.List;
import java.util.Map;
```

Add CaseMigrationService to constructor:
```java
private final CaseMigrationService caseMigrationService;
```

Add these endpoints:

```java
/**
 * Initialize workflow instances for old cases that don't have one
 * POST /api/admin/cases/migrate/initialize-workflow-instances
 */
@Operation(summary = "Initialize Workflow Instances for Old Cases", 
           description = "Creates workflow instances for cases that don't have one. Useful when migrating old cases to use workflows.")
@PostMapping("/cases/migrate/initialize-workflow-instances")
public ResponseEntity<ApiResponse<Map<String, Object>>> initializeWorkflowInstancesForOldCases() {
    Map<String, Object> result = caseMigrationService.initializeWorkflowInstancesForOldCases();
    return ResponseEntity.ok(ApiResponse.success("Workflow instance initialization completed", result));
}

/**
 * Reset cases to initial state based on current workflow configuration
 * POST /api/admin/cases/migrate/reset-to-initial-state
 * Optional: Provide caseIds in request body to reset specific cases
 */
@Operation(summary = "Reset Cases to Initial State", 
           description = "Resets cases to the current initial state of their workflow. Use when initial state configuration has changed.")
@PostMapping("/cases/migrate/reset-to-initial-state")
public ResponseEntity<ApiResponse<Map<String, Object>>> resetCasesToInitialState(
        @RequestBody(required = false) List<Long> caseIds) {
    Map<String, Object> result = caseMigrationService.resetCasesToInitialState(caseIds);
    return ResponseEntity.ok(ApiResponse.success("Reset to initial state completed", result));
}
```
