package in.gov.manipur.rccms.config;

import in.gov.manipur.rccms.entity.RegistrationFieldGroup;
import in.gov.manipur.rccms.entity.RegistrationFormField;
import in.gov.manipur.rccms.repository.RegistrationFieldGroupRepository;
import in.gov.manipur.rccms.repository.RegistrationFormFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/**
 * Initializes default registration form schema (Citizen & Lawyer)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationFormDataInitializer implements CommandLineRunner {

    private final RegistrationFormFieldRepository fieldRepository;
    private final RegistrationFieldGroupRepository groupRepository;

    @Override
    public void run(String... args) {
        initGroups(RegistrationFormField.RegistrationType.CITIZEN);
        initGroups(RegistrationFormField.RegistrationType.LAWYER);
        initFields(RegistrationFormField.RegistrationType.CITIZEN);
        initFields(RegistrationFormField.RegistrationType.LAWYER);
    }

    private void initGroups(RegistrationFormField.RegistrationType type) {
        createGroupIfNotExists(type, "basic", "Basic Info", "Basic identity details", 1);
        createGroupIfNotExists(type, "contact", "Contact", "Contact details", 2);
        createGroupIfNotExists(type, "security", "Security", "Password and security", 3);
        createGroupIfNotExists(type, "location", "Location", "Location details", 4);
    }

    private void initFields(RegistrationFormField.RegistrationType type) {
        // Basic
        createFieldIfNotExists(type, "firstName", "First Name", "TEXT", true, 1,
                "{\"minLength\":2,\"maxLength\":50}", "basic");
        createFieldIfNotExists(type, "lastName", "Last Name", "TEXT", true, 2,
                "{\"minLength\":2,\"maxLength\":50}", "basic");

        // Contact
        createFieldIfNotExists(type, "email", "Email", "EMAIL", true, 3,
                "{\"pattern\":\"^[A-Za-z0-9+_.-]+@(.+)$\"}", "contact");
        createFieldIfNotExists(type, "mobileNumber", "Mobile Number", "PHONE", true, 4,
                "{\"pattern\":\"^[6-9]\\\\d{9}$\"}", "contact");

        // Security
        createFieldIfNotExists(type, "password", "Password", "PASSWORD", true, 5,
                "{\"minLength\":8}", "security");
        createFieldIfNotExists(type, "confirmPassword", "Confirm Password", "PASSWORD", true, 6,
                null, "security");

        // Location (optional - can be disabled by admin)
        createFieldIfNotExists(type, "unitId", "Select Unit", "DROPDOWN", false, 7,
                null, "location", "{\"type\":\"ADMIN_UNITS\",\"level\":\"CIRCLE\"}");
        ensureFieldNotRequired(type, "unitId");
    }

    private void createGroupIfNotExists(RegistrationFormField.RegistrationType type, String code, String label, String description, int order) {
        if (groupRepository.findByRegistrationTypeAndGroupCode(type, code).isPresent()) {
            return;
        }
        RegistrationFieldGroup group = new RegistrationFieldGroup();
        group.setRegistrationType(type);
        group.setGroupCode(code);
        group.setGroupLabel(label);
        group.setDescription(description);
        group.setDisplayOrder(order);
        group.setIsActive(true);
        groupRepository.save(group);
        log.info("Created registration group: {} for {}", code, type);
    }

    private void createFieldIfNotExists(RegistrationFormField.RegistrationType type, String name, String label,
                                        String fieldType, boolean required, int order, String rules, String group) {
        createFieldIfNotExists(type, name, label, fieldType, required, order, rules, group, null);
    }

    private void createFieldIfNotExists(RegistrationFormField.RegistrationType type, String name, String label,
                                        String fieldType, boolean required, int order, String rules,
                                        String group, String dataSource) {
        if (fieldRepository.findByRegistrationTypeAndFieldName(type, name).isPresent()) {
            return;
        }
        RegistrationFormField field = new RegistrationFormField();
        field.setRegistrationType(type);
        field.setFieldName(name);
        field.setFieldLabel(label);
        field.setFieldType(fieldType);
        field.setIsRequired(required);
        field.setDisplayOrder(order);
        field.setValidationRules(rules);
        field.setFieldGroup(group);
        field.setDataSource(dataSource);
        field.setIsActive(true);
        fieldRepository.save(field);
        log.info("Created registration field: {} for {}", name, type);
    }

    private void ensureFieldNotRequired(RegistrationFormField.RegistrationType type, String name) {
        fieldRepository.findByRegistrationTypeAndFieldName(type, name).ifPresent(field -> {
            if (Boolean.TRUE.equals(field.getIsRequired())) {
                field.setIsRequired(false);
                fieldRepository.save(field);
                log.info("Updated registration field to optional: {} for {}", name, type);
            }
        });
    }
}
