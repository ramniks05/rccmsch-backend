# State Integration Requirements - Overview Document

## Purpose
This document outlines the basic requirements and prerequisites for a state to integrate the RCCMS (Revenue Court Case Management System) software.

---

## 1. External API Requirements

### 1.1 ROR (Record of Rights) API
**Purpose:** To fetch land record details and verify land ownership information during case processing.

**Required Functionality:**
- Fetch land record details by survey number, khata number, or plot number
- Verify land ownership and mutation status
- Retrieve land classification and area details
- Validate land records before case approval

**Integration Points:**
- Case registration (citizen application)
- Mandol update stage (land record verification)
- Land record update stage (post-approval)

---

### 1.2 Master Court API
**Purpose:** To maintain and manage court master data for the state's administrative hierarchy.

**Required Data:**
- Court hierarchy (Circle, Sub-Division, District, State level courts)
- Court types (SDC Court, SDO Court, DC Court, Revenue Tribunal, etc.)
- Court mapping to administrative units
- Court designation and contact information

**Integration Points:**
- Case type configuration (which courts handle which case types)
- Case assignment and routing
- Available courts selection during case filing

---

## 2. Master Data Requirements

### 2.1 Administrative Units
**Hierarchy Structure:**
- State Level
- District Level
- Sub-Division Level
- Circle Level

**Required Information:**
- Unit codes (unique identifiers)
- LGD codes (Local Government Directory codes)
- Unit names
- Parent-child relationships
- Active/inactive status

---

### 2.2 Acts and Legal Framework
**Required Data:**
- Act codes and names
- Act year
- Relevant sections
- Description of acts applicable to revenue cases

**Examples:**
- Manipur Land Revenue and Land Reforms Act
- State-specific revenue acts
- Other applicable legal frameworks

---

### 2.3 Case Natures
**Purpose:** Define types of cases that can be filed in the system.

**Common Case Natures:**
- Mutation (after Gift/Sale Deeds)
- Mutation (after death of landowner)
- Partition (division of land parcel)
- Change in Classification of Land
- Implementation of Higher Court Order
- Allotment of Land
- Other state-specific case natures

**Required Information:**
- Case nature code (unique identifier)
- Case nature name
- Description
- Associated Act
- Workflow code (links to workflow definition)

---

### 2.4 Case Types
**Purpose:** Define the type of filing (New File, Appeal, Revision, etc.)

**Common Case Types:**
- New File
- First Appeal
- Second Appeal
- Revision
- Review

**Required Information:**
- Type code and name
- Court level (Circle, Sub-Division, District, State)
- Court types allowed
- Appeal order (for appeals)
- From level (for appeals - which level it's appealed from)

---

### 2.5 Courts Master
**Required Information:**
- Court code (unique identifier)
- Court name
- Court level (CIRCLE, SUB_DIVISION, DISTRICT, STATE)
- Court type (SDC_COURT, SDO_COURT, DC_COURT, REVENUE_TRIBUNAL, etc.)
- Administrative unit mapping
- Designation
- Address and contact details
- Active status

---

### 2.6 Roles and Officers
**System Roles:**
- SUPER_ADMIN
- STATE_ADMIN
- DISTRICT_OFFICER
- SUB_DIVISION_OFFICER
- CIRCLE_OFFICER
- CIRCLE_MANDOL
- DEALING_ASSISTANT

**Required Information:**
- Officer details (name, mobile, email)
- Role assignments
- Posting to administrative units
- User ID generation (format: ROLE_CODE@UNIT_LGD_CODE)

---

## 3. Workflow Configuration

### 3.1 Workflow Definition
**Purpose:** Define the business process flow for each case nature.

**Components:**
- Workflow states (e.g., CITIZEN_APPLICATION, DA_ENTRY, NOTICE_GENERATED, HEARING_SCHEDULED, etc.)
- Transitions between states
- Permissions (which roles can execute which transitions)
- Conditions (prerequisites for transitions)

**Required for Each Case Nature:**
- Complete workflow definition
- State definitions
- Transition definitions
- Permission configurations
- Condition rules

---

### 3.2 Module Forms
**Purpose:** Structured forms for different stages of case processing.

**Common Module Types:**
- HEARING (hearing scheduling form)
- NOTICE (notice generation form)
- ORDERSHEET (ordersheet form)
- JUDGEMENT (judgement form)

**Required Configuration:**
- Form schema definitions
- Field definitions and validations
- Field groups
- Data sources (dropdown options, etc.)

---

### 3.3 Document Templates
**Purpose:** Templates for generating official documents.

**Common Documents:**
- Notice documents
- Ordersheet documents
- Judgement documents
- Other case-specific documents

**Required:**
- Document templates
- Template variables
- Document status workflow (DRAFT → FINAL → SIGNED)

---

## 4. Registration Forms

### 4.1 Citizen Registration Form
**Required Fields:**
- Basic information (name, Aadhar, date of birth, etc.)
- Contact details (mobile, email, address)
- Location details
- Security (password)

---

### 4.2 Lawyer Registration Form
**Required Fields:**
- Basic information
- Bar council registration details
- Contact information
- Security credentials

---

## 5. System Configuration

### 5.1 System Settings
- Application name and branding
- SMS gateway configuration
- Email service configuration
- File upload limits
- Session timeout settings

---

### 5.2 Security Configuration
- JWT secret key
- Token expiration settings
- Password policies
- Encryption keys (for Aadhar, sensitive data)
- CORS configuration

---

## 6. Infrastructure Requirements

### 6.1 Database
- PostgreSQL (production) or H2 (development)
- Database schema will be auto-created
- Initial master data seeding required

---

### 6.2 Application Server
- Java 17 or higher
- Spring Boot 3.2.0
- Maven build tool
- Minimum server specifications as per state IT policy

---

### 6.3 External Services
- SMS Gateway (for OTP and notifications)
- Email Service (for notifications)
- File Storage (for document storage)
- ROR API endpoint access

---

## 7. Integration Checklist

### Pre-Integration
- [ ] Administrative unit hierarchy defined
- [ ] Court master data prepared
- [ ] Acts and legal framework documented
- [ ] Case natures identified and documented
- [ ] Case types defined for each case nature
- [ ] ROR API access configured
- [ ] SMS gateway credentials obtained
- [ ] Email service configured

### Master Data Setup
- [ ] Administrative units created
- [ ] Courts master data imported
- [ ] Acts created
- [ ] Case natures configured
- [ ] Case types configured
- [ ] Roles initialized
- [ ] Officers created and posted

### Workflow Configuration
- [ ] Workflow definitions created for each case nature
- [ ] States and transitions configured
- [ ] Permissions assigned to roles
- [ ] Conditions configured
- [ ] Module forms configured
- [ ] Document templates created

### System Configuration
- [ ] System settings configured
- [ ] Security keys generated
- [ ] Database configured
- [ ] External API endpoints configured
- [ ] Frontend URL configured (CORS)

---

## 8. Key Integration Points

### 8.1 Case Filing
- Citizen selects case nature and case type
- System fetches available courts based on case type and user's unit
- Citizen fills registration form (if not registered)
- Case is created and assigned to appropriate officer

### 8.2 Case Processing
- Officers process cases through workflow states
- Forms are submitted at various stages
- Documents are generated and finalized
- Transitions are executed based on permissions and conditions

### 8.3 Land Record Integration
- ROR API called during case registration (verification)
- ROR API called during Mandol update (land record check)
- ROR API called during land record update (post-approval mutation)

### 8.4 Notifications
- SMS notifications for OTP, case status updates
- Email notifications for important case events
- In-app notifications for officers

---

## 9. Support and Training Requirements

### 9.1 Technical Training
- System administration
- Master data management
- Workflow configuration
- User management

### 9.2 User Training
- Officer training (case processing, workflow navigation)
- Citizen training (case filing, status tracking)
- Admin training (system configuration)

---

## 10. Post-Integration

### 10.1 Testing
- End-to-end workflow testing
- Integration testing with ROR API
- User acceptance testing
- Performance testing

### 10.2 Go-Live
- Production deployment
- Data migration (if applicable)
- User onboarding
- Monitoring and support

---

## Notes

- This is an overview document. Detailed technical specifications are available in other documentation files.
- State-specific customizations may be required based on local rules and regulations.
- Regular updates to master data (courts, officers, etc.) will be needed as organizational changes occur.
- Workflow configurations may need adjustments based on actual business process requirements.

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-09  
**For:** State Integration Planning
