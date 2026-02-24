-- Add READER Permission for SUBMIT_APPLICATION Transition
-- This allows READER to register/accept cases from CITIZEN_APPLICATION state

-- Step 1: Find the transition ID (for reference)
SELECT 
    t.id as transition_id,
    t.transition_code,
    t.transition_name,
    w.workflow_code,
    w.workflow_name
FROM workflow_transition t
JOIN workflow_definition w ON t.workflow_id = w.id
WHERE t.transition_code = 'SUBMIT_APPLICATION';

-- Step 2: Add READER permission for SUBMIT_APPLICATION transition
-- This will add permission for all workflows that have SUBMIT_APPLICATION transition
INSERT INTO workflow_permission (
    transition_id,
    role_code,
    unit_level,
    can_initiate,
    can_approve,
    hierarchy_rule,
    is_active,
    created_at
)
SELECT 
    t.id as transition_id,
    'READER' as role_code,
    NULL as unit_level,  -- NULL means all unit levels (DISTRICT, CIRCLE, etc.)
    true as can_initiate,
    false as can_approve,
    'SAME_UNIT' as hierarchy_rule,  -- READER can only register cases in their own unit/court
    true as is_active,
    NOW() as created_at
FROM workflow_transition t
JOIN workflow_definition w ON t.workflow_id = w.id
WHERE t.transition_code = 'SUBMIT_APPLICATION'
  AND NOT EXISTS (
      SELECT 1 FROM workflow_permission wp 
      WHERE wp.transition_id = t.id 
      AND wp.role_code = 'READER'
  );

-- Step 3: Verify the permission was added
SELECT 
    wp.id,
    wp.role_code,
    wp.unit_level,
    wp.can_initiate,
    wp.can_approve,
    wp.hierarchy_rule,
    wp.is_active,
    t.transition_code,
    t.transition_name,
    w.workflow_code
FROM workflow_permission wp
JOIN workflow_transition t ON wp.transition_id = t.id
JOIN workflow_definition w ON t.workflow_id = w.id
WHERE t.transition_code = 'SUBMIT_APPLICATION'
  AND wp.role_code = 'READER';
