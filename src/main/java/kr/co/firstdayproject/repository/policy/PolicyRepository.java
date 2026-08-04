package kr.co.firstdayproject.repository.policy;

import kr.co.firstdayproject.entity.policy.Policy;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    @Query("""
            select p
            from Policy p
            where p.isActive = true
              and (p.audience = '전체' or p.audience = :audience)
              and p.consentType in ('필수', '선택')
              and (p.effectiveFrom is null or p.effectiveFrom <= :now)
            order by p.displayOrder asc, p.policyId asc
            """)
    List<Policy> findActiveSignupPolicies(
            @Param("audience") String audience,
            @Param("now") LocalDateTime now
    );
}
