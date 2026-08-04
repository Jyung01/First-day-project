package kr.co.firstdayproject.service.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
        return policyRepository.findActiveSignupPolicies(
                "개인",
                LocalDateTime.now()
        );
    }

    public Optional<PersonalTermsAgreement> validatePersonalTermsAgreement(
            Set<Long> submittedPolicyIds
    ) {
        LocalDateTime now = LocalDateTime.now();
        List<Policy> policies = policyRepository.findActiveSignupPolicies(
                "개인",
                now
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

        return Optional.of(new PersonalTermsAgreement(
                agreedPolicyIds,
                displayedPolicyIds,
                now
        ));
    }
}
