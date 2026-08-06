package kr.co.firstdayproject.service.corp;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.firstdayproject.dto.corp.job.CorpJobDetailView;
import kr.co.firstdayproject.dto.corp.job.CorpJobListItem;
import kr.co.firstdayproject.dto.corp.job.JobPostingCreateRequest;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.entity.job.JobPostingSkill;
import kr.co.firstdayproject.entity.job.Skill;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.JobPostingSkillRepository;
import kr.co.firstdayproject.repository.job.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorpJobQueryService {

    private static final String DELETED_STATUS = "삭제";
    private static final String CANCELLED_APPLICATION_STATUS = "지원취소";
    private static final int JOB_LIST_PAGE_SIZE = 8;

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobPostingSkillRepository jobPostingSkillRepository;
    private final SkillRepository skillRepository;

    public CorpJobDetailView getClosedJobPosting(
        Long companyId,
        Long jobPostingId
    ) {
        validateCompanyId(companyId);

        JobPosting posting = jobPostingRepository
            .findByJobPostingIdAndCompanyId(jobPostingId, companyId)
            .orElseThrow(() -> new IllegalArgumentException(
                "채용공고를 찾을 수 없습니다."
            ));

        if (!"마감".equals(posting.getStatus())) {
            throw new IllegalArgumentException(
                "마감된 채용공고만 조회할 수 있습니다."
            );
        }

        return new CorpJobDetailView(
            posting.getJobPostingId(),
            posting.getTitle(),
            posting.getStatus(),
            getCategoryName(posting.getJobCategoryId()),
            display(posting.getEmploymentType()),
            getCareerText(posting),
            display(posting.getEducationLevel()),
            display(posting.getWorkRegion()),
            display(posting.getWorkAddress()),
            getSalaryText(posting),
            posting.getCreatedAt(),
            posting.getApplyStartAt(),
            posting.getPublishedAt(),
            posting.getApplyEndAt(),
            posting.getClosedAt(),
            posting.getHeadcount(),
            getSkillNames(posting.getJobPostingId()),
            parseBenefits(posting.getBenefitsJson()),
            display(posting.getIntroduction()),
            display(posting.getMainTasks()),
            display(posting.getQualifications()),
            display(posting.getPreferredConditions()),
            countApplicants(posting.getJobPostingId()),
            posting.getHiddenReason()
        );
    }

    public JobPostingCreateRequest getEditableJobPosting(
        Long companyId,
        Long jobPostingId
    ) {
        validateCompanyId(companyId);

        JobPosting posting = jobPostingRepository
            .findByJobPostingIdAndCompanyId(jobPostingId, companyId)
            .orElseThrow(() -> new IllegalArgumentException(
                "채용공고를 찾을 수 없습니다."
            ));

        if (!List.of("임시저장", "모집예정", "모집중", "숨김")
            .contains(posting.getStatus())) {
            throw new IllegalArgumentException(
                "수정할 수 없는 상태의 채용공고입니다."
            );
        }

        JobPostingCreateRequest request = new JobPostingCreateRequest();
        request.setJobCategoryId(posting.getJobCategoryId());
        request.setTitle(posting.getTitle());
        request.setEmploymentType(posting.getEmploymentType());
        request.setCareerType(posting.getCareerType());
        request.setMinExperienceYears(posting.getMinExperienceYears());
        request.setMaxExperienceYears(posting.getMaxExperienceYears());
        request.setEducationLevel(posting.getEducationLevel());
        request.setWorkRegion(posting.getWorkRegion());
        request.setAddress(posting.getWorkAddress());
        request.setSalaryText(posting.getSalaryText());
        request.setSalaryMin(posting.getSalaryMin());
        request.setSalaryMax(posting.getSalaryMax());
        request.setHeadcount(posting.getHeadcount());
        request.setApplyStartDate(posting.getApplyStartAt() == null
            ? null
            : posting.getApplyStartAt().toLocalDate());
        request.setApplyEndDate(posting.getApplyEndAt() == null
            ? null
            : posting.getApplyEndAt().toLocalDate());
        request.setIntroduction(posting.getIntroduction());
        request.setMainTasks(posting.getMainTasks());
        request.setQualifications(posting.getQualifications());
        request.setPreferredConditions(posting.getPreferredConditions());
        request.setBenefits(parseBenefits(posting.getBenefitsJson()));
        request.setSkillIds(getSkillIds(posting.getJobPostingId()));
        request.setSubmitType(switch (posting.getStatus()) {
            case "임시저장" -> "DRAFT";
            case "숨김" -> "REVIEW";
            default -> "PUBLISH";
        });
        return request;
    }

    public String getHiddenReason(Long companyId, Long jobPostingId) {
        validateCompanyId(companyId);

        JobPosting posting = jobPostingRepository
            .findByJobPostingIdAndCompanyId(jobPostingId, companyId)
            .orElseThrow(() -> new IllegalArgumentException(
                "채용공고를 찾을 수 없습니다."
            ));

        if (!"숨김".equals(posting.getStatus())) {
            return null;
        }

        return posting.getHiddenReason();
    }

    public boolean isRecruitingJobPosting(
        Long companyId,
        Long jobPostingId
    ) {
        return hasJobPostingStatus(companyId, jobPostingId, "모집중");
    }

    public boolean isScheduledJobPosting(
        Long companyId,
        Long jobPostingId
    ) {
        return hasJobPostingStatus(companyId, jobPostingId, "모집예정");
    }

    private boolean hasJobPostingStatus(
        Long companyId,
        Long jobPostingId,
        String status
    ) {
        validateCompanyId(companyId);

        return jobPostingRepository
            .findByJobPostingIdAndCompanyId(jobPostingId, companyId)
            .map(posting -> status.equals(posting.getStatus()))
            .orElseThrow(() -> new IllegalArgumentException(
                "채용공고를 찾을 수 없습니다."
            ));
    }

    public Page<CorpJobListItem> getJobPostings(
        Long companyId,
        String status,
        String keyword,
        int page
    ) {
        validateCompanyId(companyId);

        PageRequest pageRequest = PageRequest.of(
            Math.max(page, 0),
            JOB_LIST_PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "updatedAt")
                .and(Sort.by(Sort.Direction.DESC, "jobPostingId"))
        );

        return jobPostingRepository.findCorpJobPostings(
            companyId,
            normalizeStatus(status),
            normalizeKeyword(keyword),
            pageRequest
        ).map(this::toListItem);
    }

    public Map<String, Long> getJobStatusCounts(Long companyId) {
        validateCompanyId(companyId);

        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put(
            "all",
            jobPostingRepository.countByCompanyIdAndStatusNot(
                companyId,
                DELETED_STATUS
            )
        );
        counts.put("open", countByStatus(companyId, "모집중"));
        counts.put("scheduled", countByStatus(companyId, "모집예정"));
        counts.put("draft", countByStatus(companyId, "임시저장"));
        counts.put("closed", countByStatus(companyId, "마감"));
        counts.put("hidden", countByStatus(companyId, "숨김"));
        counts.put("review", countByStatus(companyId, "재검토요청"));
        return counts;
    }

    private CorpJobListItem toListItem(JobPosting posting) {
        return new CorpJobListItem(
            posting.getJobPostingId(),
            posting.getTitle(),
            posting.getStatus(),
            countApplicants(posting.getJobPostingId()),
            posting.getCreatedAt(),
            posting.getUpdatedAt(),
            posting.getHiddenReason()
        );
    }

    private long countByStatus(Long companyId, String status) {
        return jobPostingRepository.countByCompanyIdAndStatus(
            companyId,
            status
        );
    }

    private long countApplicants(Long jobPostingId) {
        return applicationRepository
            .countByJobPostingIdAndCurrentStatusNot(
                jobPostingId,
                CANCELLED_APPLICATION_STATUS
            );
    }

    private void validateCompanyId(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("기업회원 로그인이 필요합니다.");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equals(status)) {
            return null;
        }

        return switch (status) {
            case "OPEN" -> "모집중";
            case "SCHEDULED" -> "모집예정";
            case "DRAFT" -> "임시저장";
            case "CLOSED" -> "마감";
            case "HIDDEN" -> "숨김";
            case "REVIEW" -> "재검토요청";
            default -> throw new IllegalArgumentException(
                "올바르지 않은 공고 상태입니다."
            );
        };
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private String getCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "미설정";
        }

        JobCategory category = jobCategoryRepository.findById(categoryId)
            .orElse(null);
        if (category == null) {
            return "미설정";
        }

        if (category.getParentId() == null) {
            return category.getCategoryName();
        }

        String parentName = jobCategoryRepository
            .findById(category.getParentId())
            .map(JobCategory::getCategoryName)
            .orElse("미설정");
        return parentName + " - " + category.getCategoryName();
    }

    private List<String> getSkillNames(Long jobPostingId) {
        List<Long> skillIds = getSkillIds(jobPostingId);

        Map<Long, Skill> skillMap = skillRepository.findAllById(skillIds)
            .stream()
            .collect(Collectors.toMap(
                Skill::getSkillId,
                Function.identity()
            ));

        return skillIds.stream()
            .map(skillMap::get)
            .filter(skill -> skill != null)
            .map(Skill::getSkillName)
            .toList();
    }

    private List<Long> getSkillIds(Long jobPostingId) {
        return jobPostingSkillRepository
            .findAllByIdJobPostingId(jobPostingId)
            .stream()
            .map(JobPostingSkill::getId)
            .map(id -> id.getSkillId())
            .toList();
    }

    private String getCareerText(JobPosting posting) {
        String careerType = display(posting.getCareerType());
        Integer minimum = posting.getMinExperienceYears();
        Integer maximum = posting.getMaxExperienceYears();

        if (minimum == null && maximum == null) {
            return careerType;
        }
        if (minimum != null && maximum == null) {
            return careerType + " - " + minimum + "년 이상";
        }
        if (minimum == null) {
            return careerType + " - " + maximum + "년 이하";
        }
        return careerType + " - " + minimum + "~" + maximum + "년";
    }

    private String getSalaryText(JobPosting posting) {
        String salaryText = display(posting.getSalaryText());
        Integer minimum = posting.getSalaryMin();
        Integer maximum = posting.getSalaryMax();

        if (minimum == null && maximum == null) {
            return salaryText;
        }
        if (minimum != null && maximum == null) {
            return "연봉 - " + formatAmount(minimum) + "만원 이상";
        }
        if (minimum == null) {
            return "연봉 - " + formatAmount(maximum) + "만원 이하";
        }
        return "연봉 - "
            + formatAmount(minimum)
            + "~"
            + formatAmount(maximum)
            + "만원";
    }

    private String formatAmount(Integer amount) {
        return String.format("%,d", amount);
    }

    private List<String> parseBenefits(String benefitsJson) {
        if (benefitsJson == null || benefitsJson.isBlank()) {
            return List.of();
        }

        String content = benefitsJson.trim();
        if (content.length() < 2) {
            return List.of();
        }

        return Arrays.stream(
                content.substring(1, content.length() - 1).split(",")
            )
            .map(String::trim)
            .map(value -> value.replaceAll("^\"|\"$", ""))
            .filter(value -> !value.isBlank())
            .toList();
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "미입력" : value;
    }
}
