package in.gov.manipur.rccms.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.entity.Citizen;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class CitizenProfileUpdateDTO {

    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name must contain only letters")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Last name must contain only letters")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be 10 digits starting with 6-9")
    private String mobileNumber;

    /**
     * Dynamic fields configured by admin
     */
    @JsonIgnore
    private Map<String, Object> extraFields = new HashMap<>();

    /**
     * Capture unknown JSON fields
     */
    @JsonAnySetter
    public void addExtraField(String key, Object value) {
        extraFields.put(key, value);
    }

    /**
     * Return dynamic fields in response
     */
    @JsonAnyGetter
    public Map<String, Object> getExtraFields() {
        return extraFields;
    }

    /**
     * Constructor for Entity → DTO mapping
     */
    public CitizenProfileUpdateDTO(Citizen citizen, ObjectMapper objectMapper) {
        this.firstName = citizen.getFirstName();
        this.lastName = citizen.getLastName();
        this.email = citizen.getEmail();
        this.mobileNumber = citizen.getMobileNumber();

        if (citizen.getRegistrationData() != null) {
            try {
                this.extraFields = objectMapper.readValue(
                        citizen.getRegistrationData(),
                        Map.class
                );
            } catch (Exception e) {
                this.extraFields = new HashMap<>();
            }
        }
    }
}