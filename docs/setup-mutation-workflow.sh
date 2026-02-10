#!/bin/bash

# Setup Mutation Gift/Sale Workflow Script
# This script creates the complete workflow configuration via API calls

API_URL="http://localhost:8080"
ADMIN_USERNAME="admin"
ADMIN_PASSWORD="admin@123"

echo "=========================================="
echo "Mutation Gift/Sale Workflow Setup"
echo "=========================================="
echo ""

# Step 1: Login as Admin
echo "Step 1: Logging in as admin..."
LOGIN_RESPONSE=$(curl -s -X POST "$API_URL/api/admin/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "❌ Error: Failed to login. Please check credentials."
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo "✅ Login successful"
echo "Token: ${TOKEN:0:30}..."
echo ""

# Step 2: Create Workflow
echo "Step 2: Creating workflow..."
WORKFLOW_RESPONSE=$(curl -s -X POST "$API_URL/api/admin/workflow/definitions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "workflowCode": "MUTATION_GIFT_SALE",
    "workflowName": "Mutation (after Gift/Sale Deeds)",
    "description": "Workflow for mutation after gift/sale deeds registration",
    "isActive": true
  }')

WORKFLOW_ID=$(echo $WORKFLOW_RESPONSE | jq -r '.data.id')

if [ "$WORKFLOW_ID" == "null" ] || [ -z "$WORKFLOW_ID" ]; then
  echo "❌ Error: Failed to create workflow"
  echo "Response: $WORKFLOW_RESPONSE"
  exit 1
fi

echo "✅ Workflow created with ID: $WORKFLOW_ID"
echo ""

# Step 3: Create States
echo "Step 3: Creating states..."

declare -A STATE_IDS

create_state() {
  local state_code=$1
  local state_name=$2
  local state_order=$3
  local is_initial=$4
  local is_final=$5
  local description=$6
  
  RESPONSE=$(curl -s -X POST "$API_URL/api/admin/workflow/$WORKFLOW_ID/states" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"stateCode\": \"$state_code\",
      \"stateName\": \"$state_name\",
      \"stateOrder\": $state_order,
      \"isInitialState\": $is_initial,
      \"isFinalState\": $is_final,
      \"description\": \"$description\"
    }")
  
  STATE_ID=$(echo $RESPONSE | jq -r '.data.id')
  STATE_IDS[$state_code]=$STATE_ID
  
  if [ "$STATE_ID" != "null" ] && [ ! -z "$STATE_ID" ]; then
    echo "  ✅ Created state: $state_name (ID: $STATE_ID)"
  else
    echo "  ❌ Failed to create state: $state_name"
    echo "  Response: $RESPONSE"
  fi
}

create_state "CITIZEN_APPLICATION" "Citizen Application" 1 true false "Landowner applies for mutation in Form no. 16"
create_state "DA_ENTRY" "DA Entry" 2 false false "Clerk makes an entry in the Mutation Register"
create_state "MANDOL_RECEIVED" "Mandol Received" 3 false false "Application passed to Circle Mandol"
create_state "NOTICE_GENERATED" "Notice Generated" 4 false false "Notice is sent to the parties concerned"
create_state "HEARING_SCHEDULED" "Hearing Scheduled" 5 false false "Date for hearing is fixed"
create_state "HEARING_COMPLETED" "Hearing Completed" 6 false false "SDC hears the parties and studies the documents"
create_state "DECISION_PENDING" "Decision Pending" 7 false false "SDC decision pending"
create_state "APPROVED" "Approved" 8 false false "SDC approves and passes a Mutation Order"
create_state "MANDOL_UPDATE" "Mandol Update" 9 false false "SDC hands over the order to the Circle Mandol"
create_state "LAND_RECORD_UPDATED" "Land Record Updated" 10 false false "Circle Mandol updates the land record"
create_state "PATTA_PREPARATION" "Patta Preparation" 11 false false "Landowner gets informed regarding preparation of new patta"
create_state "COMPLETED" "Completed" 12 false true "Case completed successfully"
create_state "REJECTED" "Rejected" 13 false true "SDC rejects the application"

echo ""
echo "✅ All states created"
echo ""

# Step 4: Create Transitions
echo "Step 4: Creating transitions..."

declare -A TRANSITION_IDS

create_transition() {
  local transition_code=$1
  local transition_name=$2
  local from_state_code=$3
  local to_state_code=$4
  local requires_comment=$5
  local description=$6
  
  FROM_STATE_ID=${STATE_IDS[$from_state_code]}
  TO_STATE_ID=${STATE_IDS[$to_state_code]}
  
  if [ -z "$FROM_STATE_ID" ] || [ -z "$TO_STATE_ID" ]; then
    echo "  ❌ Failed: State IDs not found for $transition_code"
    return
  fi
  
  RESPONSE=$(curl -s -X POST "$API_URL/api/admin/workflow/$WORKFLOW_ID/transitions" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"transitionCode\": \"$transition_code\",
      \"transitionName\": \"$transition_name\",
      \"fromStateId\": $FROM_STATE_ID,
      \"toStateId\": $TO_STATE_ID,
      \"requiresComment\": $requires_comment,
      \"isActive\": true,
      \"description\": \"$description\"
    }")
  
  TRANSITION_ID=$(echo $RESPONSE | jq -r '.data.id')
  TRANSITION_IDS[$transition_code]=$TRANSITION_ID
  
  if [ "$TRANSITION_ID" != "null" ] && [ ! -z "$TRANSITION_ID" ]; then
    echo "  ✅ Created transition: $transition_name (ID: $TRANSITION_ID)"
  else
    echo "  ❌ Failed to create transition: $transition_name"
    echo "  Response: $RESPONSE"
  fi
}

create_transition "SUBMIT_APPLICATION" "Submit Application" "CITIZEN_APPLICATION" "DA_ENTRY" false "Landowner submits mutation application"
create_transition "ENTER_IN_REGISTER" "Enter in Register" "DA_ENTRY" "MANDOL_RECEIVED" false "Clerk makes an entry in the Mutation Register"
create_transition "RECEIVE_BY_MANDOL" "Receive by Mandol" "MANDOL_RECEIVED" "NOTICE_GENERATED" false "Circle Mandol receives the application"
create_transition "GENERATE_NOTICE" "Generate Notice" "NOTICE_GENERATED" "HEARING_SCHEDULED" false "Notice is sent to the parties concerned"
create_transition "SCHEDULE_HEARING" "Schedule Hearing" "HEARING_SCHEDULED" "HEARING_COMPLETED" false "Date for hearing is fixed"
create_transition "COMPLETE_HEARING" "Complete Hearing" "HEARING_COMPLETED" "DECISION_PENDING" true "SDC hears the parties and studies the documents"
create_transition "APPROVE" "Approve" "DECISION_PENDING" "APPROVED" true "SDC approves the application"
create_transition "REJECT" "Reject" "DECISION_PENDING" "REJECTED" true "SDC rejects the application"
create_transition "PASS_TO_MANDOL" "Pass to Mandol" "APPROVED" "MANDOL_UPDATE" false "SDC hands over the order to the Circle Mandol"
create_transition "UPDATE_LAND_RECORD" "Update Land Record" "MANDOL_UPDATE" "LAND_RECORD_UPDATED" false "Circle Mandol updates the land record"
create_transition "PREPARE_PATTA" "Prepare Patta" "LAND_RECORD_UPDATED" "PATTA_PREPARATION" false "Inform landowner regarding preparation of new patta"
create_transition "COMPLETE" "Complete" "PATTA_PREPARATION" "COMPLETED" false "Case completed successfully"

echo ""
echo "✅ All transitions created"
echo ""

# Step 5: Create Permissions
echo "Step 5: Creating permissions..."

create_permission() {
  local transition_code=$1
  local role_code=$2
  local unit_level=$3
  local can_initiate=$4
  local can_approve=$5
  local hierarchy_rule=$6
  
  TRANSITION_ID=${TRANSITION_IDS[$transition_code]}
  
  if [ -z "$TRANSITION_ID" ]; then
    echo "  ❌ Failed: Transition ID not found for $transition_code"
    return
  fi
  
  if [ "$unit_level" == "null" ]; then
    UNIT_LEVEL_JSON="null"
  else
    UNIT_LEVEL_JSON="\"$unit_level\""
  fi
  
  RESPONSE=$(curl -s -X POST "$API_URL/api/admin/workflow/transitions/$TRANSITION_ID/permissions" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"roleCode\": \"$role_code\",
      \"unitLevel\": $UNIT_LEVEL_JSON,
      \"canInitiate\": $can_initiate,
      \"canApprove\": $can_approve,
      \"hierarchyRule\": \"$hierarchy_rule\",
      \"isActive\": true
    }")
  
  PERMISSION_ID=$(echo $RESPONSE | jq -r '.data.id')
  
  if [ "$PERMISSION_ID" != "null" ] && [ ! -z "$PERMISSION_ID" ]; then
    echo "  ✅ Created permission: $transition_code -> $role_code (ID: $PERMISSION_ID)"
  else
    echo "  ❌ Failed to create permission: $transition_code -> $role_code"
    echo "  Response: $RESPONSE"
  fi
}

create_permission "SUBMIT_APPLICATION" "CITIZEN" "null" true false "ANY_UNIT"
create_permission "ENTER_IN_REGISTER" "DEALING_ASSISTANT" "CIRCLE" true false "SAME_UNIT"
create_permission "RECEIVE_BY_MANDOL" "CIRCLE_MANDOL" "CIRCLE" true false "SAME_UNIT"
create_permission "GENERATE_NOTICE" "CIRCLE_MANDOL" "CIRCLE" true false "SAME_UNIT"
create_permission "SCHEDULE_HEARING" "CIRCLE_OFFICER" "CIRCLE" true false "SAME_UNIT"
create_permission "COMPLETE_HEARING" "CIRCLE_OFFICER" "CIRCLE" true false "SAME_UNIT"
create_permission "APPROVE" "CIRCLE_OFFICER" "CIRCLE" true true "SAME_UNIT"
create_permission "REJECT" "CIRCLE_OFFICER" "CIRCLE" true true "SAME_UNIT"
create_permission "PASS_TO_MANDOL" "CIRCLE_OFFICER" "CIRCLE" true false "SAME_UNIT"
create_permission "UPDATE_LAND_RECORD" "CIRCLE_MANDOL" "CIRCLE" true false "SAME_UNIT"
create_permission "PREPARE_PATTA" "DEALING_ASSISTANT" "CIRCLE" true false "SAME_UNIT"
create_permission "COMPLETE" "DEALING_ASSISTANT" "CIRCLE" true false "SAME_UNIT"

echo ""
echo "✅ All permissions created"
echo ""

# Summary
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo "Workflow ID: $WORKFLOW_ID"
echo "Workflow Code: MUTATION_GIFT_SALE"
echo ""
echo "States Created: 13"
echo "Transitions Created: 12"
echo "Permissions Created: 12"
echo ""
echo "You can now verify the workflow at:"
echo "GET $API_URL/api/admin/workflow/$WORKFLOW_ID/states"
echo "GET $API_URL/api/admin/workflow/$WORKFLOW_ID/transitions/all"
echo ""
