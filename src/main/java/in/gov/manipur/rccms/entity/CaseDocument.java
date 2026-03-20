package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generated document for a case (notice, ordersheet, judgement).
 */
@Entity
@Table(name = "case_documents", indexes = {
        @Index(name = "idx_doc_case", columnList = "case_id"),
        @Index(name = "idx_doc_module_type", columnList = "module_type"),
        @Index(name = "idx_doc_template", columnList = "template_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false, foreignKey = @ForeignKey(name = "fk_doc_case"))
    private Case caseEntity;

    @Column(name = "case_id", insertable = false, updatable = false)
    private Long caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_nature_id", nullable = false, foreignKey = @ForeignKey(name = "fk_doc_case_nature"))
    private CaseNature caseNature;

    @Column(name = "case_nature_id", insertable = false, updatable = false)
    private Long caseNatureId;

    @Column(name = "module_type", nullable = false, length = 30)
    private String moduleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", foreignKey = @ForeignKey(name = "fk_doc_template"))
    private CaseDocumentTemplate template;

    @Column(name = "template_id", insertable = false, updatable = false)
    private Long templateId;

    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "content_data", columnDefinition = "TEXT")
    private String contentData; // JSON with editable fields

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @Column(name = "signed_by_officer_id")
    private Long signedByOfficerId;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = DocumentStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

