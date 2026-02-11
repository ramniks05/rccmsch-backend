package in.gov.manipur.rccms.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import in.gov.manipur.rccms.entity.WhatsNew;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsNewDTO {

    private Long whatsNewId;
    private Integer itemId;
    private LocalDateTime publishedDate;
    private String title;
    private String pdfUrl;

    private List<WhatsNewDTO> whatsNewDTOS;

    public WhatsNewDTO(WhatsNew entity) {

        if (entity.getWhatsNewJson() != null) {
            this.whatsNewDTOS = entity.getWhatsNewJson()
                    .stream()
                    .map(item -> {
                        WhatsNewDTO dto = new WhatsNewDTO();
                        dto.setWhatsNewId(entity.getWhatsNewId());
                        dto.setItemId(item.getItemId());
                        dto.setPublishedDate(item.getPublishedDate());
                        dto.setTitle(item.getTitle());
                        dto.setPdfUrl(item.getPdfUrl());
                        return dto;
                    })
                    .toList();
        }
    }

}
