package kr.co.firstdayproject.repository.policy;

import kr.co.firstdayproject.entity.policy.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<Policy> findByPolicyCodeAndIsActiveTrue(String policyCode);

    Optional<Policy> findByPolicyCode(String policyCode);

    /**
     * 회원가입 화면 등 기존 호출부 호환용. LocalDateTime을 받아 LocalDate로 변환 후
     * 실제 쿼리 메서드(findActiveSignupPoliciesAsOf)에 위임합니다.
     * 기존에 이 메서드를 LocalDateTime으로 호출하던 코드는 전혀 수정하지 않아도 됩니다.
     */
    default List<Policy> findActiveSignupPolicies(String audience, LocalDateTime now) {
        return findActiveSignupPoliciesAsOf(audience, now.toLocalDate());
    }

    /**
     * 회원가입 화면에서 사용하는 활성 약관 조회.
     * audience가 지정 대상(예: "개인"/"기업") 또는 "전체"이고,
     * is_active = true, effective_from이 today 이전(또는 null)인 약관만 노출 순서대로 반환.
     *
     * Policy.effectiveFrom이 LocalDate 컬럼이라 파라미터도 LocalDate로 맞춤.
     */
    @Query("""
            SELECT p FROM Policy p
            WHERE p.isActive = true
              AND (p.audience = :audience OR p.audience = '전체')
              AND (p.effectiveFrom IS NULL OR p.effectiveFrom <= :today)
            ORDER BY p.displayOrder ASC
            """)
    List<Policy> findActiveSignupPoliciesAsOf(@Param("audience") String audience, @Param("today") LocalDate today);
}
              AND p.consentType IN ('필수', '선택')
              AND (p.effectiveFrom IS NULL OR p.effectiveFrom <= :today)
            ORDER BY p.displayOrder ASC
            """)
    List<Policy> findActiveSignupPolicies(@Param("audience") String audience, @Param("today") LocalDate today);
}
