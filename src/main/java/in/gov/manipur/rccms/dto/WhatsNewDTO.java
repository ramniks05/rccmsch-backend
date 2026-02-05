package in.gov.manipur.rccms.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import in.gov.manipur.rccms.entity.WhatsNew;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsNewDTO {

    private Long id;
    private LocalDate publishedDate;
    private String title;
    private String pdfUrl;

    private List<WhatsNewDTO> whatsNewDTOS;
//
//    public WhatsNewDTO(WhatsNew savedWhatsNew) {
//        this.whatsNewDTOS = savedWhatsNew.getWhatsNew();
//    }
//
//    public WhatsNewDTO(WhatsNew savedWhatsNew) {
//        this.id= savedWhatsNew.getDocumentId();
//        this.whatsNewDTOS = savedWhatsNew.getWhatsNew();
//    }

    public WhatsNewDTO(WhatsNew entity) {

        this.id = entity.getDocumentId();

        if (entity.getWhatsNew() != null) {
            this.whatsNewDTOS = entity.getWhatsNew()
                    .stream()
                    .map(item -> {
                        WhatsNewDTO dto = new WhatsNewDTO();
                        dto.setPublishedDate(item.getPublishedDate());
                        dto.setTitle(item.getTitle());
                        dto.setPdfUrl(item.getPdfUrl());
                        return dto;
                    })
                    .toList();
        }
    }

}
