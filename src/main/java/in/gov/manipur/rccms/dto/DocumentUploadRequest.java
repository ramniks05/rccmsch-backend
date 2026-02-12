package in.gov.manipur.rccms.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
public class DocumentUploadRequest {

    private String title;
    private String publishedOn;
    private String url;           // optional external link
    private MultipartFile file;
}
