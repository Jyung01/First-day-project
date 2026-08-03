package kr.co.firstdayproject.entity.company;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 기업 공개정보; 가입 승인 상태와 이용정지·탈퇴 운영 상태를 분리
 * DB table: companies
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "business_number", nullable = false, length = 20)
    private String businessNumber;
    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;
    @Column(name = "representative_name", length = 100)
    private String representativeName;
    @Column(name = "established_date")
    private LocalDate establishedDate;
    @Column(name = "industry_name", length = 100)
    private String industryName;
    @Column(name = "company_size", length = 50)
    private String companySize;
    @Column(name = "homepage_url", length = 1000)
    private String homepageUrl;
    @Column(name = "logo_url", length = 1000)
    private String logoUrl;
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    @Column(name = "address_line1", length = 255)
    private String addressLine1;
    @Column(name = "address_line2", length = 255)
    private String addressLine2;
    @Column(name = "short_description", length = 300)
    private String shortDescription;
    @Column(name = "introduction", columnDefinition = "LONGTEXT")
    private String introduction;
    @Column(name = "benefits", columnDefinition = "LONGTEXT")
    private String benefits;
    @Column(name = "approval_status", nullable = false, length = 10)
    private String approvalStatus;
    @Column(name = "company_status", nullable = false, length = 10)
    private String companyStatus;
    @Column(name = "latest_rejection_reason", length = 1000)
    private String latestRejectionReason;
    @Column(name = "reviewed_by")
    private Long reviewedBy;
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    @Column(name = "reapply_requested_at")
    private LocalDateTime reapplyRequestedAt;
    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;
}
