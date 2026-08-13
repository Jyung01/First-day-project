package kr.co.firstdayproject.service.job;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import kr.co.firstdayproject.dto.job.*;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.job.Skill;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.entity.job.JobPostingSkill;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.SkillRepository;
import kr.co.firstdayproject.repository.job.JobPostingSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    /*
     * 메인 화면에서 최신순과 인기순으로 보여줄 공고 개수.
     */
    private static final int MAIN_JOB_COUNT = 6;
    private static final int JOB_LIST_PAGE_SIZE = 12;
    private static final int JOB_DETAIL_LIST_PAGE_SIZE = 3;

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;
    private final SavedJobService savedJobService;
    private final CompanyRepository companyRepository;
    private final JobPostingSkillRepository jobPostingSkillRepository;

    @Transactional
    public JobDetailView getRecruitingJobPosting(
            Long jobPostingId,
            Authentication authentication
    ) {
        JobPosting posting = jobPostingRepository
                .findByJobPostingIdAndStatusIn(
                        jobPostingId,
                        List.of("모집중")
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "조회할 수 없는 채용공고입니다."
                ));

        Company company = companyRepository.findById(posting.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "기업 정보를 찾을 수 없습니다."
                ));

        posting.setViewCount(
                (posting.getViewCount() == null ? 0L : posting.getViewCount()) + 1
        );
        long applicantCount = applicationRepository
                .countByJobPostingIdAndCurrentStatusNotIn(
                        jobPostingId,
                        List.of("지원취소", "채용종료")
                );

        return new JobDetailView(
                posting.getJobPostingId(),
                posting.getTitle(),
                company.getCompanyName(),
                company.getLogoUrl(),
                display(company.getIndustryName()),
                display(company.getShortDescription()),
                display(company.getIntroduction()),
                joinAddress(company.getAddressLine1(), company.getAddressLine2()),
                getCategoryText(posting.getJobCategoryId()),
                getCareerText(posting),
                display(posting.getEducationLevel()),
                display(posting.getEmploymentType()),
                display(posting.getWorkRegion()),
                display(posting.getWorkAddress()),
                getSalaryText(posting),
                posting.getHeadcount(),
                posting.getApplyStartAt(),
                posting.getApplyEndAt(),
                display(posting.getIntroduction()),
                display(posting.getMainTasks()),
                display(posting.getQualifications()),
                display(posting.getPreferredConditions()),
                parseBenefits(posting.getBenefitsJson()),
                getSkillNames(posting.getJobPostingId()),
                posting.getViewCount(),
                getDeadlineText(posting.getApplyEndAt(), LocalDate.now()),
                posting.getPublishedAt() != null
                        && !posting.getPublishedAt().isBefore(
                                LocalDateTime.now().minusDays(7)
                        ),
                posting.getViewCount() >= 100 || applicantCount >= 10,
                savedJobService.getSavedJobPostingIds(
                        List.of(jobPostingId),
                        authentication
                ).contains(jobPostingId)
        );
    }

    // 메인: 모집 중인 채용공고 최신순
    public List<MainJobListItem> getLatestJobPostingList() {
        return jobPostingRepository.findLatestRecruitingJobPostings(
                PageRequest.of(0, MAIN_JOB_COUNT)
        );
    }

    // 메인: 모집 중인 채용공고 인기순
    public List<MainJobListItem> getPopularJobPostingList() {
        return jobPostingRepository.findPopularRecruitingJobPostings(
                PageRequest.of(0, MAIN_JOB_COUNT)
        );
    }

    /**
     * 일반회원 화면에서 사용할 활성 직무 카테고리 목록
     */
    public List<JobCategoryGroup> getActiveJobCategoryGroups() {
        List<JobCategory> parents = jobCategoryRepository
                .findAllByIsActiveTrueAndDepthOrderByDisplayOrderAscJobCategoryIdAsc(
                        1
                );

        List<JobCategory> children = jobCategoryRepository
                .findAllByIsActiveTrueAndDepthOrderByDisplayOrderAscJobCategoryIdAsc(
                        2
                );

        Map<Long, List<JobCategoryOption>> childrenByParentId =
                new LinkedHashMap<>();

        for (JobCategory child : children) {
            if (child.getParentId() == null) {
                continue;
            }

            childrenByParentId
                    .computeIfAbsent(
                            child.getParentId(),
                            ignored -> new ArrayList<>()
                    )
                    .add(new JobCategoryOption(
                            child.getJobCategoryId(),
                            child.getCategoryName()
                    ));
        }

        return parents.stream()
                .map(parent -> new JobCategoryGroup(
                        parent.getJobCategoryId(),
                        parent.getCategoryName(),
                        childrenByParentId.getOrDefault(
                                parent.getJobCategoryId(),
                                List.of()
                        )
                ))
                .filter(group -> !group.children().isEmpty())
                .toList();
    }

    /**
     * 일반회원 필터에서 사용할 활성 기술 분류와 하위 기술 목록.
     */
    public List<SkillFilterGroup> getActiveSkillGroups() {
        List<Skill> skills = skillRepository.findByIsActiveTrue(
                Sort.by(
                        "depth",
                        "parentId",
                        "displayOrder",
                        "skillId"
                )
        );

        return skills.stream()
                .filter(skill -> skill.getDepth() == 1)
                .map(parent -> new SkillFilterGroup(
                        parent.getSkillId(),
                        parent.getSkillName(),
                        skills.stream()
                                .filter(child -> child.getDepth() == 2)
                                .filter(child -> parent.getSkillId().equals(
                                        child.getParentId()
                                ))
                                .map(child -> new SkillFilterOption(
                                        child.getSkillId(),
                                        child.getSkillName()
                                ))
                                .toList()
                ))
                .filter(group -> !group.children().isEmpty())
                .toList();
    }

    public Page<JobListItem> getRecruitingJobPostingList(
            String keyword,
            Long parentCategoryId,
            List<Long> categoryIds,
            List<String> regions,
            List<String> careers,
            List<String> educations,
            List<Long> skillIds,
            String sortType,
            int page,
            Authentication authentication
    ) {
        return getRecruitingJobPostingList(
                keyword,
                parentCategoryId,
                categoryIds,
                regions,
                careers,
                educations,
                skillIds,
                sortType,
                JOB_LIST_PAGE_SIZE,
                page,
                authentication
        );
    }

    public Page<JobListItem> getRecruitingJobPostingDetailList(
            String keyword,
            Long parentCategoryId,
            List<Long> categoryIds,
            List<String> regions,
            List<String> careers,
            List<String> educations,
            List<Long> skillIds,
            String sortType,
            int page,
            Authentication authentication
    ) {
        return getRecruitingJobPostingList(
                keyword,
                parentCategoryId,
                categoryIds,
                regions,
                careers,
                educations,
                skillIds,
                sortType,
                JOB_DETAIL_LIST_PAGE_SIZE,
                page,
                authentication
        );
    }

    private Page<JobListItem> getRecruitingJobPostingList(
            String keyword,
            Long parentCategoryId,
            List<Long> categoryIds,
            List<String> regions,
            List<String> careers,
            List<String> educations,
            List<Long> skillIds,
            String sortType,
            int pageSize,
            int page,
            Authentication authentication
    ) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                pageSize,
                resolveJobListSort(sortType)
        );

        List<String> normalizedRegions = normalizeRegions(regions);
        List<Long> normalizedCategoryIds = normalizeIds(categoryIds);
        List<String> normalizedCareers = normalizeValues(careers);
        List<String> normalizedEducations = normalizeValues(educations);
        List<Long> normalizedSkillIds = normalizeIds(skillIds);

        Page<JobListQueryItem> queryPage =
                jobPostingRepository.findRecruitingJobPostings(
                        normalizeKeyword(keyword),
                        parentCategoryId,
                        !normalizedCategoryIds.isEmpty(),
                        valuesOrSentinel(normalizedCategoryIds, -1L),
                        !normalizedRegions.isEmpty(),
                        valuesOrSentinel(normalizedRegions, ""),
                        !normalizedCareers.isEmpty(),
                        valuesOrSentinel(normalizedCareers, ""),
                        !normalizedEducations.isEmpty(),
                        valuesOrSentinel(normalizedEducations, ""),
                        !normalizedSkillIds.isEmpty(),
                        valuesOrSentinel(normalizedSkillIds, -1L),
                        pageRequest
                );

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        Set<Long> savedJobPostingIds =
                savedJobService.getSavedJobPostingIds(
                        queryPage.getContent()
                                .stream()
                                .map(JobListQueryItem::jobPostingId)
                                .toList(),
                        authentication
                );

        Map<Long, List<String>> skillNamesByPostingId =
                getListSkillNames(queryPage.getContent());

        return queryPage.map(job -> {
            long viewCount =
                    job.viewCount() == null ? 0L : job.viewCount();

            boolean newPosting =
                    job.publishedAt() != null
                            && !job.publishedAt().isBefore(
                            now.minusDays(7)
                    );

            long applicantCount =
                    job.applicantCount() == null
                            ? 0L
                            : job.applicantCount();

            boolean hotPosting =
                    viewCount >= 100
                            || applicantCount >= 10;

            boolean bookmarked = savedJobPostingIds.contains(
                    job.jobPostingId()
            );
            List<String> allSkills = skillNamesByPostingId.getOrDefault(
                    job.jobPostingId(),
                    List.of()
            );
            List<String> visibleSkills = allSkills.stream()
                    .limit(3)
                    .toList();

            return new JobListItem(
                    job.jobPostingId(),
                    job.logoUrl(),
                    job.companyName(),
                    job.title(),
                    job.workRegion(),
                    job.careerType(),
                    job.employmentType(),
                    job.categoryName(),
                    visibleSkills,
                    Math.max(allSkills.size() - visibleSkills.size(), 0),
                    viewCount,
                    applicantCount,               // applicantCount
                    newPosting,
                    hotPosting,
                    getDeadlineText(
                            job.applyEndAt(),
                            today
                    ),
                    bookmarked
            );
        });
    }

    private Map<Long, List<String>> getListSkillNames(
            List<JobListQueryItem> jobs
    ) {
        if (jobs.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<String>> result = new LinkedHashMap<>();
        jobPostingSkillRepository.findListSkillsByJobPostingIds(
                        jobs.stream()
                                .map(JobListQueryItem::jobPostingId)
                                .toList()
                )
                .forEach(item -> result.computeIfAbsent(
                        item.jobPostingId(),
                        ignored -> new ArrayList<>()
                ).add(item.skillName()));

        return Collections.unmodifiableMap(result);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private List<String> normalizeRegions(List<String> regions) {
        return normalizeValues(regions);
    }

    private List<String> normalizeValues(List<String> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private List<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null && value > 0)
                .distinct()
                .toList();
    }

    private <T> List<T> valuesOrSentinel(
            List<T> values,
            T sentinel
    ) {
        return values.isEmpty() ? List.of(sentinel) : values;
    }

    private Sort resolveJobListSort(String sortType) {
        if ("deadline".equals(sortType)) {
            return Sort.by(
                    Sort.Order.asc("applyEndAt").nullsLast(),
                    Sort.Order.desc("jobPostingId")
            );
        }

        if ("views".equals(sortType)) {
            return Sort.by(
                    Sort.Order.desc("viewCount"),
                    Sort.Order.desc("jobPostingId")
            );
        }

        return Sort.by(
                Sort.Order.desc("publishedAt"),
                Sort.Order.desc("jobPostingId")
        );
    }

    private String getCategoryText(Long categoryId) {
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
        Map<Long, String> names = skillRepository.findAllById(skillIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        Skill::getSkillId,
                        Skill::getSkillName
                ));
        return skillIds.stream()
                .map(names::get)
                .filter(name -> name != null)
                .toList();
    }

    private String getCareerText(JobPosting posting) {
        String type = display(posting.getCareerType());
        Integer minimum = posting.getMinExperienceYears();
        Integer maximum = posting.getMaxExperienceYears();
        if (minimum == null && maximum == null) return type;
        if (maximum == null) return type + " - " + minimum + "년 이상";
        if (minimum == null) return type + " - " + maximum + "년 이하";
        return type + " - " + minimum + "~" + maximum + "년";
    }

    private String getSalaryText(JobPosting posting) {
        Integer minimum = posting.getSalaryMin();
        Integer maximum = posting.getSalaryMax();
        if (minimum == null && maximum == null) {
            return display(posting.getSalaryText());
        }
        if (maximum == null) {
            return String.format("연봉 - %,d만원 이상", minimum);
        }
        if (minimum == null) {
            return String.format("연봉 - %,d만원 이하", maximum);
        }
        return String.format("연봉 - %,d~%,d만원", minimum, maximum);
    }

    private List<String> parseBenefits(String benefitsJson) {
        if (benefitsJson == null || benefitsJson.isBlank()) return List.of();
        String content = benefitsJson.trim();
        if (content.length() < 2) return List.of();
        return Arrays.stream(content.substring(1, content.length() - 1).split(","))
                .map(String::trim)
                .map(value -> value.replaceAll("^\"|\"$", ""))
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String joinAddress(String address1, String address2) {
        return java.util.stream.Stream.of(address1, address2)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private String display(String value) {
        return value == null || value.isBlank() ? "미입력" : value;
    }

    private String getDeadlineText(
            LocalDateTime applyEndAt,
            LocalDate today
    ) {
        if (applyEndAt == null) {
            return "상시";
        }

        long days = ChronoUnit.DAYS.between(
                today,
                applyEndAt.toLocalDate()
        );

        if (days < 0) {
            return "마감";
        }

        if (days == 0) {
            return "D-Day";
        }

        return "D-" + days;
    }
}
