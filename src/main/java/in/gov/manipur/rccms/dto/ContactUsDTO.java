package in.gov.manipur.rccms.dto;


import in.gov.manipur.rccms.entity.ContactUs;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactUsDTO {

    private String fullName;
    private String email;
    private String mobile;
    private Integer queryCategory;
    private String subject;
    private String message;


    public ContactUsDTO(ContactUs contact) {
        if (contact != null) {
            this.fullName = contact.getFullName() != null ? contact.getFullName() : "NA";
            this.email = contact.getEmail() != null ? contact.getEmail() : "NA";
            this.mobile = contact.getMobile() != null ? contact.getMobile() : "NA";
            this.queryCategory = contact.getQueryCategory() != null ? contact.getQueryCategory() : null;
            this.subject = contact.getSubject() != null ? contact.getSubject() : "NA";
            this.message = contact.getMessage() != null ? contact.getMessage() : "Na";
        }
    }

}
