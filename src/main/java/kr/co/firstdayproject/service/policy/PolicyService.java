package kr.co.firstdayproject.service.policy;

import kr.co.firstdayproject.dto.policy.PolicyDto;
import kr.co.firstdayproject.entity.policy.Policy;
import kr.co.firstdayproject.repository.policy.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final PolicyRepository policyRepository;

    /** 좌측 목록에 표시할 활성 약관 전체 (노출 순서대로) */
    public List<PolicyDto> getActivePolicies() {
        return policyRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** 코드로 활성 약관 하나 조회 (사용자 화면) */
    public PolicyDto getActivePolicyByCode(String policyCode) {
        Policy policy = policyRepository.findByPolicyCodeAndIsActiveTrue(policyCode)
                .orElseThrow(() -> new NoSuchElementException("약관을 찾을 수 없습니다. code=" + policyCode));
        return toDto(policy);
    }

    /** 관리자 - 활성/비활성 여부 상관없이 코드로 조회 */
    public PolicyDto getPolicyByCode(String policyCode) {
        Policy policy = policyRepository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new NoSuchElementException("약관을 찾을 수 없습니다. code=" + policyCode));
        return toDto(policy);
    }

    /** 관리자 - 약관 제목/본문 수정 */
    @Transactional
    public void updatePolicy(String policyCode, String title, String content, Long adminId) {
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("본문 내용을 입력해주세요.");
        }

        Policy policy = policyRepository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new NoSuchElementException("약관을 찾을 수 없습니다. code=" + policyCode));

        policy.setTitle(title.trim());
        policy.setContent(content.trim());
        policy.setUpdatedBy(adminId);
        policy.setUpdatedAt(LocalDateTime.now());
    }

    private PolicyDto toDto(Policy policy) {
        return PolicyDto.builder()
                .policyId(policy.getPolicyId())
                .policyCode(policy.getPolicyCode())
                .title(policy.getTitle())
                .audience(policy.getAudience())
                .consentType(policy.getConsentType())
                .content(policy.getContent())
                .effectiveFrom(policy.getEffectiveFrom() == null ? null : policy.getEffectiveFrom().format(DATE_FORMAT))
                .updatedAt(policy.getUpdatedAt() == null ? null : policy.getUpdatedAt().format(DATE_FORMAT))
                .build();
    }
}