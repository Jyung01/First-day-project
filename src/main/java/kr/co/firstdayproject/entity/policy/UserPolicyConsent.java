package kr.co.firstdayproject.entity.policy;

import jakarta.persistence.*;
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
 * 회원별 정책 동의 기록; 약관 버전 이력은 관리하지 않음
 * DB table: user_policy_consents
 */
@Entity
@Table(name = "user_policy_consents")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserPolicyConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_id", nullable = false)
    private Long consentId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "policy_id", nullable = false)
    private Long policyId;
    @Column(name = "consented", nullable = false)
    private Boolean consented;
    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
