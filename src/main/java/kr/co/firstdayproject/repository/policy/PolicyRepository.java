package kr.co.firstdayproject.repository.policy;

import kr.co.firstdayproject.entity.policy.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<Policy> findByPolicyCodeAndIsActiveTrue(String policyCode);

    Optional<Policy> findByPolicyCode(String policyCode);

    /**
     * 회원가입 화면에서 사용하는 활성 약관 조회.
     * audience가 지정 대상(예: "개인"/"기업") 또는 "전체"이고,
     * is_active = true, effective_from이 now 이전(또는 null)인 약관만 노출 순서대로 반환.
     *
     * TODO: 원래 프로젝트에 있던 시그니처를 추정으로 복원한 것입니다.
     *       PersonalSignupService 등 실제 호출부와 동작이 다르면 알려주세요.
     */
    @Query("""
            SELECT p FROM Policy p
            WHERE p.isActive = true
              AND (p.audience = :audience OR p.audience = '전체')
              AND p.consentType IN ('필수', '선택')
              AND (p.effectiveFrom IS NULL OR p.effectiveFrom <= :today)
            ORDER BY p.displayOrder ASC
            """)
    List<Policy> findActiveSignupPolicies(@Param("audience") String audience, @Param("today") LocalDate today);
}
