package kr.co.firstdayproject.repository.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kr.co.firstdayproject.entity.policy.Policy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest(properties = {
        "spring.jpa.database=h2",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PolicyRepositoryTest {

    @Autowired
    private PolicyRepository policyRepository;

    @Test
    void findsOnlyRequiredAndOptionalSignupPolicies() {
        LocalDate today = LocalDate.of(2026, 8, 11);
        policyRepository.saveAll(List.of(
                policy("PERSONAL_REQUIRED", "개인 필수", "개인", "필수", 2, true, today),
                policy("COMMON_OPTIONAL", "공통 선택", "전체", "선택", 1, true, null),
                policy("PUBLIC_PRIVACY", "개인정보처리방침", "전체", "공개", 3, true, today),
                policy("CORPORATE_REQUIRED", "기업 필수", "기업", "필수", 4, true, today),
                policy("FUTURE_REQUIRED", "미래 약관", "개인", "필수", 5, true, today.plusDays(1)),
                policy("INACTIVE_REQUIRED", "비활성 약관", "개인", "필수", 6, false, today)
        ));

        List<Policy> result = policyRepository.findActiveSignupPolicies("개인", today);

        assertThat(result)
                .extracting(Policy::getPolicyCode)
                .containsExactly("COMMON_OPTIONAL", "PERSONAL_REQUIRED")
                .doesNotContain("PUBLIC_PRIVACY");
    }

    private Policy policy(
            String code,
            String title,
            String audience,
            String consentType,
            int displayOrder,
            boolean active,
            LocalDate effectiveFrom
    ) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 11, 12, 0);
        return Policy.builder()
                .policyCode(code)
                .title(title)
                .audience(audience)
                .consentType(consentType)
                .content(title + " 내용")
                .effectiveFrom(effectiveFrom)
                .displayOrder(displayOrder)
                .isActive(active)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
