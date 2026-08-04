package kr.co.firstdayproject.dto.auth;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public record PersonalTermsAgreement(
        Set<Long> agreedPolicyIds,
        Set<Long> displayedPolicyIds,
        LocalDateTime agreedAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public PersonalTermsAgreement {
        agreedPolicyIds = immutableCopy(agreedPolicyIds);
        displayedPolicyIds = immutableCopy(displayedPolicyIds);
        agreedAt = Objects.requireNonNull(agreedAt);
    }

    private static Set<Long> immutableCopy(Set<Long> policyIds) {
        return Collections.unmodifiableSet(
                new HashSet<>(Objects.requireNonNull(policyIds))
        );
    }
}
