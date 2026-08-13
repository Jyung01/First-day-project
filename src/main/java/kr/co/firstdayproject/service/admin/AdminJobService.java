package kr.co.firstdayproject.service.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.co.firstdayproject.dto.admin.job.AdminJobDetailView;
import kr.co.firstdayproject.dto.admin.job.AdminJobListItem;
import kr.co.firstdayproject.dto.job.JobCategoryGroup;
import kr.co.firstdayproject.dto.job.JobCategoryOption;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.entity.job.JobPostingSkill;
import kr.co.firstdayproject.entity.job.Skill;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.JobPostingSkillRepository;
import kr.co.firstdayproject.repository.job.SkillRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminJobService {

    private static final int PAGE_SIZE = 10;
    private static final String CANCELLED_APPLICATION_STATUS = "지원취소";
    private static final List<String> MANAGED_STATUSES = List.of(
        "모집예정",
        "모집중",
        "마감",
        "숨김",
        "재검토요청"
    );

    private final JobPostingRepository jobPostingRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final CompanyRepository companyRepository;
    private final JobPostingSkillRepository jobPostingSkillRepository;
    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public AdminJobDetailView getJobPosting(Long jobPostingId) {
        JobPosting posting = jobPostingRepository
            .findByJobPostingIdAndStatusIn(
                jobPostingId,
                MANAGED_STATUSES
            )
            .orElseThrow(() -> new IllegalArgumentException(
                "채용공고를 찾을 수 없습니다."
            ));
        Company company = companyRepository
            .findById(posting.getCompanyId())
            .orElseThrow(() -> new IllegalArgumentException(
                "기업 정보를 찾을 수 없습니다."
            ));
        User contact = userRepository
            .findFirstByCompanyIdAndUserTypeOrderByUserIdAsc(
                company.getCompanyId(),
                "기업"
            )
            .orElse(null);

        return new AdminJobDetailView(
            posting.getJobPostingId(),
            company.getCompanyName(),
            display(company.getIndustryName()),
            getCompanyAddress(company),
            contact == null ? "미등록" : contact.getName(),
            contact == null ? null : trimToNull(contact.getDepartment()),
            contact == null ? null : trimToNull(contact.getPositionTitle()),
            contact == null ? "미등록" : contact.getEmail(),
            contact == null ? "미등록" : display(contact.getPhone()),
            posting.getTitle(),
            posting.getStatus(),
            getCategoryName(posting.getJobCategoryId()),
            display(posting.getEmploymentType()),
            getCareerText(posting),
            display(posting.getEducationLevel()),
            display(posting.getWorkRegion()),
            display(posting.getWorkAddress()),
            getSalaryText(posting),
            posting.getHeadcount(),
            posting.getCreatedAt(),
            posting.getPublishedAt(),
            posting.getApplyStartAt(),
            posting.getApplyEndAt(),
            posting.getClosedAt(),
            posting.getHiddenAt(),
            posting.getHiddenReason(),
            applicationRepository
                .countByJobPostingIdAndCurrentStatusNot(
                    posting.getJobPostingId(),
                    CANCELLED_APPLICATION_STATUS
                ),
            getSkillNames(posting.getJobPostingId()),
            parseBenefits(posting.getBenefitsJson()),
            display(posting.getIntroduction()),
            display(posting.getMainTasks()),
            display(posting.getQualifications()),
            display(posting.getPreferredConditions())
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Page<AdminJobListItem> getJobPostings(
        String status,
        Long parentCategoryId,
        Long categoryId,
        String keyword,
        int page
    ) {
        PageRequest pageRequest = PageRequest.of(
            Math.max(page, 0),
            PAGE_SIZE,
            Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "jobPostingId"))
        );

        return jobPostingRepository.findAdminJobPostings(
            MANAGED_STATUSES,
            normalizeStatus(status),
            parentCategoryId,
            categoryId,
            normalizeKeyword(keyword),
            pageRequest
        );
    }

    public List<JobCategoryGroup> getJobCategoryGroups() {
        List<JobCategory> categories = jobCategoryRepository.findByIsActiveTrue(
            Sort.by(
                "depth",
                "parentId",
                "displayOrder",
                "jobCategoryId"
            )
        );

        return categories.stream()
            .filter(category -> category.getDepth() == 1)
            .map(parent -> new JobCategoryGroup(
                parent.getJobCategoryId(),
                parent.getCategoryName(),
                categories.stream()
                    .filter(child -> child.getDepth() == 2)
                    .filter(child -> parent.getJobCategoryId().equals(
                        child.getParentId()
                    ))
                    .map(child -> new JobCategoryOption(
                        child.getJobCategoryId(),
                        child.getCategoryName()
                    ))
                    .toList()
            ))
            .toList();
    }

    public Map<String, Long> getStatistics() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        LocalDateTime now = LocalDateTime.now();

        Map<String, Long> statistics = new LinkedHashMap<>();
        statistics.put(
            "todayCreated",
            jobPostingRepository.countByStatusInAndCreatedAtBetween(
                MANAGED_STATUSES,
                startOfToday,
                startOfTomorrow
            )
        );
        statistics.put(
            "todayPublished",
            jobPostingRepository.countByStatusAndPublishedAtBetween(
                "모집중",
                startOfToday,
                startOfTomorrow
            )
        );
        statistics.put(
            "open",
            jobPostingRepository.countByStatus("모집중")
        );
        statistics.put(
            "closingSoon",
            jobPostingRepository.countByStatusAndApplyEndAtBetween(
                "모집중",
                now,
                now.plusDays(7)
            )
        );
        statistics.put(
            "hidden",
            jobPostingRepository.countByStatusIn(
                List.of("숨김", "재검토요청")
            )
        );
        statistics.put(
            "review",
            jobPostingRepository.countByStatus("재검토요청")
        );
        return statistics;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equals(status)) {
            return null;
        }

        return switch (status) {
            case "SCHEDULED" -> "모집예정";
            case "POSTED" -> "모집중";
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
        List<Long> skillIds = jobPostingSkillRepository
            .findAllByIdJobPostingId(jobPostingId)
            .stream()
            .map(JobPostingSkill::getId)
            .map(id -> id.getSkillId())
            .toList();
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

    private String getCareerText(JobPosting posting) {
        String type = display(posting.getCareerType());
        Integer minimum = posting.getMinExperienceYears();
        Integer maximum = posting.getMaxExperienceYears();

        if (minimum == null && maximum == null) {
            return type;
        }
        if (minimum != null && maximum == null) {
            return type + " - " + minimum + "년 이상";
        }
        if (minimum == null) {
            return type + " - " + maximum + "년 이하";
        }
        return type + " - " + minimum + "~" + maximum + "년";
    }

    private String getSalaryText(JobPosting posting) {
        Integer minimum = posting.getSalaryMin();
        Integer maximum = posting.getSalaryMax();

        if (minimum == null && maximum == null) {
            return display(posting.getSalaryText());
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

    private String getCompanyAddress(Company company) {
        String address = display(company.getAddressLine1());
        if (company.getAddressLine2() == null
            || company.getAddressLine2().isBlank()) {
            return address;
        }
        return address + " " + company.getAddressLine2().trim();
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

    private String formatAmount(Integer amount) {
        return String.format("%,d", amount);
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "미등록" : value;
    }
}
