package in.gov.manipur.rccms.entity;

import in.gov.manipur.rccms.Constants.Enums.EventType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "calender_events")
@Data
public class CalenderEvent {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", nullable = false, unique = true)
    private Long eventId;

    @Column(name = "event_title", nullable = false)
    private String title;
    @Column(name = "event_description", nullable = false)
    private String description;
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
    @Column(name = "event_date", nullable = false)
    private LocalDate date;
    @Column(name = "financial_year", nullable = false)
    private String financialYear;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdDate;
    @Column(name = "updated_on", nullable = false)
    private LocalDateTime updatedDate;

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = true;
        }
    }


}
