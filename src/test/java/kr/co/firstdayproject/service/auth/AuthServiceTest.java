package kr.co.firstdayproject.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import kr.co.firstdayproject.dto.auth.CorporateTermsAgreement;
import kr.co.firstdayproject.dto.auth.PersonalTermsAgreement;
import kr.co.firstdayproject.entity.policy.Policy;
import kr.co.firstdayproject.repository.policy.PolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void acceptsRequiredAndOptionalPersonalPolicies() {
        when(policyRepository.findActiveSignupPolicies(any(), any()))
                .thenReturn(personalPolicies());

        Optional<PersonalTermsAgreement> result =
                authService.validatePersonalTermsAgreement(Set.of(1L, 2L, 3L, 4L));

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().agreedPolicyIds())
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(result.orElseThrow().displayedPolicyIds())
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }

    @Test
    void rejectsAgreementWhenRequiredPolicyIsMissing() {
        when(policyRepository.findActiveSignupPolicies(any(), any()))
                .thenReturn(personalPolicies());

        Optional<PersonalTermsAgreement> result =
                authService.validatePersonalTermsAgreement(Set.of(1L, 3L, 4L));

        assertThat(result).isEmpty();
    }

    @Test
    void removesPolicyIdsThatWereNotDisplayed() {
        when(policyRepository.findActiveSignupPolicies(any(), any()))
                .thenReturn(personalPolicies());

        PersonalTermsAgreement result = authService
                .validatePersonalTermsAgreement(Set.of(1L, 2L, 999L))
                .orElseThrow();

        assertThat(result.agreedPolicyIds())
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void acceptsRequiredCorporatePolicies() {
        when(policyRepository.findActiveSignupPolicies(any(), any()))
                .thenReturn(corporatePolicies());

        Optional<CorporateTermsAgreement> result =
                authService.validateCorporateTermsAgreement(Set.of(10L, 11L, 12L));

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().agreedPolicyIds())
                .containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void rejectsCorporateAgreementWhenRequiredPolicyIsMissing() {
        when(policyRepository.findActiveSignupPolicies(any(), any()))
                .thenReturn(corporatePolicies());

        Optional<CorporateTermsAgreement> result =
                authService.validateCorporateTermsAgreement(Set.of(10L, 12L));

        assertThat(result).isEmpty();
    }

    @Test
    void listsRequiredCorporatePoliciesBeforeOptionalPolicies() {
        when(policyRepository.findActiveSignupPolicies(any(), any()))
                .thenReturn(List.of(
                        policy(12L, "선택"),
                        policy(10L, "필수"),
                        policy(13L, "선택"),
                        policy(11L, "필수")
                ));

        List<Policy> result = authService.getCorporateSignupPolicies();

        assertThat(result)
                .extracting(Policy::getPolicyId)
                .containsExactly(10L, 11L, 12L, 13L);
    }

    private List<Policy> personalPolicies() {
        return List.of(
                policy(1L, "필수"),
                policy(2L, "필수"),
                policy(3L, "선택"),
                policy(4L, "선택")
        );
    }

    private List<Policy> corporatePolicies() {
        return List.of(
                policy(10L, "필수"),
                policy(11L, "필수"),
                policy(12L, "선택")
        );
    }

    private Policy policy(Long policyId, String consentType) {
        return Policy.builder()
                .policyId(policyId)
                .consentType(consentType)
                .build();
    }
}
