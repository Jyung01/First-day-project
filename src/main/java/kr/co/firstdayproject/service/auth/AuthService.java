package kr.co.firstdayproject.service.auth;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import kr.co.firstdayproject.dto.auth.CorporateTermsAgreement;
import kr.co.firstdayproject.dto.auth.PersonalTermsAgreement;
import kr.co.firstdayproject.entity.policy.Policy;
import kr.co.firstdayproject.repository.policy.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final PolicyRepository policyRepository;

    public List<Policy> getPersonalSignupPolicies() {
        return getSignupPolicies("개인");
    }

    public List<Policy> getCorporateSignupPolicies() {
        return getSignupPolicies("기업").stream()
                .sorted(Comparator.comparingInt(
                        policy -> "필수".equals(policy.getConsentType()) ? 0 : 1
                ))
                .toList();
    }

    public Optional<PersonalTermsAgreement> validatePersonalTermsAgreement(
            Set<Long> submittedPolicyIds
    ) {
        return validateSignupTermsAgreement("개인", submittedPolicyIds)
                .map(agreement -> new PersonalTermsAgreement(
                        agreement.agreedPolicyIds(),
                        agreement.displayedPolicyIds(),
                        agreement.agreedAt()
                ));
    }

    public Optional<CorporateTermsAgreement> validateCorporateTermsAgreement(
            Set<Long> submittedPolicyIds
    ) {
        return validateSignupTermsAgreement("기업", submittedPolicyIds)
                .map(agreement -> new CorporateTermsAgreement(
                        agreement.agreedPolicyIds(),
                        agreement.displayedPolicyIds(),
                        agreement.agreedAt()
                ));
    }

    private List<Policy> getSignupPolicies(String audience) {
        return policyRepository.findActiveSignupPolicies(
                audience,
                LocalDateTime.now().toLocalDate()
        );
    }

    private Optional<TermsAgreement> validateSignupTermsAgreement(
            String audience,
            Set<Long> submittedPolicyIds
    ) {
        LocalDateTime now = LocalDateTime.now();
        List<Policy> policies = policyRepository.findActiveSignupPolicies(
                audience,
                now.toLocalDate()
        );

        if (policies.isEmpty()) {
            return Optional.empty();
        }

        Set<Long> displayedPolicyIds = policies.stream()
                .map(Policy::getPolicyId)
                .collect(Collectors.toSet());

        Set<Long> agreedPolicyIds = submittedPolicyIds.stream()
                .filter(displayedPolicyIds::contains)
                .collect(Collectors.toSet());

        Set<Long> requiredPolicyIds = policies.stream()
                .filter(policy -> "필수".equals(policy.getConsentType()))
                .map(Policy::getPolicyId)
                .collect(Collectors.toSet());

        if (!agreedPolicyIds.containsAll(requiredPolicyIds)) {
            return Optional.empty();
        }

        return Optional.of(new TermsAgreement(
                agreedPolicyIds,
                displayedPolicyIds,
                now
        ));
    }

    private record TermsAgreement(
            Set<Long> agreedPolicyIds,
            Set<Long> displayedPolicyIds,
            LocalDateTime agreedAt
    ) {
    }
}
