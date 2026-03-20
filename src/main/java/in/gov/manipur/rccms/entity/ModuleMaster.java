package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "module_master", indexes = {
        @Index(name = "idx_module_master_code", columnList = "code", unique = true),
        @Index(name = "idx_module_master_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 60, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "kind", nullable = false, length = 20)
    private String kind; // FORM, DOCUMENT, BOTH

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "allows_file_upload", nullable = false)
    private Boolean allowsFileUpload = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (allowsFileUpload == null) {
            allowsFileUpload = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
