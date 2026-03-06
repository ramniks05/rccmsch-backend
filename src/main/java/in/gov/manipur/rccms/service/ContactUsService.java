package in.gov.manipur.rccms.service;


import in.gov.manipur.rccms.dto.ContactUsDTO;
import in.gov.manipur.rccms.entity.ContactUs;
import in.gov.manipur.rccms.repository.ContactUsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactUsService {

    private final ContactUsRepository contactUsRepository;

    public ContactUsDTO saveContact(ContactUsDTO dto) {
        ContactUs contact = new ContactUs();
        contact.setFullName(dto.getFullName());
        contact.setEmail(dto.getEmail());
        contact.setMobile(dto.getMobile());
        contact.setQueryCategory(dto.getQueryCategory());
        contact.setSubject(dto.getSubject());
        contact.setMessage(dto.getMessage());
        ContactUs savedContact = contactUsRepository.save(contact);

        return (new ContactUsDTO(savedContact));
    }
}
