package in.gov.manipur.rccms.entity;

import in.gov.manipur.rccms.dto.WhatsNewDTO;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "whats_new")
public class WhatsNew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "whats_new_id", nullable = false, unique = true)
    private Long whatsNewId;
    @Column(name = "updated_on", nullable = false)
    private LocalDateTime updatedOn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "whats_new_json", columnDefinition = "jsonb")
    private List<WhatsNewDTO> whatsNewJson;


}
