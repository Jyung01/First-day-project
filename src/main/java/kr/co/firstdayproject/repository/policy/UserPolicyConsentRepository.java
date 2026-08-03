package kr.co.firstdayproject.repository.policy;

import kr.co.firstdayproject.entity.policy.UserPolicyConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPolicyConsentRepository extends JpaRepository<UserPolicyConsent, Long> {
}
