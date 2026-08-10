package kr.co.firstdayproject.entity.policy;

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
 * 회원가입 동의 및 공개 정책; 별도 버전 이력은 관리하지 않음
 * DB table: policies
 */
@Entity
@Table(name = "policies")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "policy_code", nullable = false, unique = true, length = 50)
    private String policyCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "audience", length = 10)
    private String audience; // 개인 / 기업 / 전체

    @Column(name = "consent_type", length = 10)
    private String consentType; // 필수 / 선택 / 공개

    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}