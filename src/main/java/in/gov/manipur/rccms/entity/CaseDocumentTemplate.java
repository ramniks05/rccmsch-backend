package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Template for module documents (notice, ordersheet, judgement).
 * Templates are configured per case nature and module type.
 */
@Entity
@Table(name = "case_document_templates", indexes = {
        @Index(name = "idx_doc_template_case_nature", columnList = "case_nature_id"),
        @Index(name = "idx_doc_template_case_type", columnList = "case_type_id"),
        @Index(name = "idx_doc_template_module_type", columnList = "module_type"),
        @Index(name = "idx_doc_template_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseDocumentTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_nature_id", nullable = false, foreignKey = @ForeignKey(name = "fk_doc_template_case_nature"))
    private CaseNature caseNature;

    @Column(name = "case_nature_id", insertable = false, updatable = false)
    private Long caseNatureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id", foreignKey = @ForeignKey(name = "fk_doc_template_case_type"))
    private CaseType caseType;

    @Column(name = "case_type_id", insertable = false, updatable = false)
    private Long caseTypeId; // optional override per case type

    @Column(name = "module_type", nullable = false, length = 30)
    private String moduleType;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Column(name = "template_html", columnDefinition = "TEXT")
    private String templateHtml;

    @Column(name = "template_data", columnDefinition = "TEXT")
    private String templateData; // JSON: placeholders, editable fields

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "allow_edit_after_sign", nullable = false)
    private Boolean allowEditAfterSign = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
        if (allowEditAfterSign == null) {
            allowEditAfterSign = false;
        }
        if (version == null) {
            version = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

