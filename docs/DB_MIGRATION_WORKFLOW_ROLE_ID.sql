-- Migration: Add role_id (role_master FK) to workflow permission and case workflow instance
-- Run after role_master is populated. Backfill role_id from role_code where possible.

-- 1. workflow_permission: add role_id (nullable first, backfill from role_master, then use for lookups)
ALTER TABLE workflow_permission
  ADD COLUMN IF NOT EXISTS role_id BIGINT NULL,
  ADD CONSTRAINT fk_permission_role FOREIGN KEY (role_id) REFERENCES role_master(id);

UPDATE workflow_permission wp
SET role_id = (SELECT id FROM role_master rm WHERE rm.role_code = wp.role_code LIMIT 1)
WHERE wp.role_id IS NULL AND wp.role_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_workflow_permission_role_id ON workflow_permission(role_id);

-- 2. case_workflow_instance: add assigned_to_role_id (nullable)
ALTER TABLE case_workflow_instance
  ADD COLUMN IF NOT EXISTS assigned_to_role_id BIGINT NULL,
  ADD CONSTRAINT fk_instance_assigned_role FOREIGN KEY (assigned_to_role_id) REFERENCES role_master(id);

UPDATE case_workflow_instance cwi
SET assigned_to_role_id = (SELECT id FROM role_master rm WHERE rm.role_code = cwi.assigned_to_role LIMIT 1)
WHERE cwi.assigned_to_role_id IS NULL AND cwi.assigned_to_role IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_case_workflow_instance_assigned_role_id ON case_workflow_instance(assigned_to_role_id);
