package in.gov.manipur.rccms.controller;


import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.ContactUsDTO;
import in.gov.manipur.rccms.service.ContactUsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contactUs")
public class ContactUsController {

    private final ContactUsService contactUsService;


    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ContactUsDTO>> submitContact(@RequestBody ContactUsDTO contactUsDTO) {

        ContactUsDTO savedContactUsDTO = contactUsService.saveContact(contactUsDTO);

        return ResponseEntity.ok(ApiResponse.success("Saved successfully", savedContactUsDTO));
    }
}
