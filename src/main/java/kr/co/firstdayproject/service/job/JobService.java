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
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.entity.resume.ResumeCareer;
import kr.co.firstdayproject.entity.resume.ResumeSkill;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.SkillRepository;
import kr.co.firstdayproject.repository.job.JobPostingSkillRepository;
import kr.co.firstdayproject.repository.job.UserDesiredJobRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import kr.co.firstdayproject.repository.resume.ResumeCareerRepository;
import kr.co.firstdayproject.repository.resume.ResumeSkillRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterItemRepository;
import kr.co.firstdayproject.service.ai.PersonalizedJobRecommendationService;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
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
    private static final int JOB_PICKER_PAGE_SIZE = 10;

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final SkillRepository skillRepository;
    private final SavedJobService savedJobService;
    private final CompanyRepository companyRepository;
    private final JobPostingSkillRepository jobPostingSkillRepository;
    private final UserDesiredJobRepository userDesiredJobRepository;
    private final ResumeRepository resumeRepository;
    private final ResumeCareerRepository resumeCareerRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterItemRepository coverLetterItemRepository;
    private final java.util.Optional<PersonalizedJobRecommendationService> personalizedJobRecommendationService;

    @Transactional
    public JobDetailView getViewableJobPosting(
            Long jobPostingId,
            Authentication authentication
    ) {
        JobPosting posting = jobPostingRepository
                .findByJobPostingIdAndStatusIn(
                        jobPostingId,
                        List.of("모집중", "마감")
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "조회할 수 없는 채용공고입니다."
                ));

        boolean recruiting = "모집중".equals(posting.getStatus());
        boolean savedClosedPosting = "마감".equals(posting.getStatus())
                && savedJobService.isSavedJob(
                        jobPostingId,
                        authentication
                );

        if (!recruiting && !savedClosedPosting) {
            throw new ResourceNotFoundException(
                    "조회할 수 없는 채용공고입니다."
            );
        }

        Company company = companyRepository.findById(posting.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "기업 정보를 찾을 수 없습니다."
                ));
        if (!"승인".equals(company.getApprovalStatus())
                || !"정상".equals(company.getCompanyStatus())) {
            throw new ResourceNotFoundException("조회할 수 없는 채용공고입니다.");
        }

        posting.setViewCount(
                (posting.getViewCount() == null ? 0L : posting.getViewCount()) + 1
        );
        long applicantCount = applicationRepository
                .countByJobPostingIdAndCurrentStatusNotIn(
                        jobPostingId,
                        List.of("지원취소", "채용종료")
                );

        LocalDateTime now = LocalDateTime.now();
        boolean availableCompany = "승인".equals(company.getApprovalStatus())
                && "정상".equals(company.getCompanyStatus());
        boolean acceptingApplications = availableCompany
                && recruiting
                && (posting.getApplyStartAt() == null
                    || !posting.getApplyStartAt().isAfter(now))
                && (posting.getApplyEndAt() == null
                    || !posting.getApplyEndAt().isBefore(now));
        String applicationUnavailableMessage = availableCompany
                ? "현재 지원할 수 없는 채용공고입니다.\n"
                    + "공고 상태를 확인한 후 다시 시도해주세요."
                : "기업 계정 이용 제한으로 신규 입사지원이 "
                    + "일시 중지되었습니다.";

        return new JobDetailView(
                posting.getJobPostingId(),
                posting.getStatus(),
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
                getDetailDeadlineText(posting, LocalDate.now()),
                acceptingApplications,
                applicationUnavailableMessage,
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

    public List<MainJobListItem> getPersonalizedJobPostingList(Long userId) {
        return getPersonalizedJobPostingList(userId, getPersonalizedJobMatchScores(userId));
    }

    public List<MainJobListItem> getPersonalizedJobPostingList(
            Long userId,
            Map<Long, Integer> semanticScores
    ) {
        if (userId == null) {
            return List.of();
        }

        List<Long> desiredCategoryIds = userDesiredJobRepository
                .findJobCategoriesByUserId(userId)
                .stream()
                .map(JobCategory::getJobCategoryId)
                .distinct()
                .toList();

        Resume resume = resumeRepository
                .findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .orElse(null);
        Set<Long> resumeSkillIds = resume == null ? Set.of() : resumeSkillRepository
                .findByIdResumeIdOrderByDisplayOrderAsc(resume.getResumeId())
                .stream()
                .map(ResumeSkill::getId)
                .map(id -> id.getSkillId())
                .collect(java.util.stream.Collectors.toSet());
        int careerMonths = resume == null ? 0 : resumeCareerRepository
                .findByResumeIdOrderByDisplayOrderAsc(resume.getResumeId())
                .stream()
                .mapToInt(this::careerMonths)
                .sum();
        String profileText = buildProfileText(resume, userId);

        List<JobPosting> candidates = jobPostingRepository
                .findByStatusOrderByPublishedAtDescJobPostingIdDesc(
                        "모집중", PageRequest.of(0, 100)
                );
        if (candidates.isEmpty()) {
            return List.of();
        }

        Map<Long, Company> companies = companyRepository.findAllById(candidates.stream()
                        .map(JobPosting::getCompanyId).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(
                        Company::getCompanyId, company -> company));
        Map<Long, JobCategory> categories = jobCategoryRepository.findAllById(candidates.stream()
                        .map(JobPosting::getJobCategoryId)
                        .filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(
                        JobCategory::getJobCategoryId, category -> category));
        Map<Long, Set<Long>> postingSkills = new java.util.HashMap<>();
        jobPostingSkillRepository.findAllByIdJobPostingIdIn(candidates.stream()
                        .map(JobPosting::getJobPostingId).toList())
                .forEach(skill -> postingSkills.computeIfAbsent(
                                skill.getId().getJobPostingId(), ignored -> new java.util.HashSet<>())
                        .add(skill.getId().getSkillId()));

        return candidates.stream()
                .sorted(java.util.Comparator.comparingInt((JobPosting posting) ->
                                recommendationScore(posting, desiredCategoryIds, resumeSkillIds,
                                        careerMonths, profileText, categories, postingSkills)
                                        + semanticScores.getOrDefault(posting.getJobPostingId(), 0))
                        .reversed()
                        .thenComparing(JobPosting::getPublishedAt,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(JobPosting::getJobPostingId,
                                java.util.Comparator.reverseOrder()))
                .limit(MAIN_JOB_COUNT)
                .map(posting -> new MainJobListItem(
                        posting.getJobPostingId(),
                        companies.get(posting.getCompanyId()) == null ? null
                                : companies.get(posting.getCompanyId()).getLogoUrl(),
                        companies.get(posting.getCompanyId()) == null ? "기업"
                                : companies.get(posting.getCompanyId()).getCompanyName(),
                        posting.getTitle(), posting.getWorkRegion(), posting.getCareerType(),
                        posting.getEmploymentType(),
                        categories.get(posting.getJobCategoryId()) == null ? "직무 미정"
                                : categories.get(posting.getJobCategoryId()).getCategoryName(),
                        posting.getViewCount()))
                .toList();
    }

    public Map<Long, Integer> getPersonalizedJobMatchScores(Long userId) {
        try {
            return personalizedJobRecommendationService
                    .map(service -> service.findSemanticMatchScores(userId))
                    .orElseGet(Map::of);
        } catch (RuntimeException exception) {
            // 추천 API나 벡터 저장소가 일시적으로 사용할 수 없어도 메인은 열려야 한다.
            return Map.of();
        }
    }

    public Map<Long, List<String>> getPersonalizedJobMatchReasons(
            Long userId, List<MainJobListItem> jobs, Map<Long, Integer> semanticScores
    ) {
        Resume resume = resumeRepository
                .findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .orElse(null);
        Set<Long> resumeSkillIds = resume == null ? Set.of() : resumeSkillRepository
                .findByIdResumeIdOrderByDisplayOrderAsc(resume.getResumeId()).stream()
                .map(ResumeSkill::getId).map(id -> id.getSkillId())
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> skillNames = skillRepository.findAllById(resumeSkillIds).stream()
                .collect(java.util.stream.Collectors.toMap(Skill::getSkillId, Skill::getSkillName));
        Map<Long, Set<Long>> postingSkills = new java.util.HashMap<>();
        jobPostingSkillRepository.findAllByIdJobPostingIdIn(jobs.stream()
                        .map(MainJobListItem::jobPostingId).toList())
                .forEach(skill -> postingSkills.computeIfAbsent(skill.getId().getJobPostingId(),
                        ignored -> new java.util.HashSet<>()).add(skill.getId().getSkillId()));

        Map<Long, List<String>> result = new java.util.HashMap<>();
        jobs.forEach(job -> {
            List<String> reasons = new ArrayList<>();
            List<String> matchedSkills = postingSkills.getOrDefault(job.jobPostingId(), Set.of()).stream()
                    .filter(resumeSkillIds::contains).map(skillNames::get)
                    .filter(java.util.Objects::nonNull).limit(2).toList();
            if (!matchedSkills.isEmpty()) reasons.add("보유 스킬 일치: " + String.join(", ", matchedSkills));
            if (semanticScores.containsKey(job.jobPostingId())) reasons.add("이력서·자기소개서 경험과 연관");
            if (resume != null && job.careerType() != null
                    && (job.careerType().equals(resume.getCareerType()) || "경력무관".equals(job.careerType()))) {
                reasons.add("경력 조건과 일치");
            }
            result.put(job.jobPostingId(), reasons);
        });
        return result;
    }

    private int recommendationScore(
            JobPosting posting, List<Long> desiredCategoryIds, Set<Long> resumeSkillIds,
            int careerMonths, String profileText, Map<Long, JobCategory> categories,
            Map<Long, Set<Long>> postingSkills
    ) {
        int score = desiredCategoryIds.contains(posting.getJobCategoryId()) ? 100 : 0;
        score += (int) postingSkills.getOrDefault(posting.getJobPostingId(), Set.of()).stream()
                .filter(resumeSkillIds::contains).count() * 30;
        if ("경력무관".equals(posting.getCareerType())) score += 8;
        if (careerMonths == 0 && "신입".equals(posting.getCareerType())) score += 20;
        if (careerMonths > 0 && "경력".equals(posting.getCareerType())) score += 20;
        int years = careerMonths / 12;
        if (posting.getMinExperienceYears() != null && years >= posting.getMinExperienceYears()) score += 15;
        if (posting.getMaxExperienceYears() != null && years <= posting.getMaxExperienceYears()) score += 5;

        JobCategory category = categories.get(posting.getJobCategoryId());
        if (containsProfileTerm(profileText, posting.getTitle())) score += 12;
        if (category != null && containsProfileTerm(profileText, category.getCategoryName())) score += 12;
        return score;
    }

    private String buildProfileText(Resume resume, Long userId) {
        StringBuilder text = new StringBuilder(resume == null || resume.getSummary() == null
                ? "" : resume.getSummary());
        coverLetterRepository.findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .ifPresent(letter -> coverLetterItemRepository
                        .findByCoverLetterIdOrderByDisplayOrderAsc(letter.getCoverLetterId())
                        .forEach(item -> text.append(' ').append(item.getAnswer())));
        return text.toString().toLowerCase();
    }

    private boolean containsProfileTerm(String profileText, String term) {
        return term != null && term.length() >= 2
                && profileText.contains(term.toLowerCase());
    }

    private int careerMonths(ResumeCareer career) {
        if (career.getStartDate() == null) return 0;
        LocalDate end = Boolean.TRUE.equals(career.getIsCurrent()) || career.getEndDate() == null
                ? LocalDate.now() : career.getEndDate();
        return Math.max(0, (int) ChronoUnit.MONTHS.between(career.getStartDate(), end));
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

    /** 자소서 AI 첨삭 대상 공고 선택 picker에서 쓰는 검색 목록(10건씩) */
    public Page<JobListItem> getJobPostingPickerList(
            String keyword,
            int page,
            Authentication authentication
    ) {
        return getRecruitingJobPostingList(
                keyword,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                JOB_PICKER_PAGE_SIZE,
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
                    applicantCount,
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

    private String getDetailDeadlineText(JobPosting posting, LocalDate today) {
        if ("마감".equals(posting.getStatus())) return "마감";
        if (posting.getApplyEndAt() == null) return "상시채용";
        LocalDate endDate = posting.getApplyEndAt().toLocalDate();
        long days = ChronoUnit.DAYS.between(today, endDate);
        if (days < 0) return "마감";
        if (days == 0) return "오늘 마감";
        return "D-" + days + " (" + endDate + " 마감)";
    }
}
