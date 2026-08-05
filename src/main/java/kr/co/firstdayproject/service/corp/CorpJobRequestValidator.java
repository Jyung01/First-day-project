package kr.co.firstdayproject.service.corp;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import kr.co.firstdayproject.dto.corp.job.JobPostingCreateRequest;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CorpJobRequestValidator {

    private static final int CHILD_DEPTH = 2;
    private static final int MAX_SKILL_COUNT = 5;

    private static final Set<String> EMPLOYMENT_TYPES = Set.of(
        "정규직",
        "계약직",
        "인턴",
        "프리랜서",
        "파견직",
        "기타"
    );

    private static final Set<String> CAREER_TYPES = Set.of(
        "신입",
        "경력",
        "경력무관"
    );

    private static final Set<String> EDUCATION_LEVELS = Set.of(
        "학력무관",
        "고졸이상",
        "전문대졸이상",
        "대졸이상",
        "석사이상",
        "박사"
    );

    private static final Set<String> SALARY_TEXTS = Set.of(
        "연봉",
        "회사 내규에 따름",
        "면접 후 결정",
        "협의 후 결정"
    );

    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;

    public void validatePublishableCompany(Company company) {
        if (!"승인".equals(company.getApprovalStatus())
            || !"정상".equals(company.getCompanyStatus())) {
            throw new IllegalArgumentException(
                "승인된 정상 기업만 채용공고를 등록할 수 있습니다."
            );
        }
    }

    public void validateCategory(Long jobCategoryId) {
        jobCategoryRepository
            .findByJobCategoryIdAndDepthAndIsActiveTrue(
                jobCategoryId,
                CHILD_DEPTH
            )
            .orElseThrow(() -> new IllegalArgumentException(
                "활성화된 2차 직무를 선택해 주세요."
            ));
    }

    public void validatePublishRequest(JobPostingCreateRequest request) {
        requireText(request.getEmploymentType(), "고용 형태를 선택해 주세요.");
        requireText(request.getCareerType(), "경력 구분을 선택해 주세요.");
        requireText(request.getEducationLevel(), "학력 조건을 선택해 주세요.");
        requireText(request.getSalaryText(), "급여 표시를 선택해 주세요.");
        requireText(request.getWorkRegion(), "근무 지역을 입력해 주세요.");
        requireText(request.getAddress(), "근무 주소를 입력해 주세요.");
        requireText(request.getMainTasks(), "주요 업무를 입력해 주세요.");
        requireText(request.getQualifications(), "자격 요건을 입력해 주세요.");

        if (request.getHeadcount() == null) {
            throw new IllegalArgumentException("모집 인원을 입력해 주세요.");
        }
        if (request.getApplyEndDate() == null) {
            throw new IllegalArgumentException("접수 마감일을 선택해 주세요.");
        }

        validateConditionalFields(request);
        validateCategory(request.getJobCategoryId());
    }

    public List<Long> normalizeAndValidateSkills(List<Long> requestedIds) {
        List<Long> skillIds = requestedIds == null
            ? List.of()
            : new LinkedHashSet<>(requestedIds).stream().toList();

        if (skillIds.size() > MAX_SKILL_COUNT) {
            throw new IllegalArgumentException(
                "기술 스택은 최대 5개까지 선택할 수 있습니다."
            );
        }

        int activeSkillCount = skillRepository
            .findAllBySkillIdInAndDepthAndIsActiveTrue(
                skillIds,
                CHILD_DEPTH
            )
            .size();

        if (activeSkillCount != skillIds.size()) {
            throw new IllegalArgumentException(
                "선택한 기술 중 사용할 수 없는 기술이 있습니다."
            );
        }

        return skillIds;
    }

    private void validateConditionalFields(
        JobPostingCreateRequest request
    ) {
        if (!EMPLOYMENT_TYPES.contains(request.getEmploymentType())) {
            throw new IllegalArgumentException(
                "올바른 고용 형태를 선택해 주세요."
            );
        }
        if (!CAREER_TYPES.contains(request.getCareerType())) {
            throw new IllegalArgumentException(
                "올바른 경력 구분을 선택해 주세요."
            );
        }
        if (!EDUCATION_LEVELS.contains(request.getEducationLevel())) {
            throw new IllegalArgumentException(
                "올바른 학력 조건을 선택해 주세요."
            );
        }
        if (!SALARY_TEXTS.contains(request.getSalaryText())) {
            throw new IllegalArgumentException(
                "올바른 급여 표시를 선택해 주세요."
            );
        }

        if ("경력".equals(request.getCareerType())) {
            validateRange(
                request.getMinExperienceYears(),
                request.getMaxExperienceYears(),
                "경력 연수"
            );
        } else {
            request.setMinExperienceYears(null);
            request.setMaxExperienceYears(null);
        }

        if ("연봉".equals(request.getSalaryText())) {
            validateRange(
                request.getSalaryMin(),
                request.getSalaryMax(),
                "연봉"
            );
        } else {
            request.setSalaryMin(null);
            request.setSalaryMax(null);
        }
    }

    private void validateRange(
        Integer minimum,
        Integer maximum,
        String fieldName
    ) {
        if (minimum == null && maximum == null) {
            throw new IllegalArgumentException(
                fieldName + "은 최소값 또는 최대값을 입력해 주세요."
            );
        }
        if (minimum != null && maximum != null && minimum > maximum) {
            throw new IllegalArgumentException(
                fieldName + "은 최대값이 최소값 이상이어야 합니다."
            );
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
