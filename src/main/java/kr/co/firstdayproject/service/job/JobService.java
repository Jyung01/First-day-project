package kr.co.firstdayproject.service.job;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kr.co.firstdayproject.dto.job.*;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    private final JobPostingRepository jobPostingRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final SavedJobService savedJobService;

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

    public Page<JobListItem> getRecruitingJobPostingList(
            String keyword,
            Long parentCategoryId,
            Long categoryId,
            int page,
            Authentication authentication
    ) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                JOB_LIST_PAGE_SIZE
        );

        Page<JobListQueryItem> queryPage =
                jobPostingRepository.findRecruitingJobPostings(
                        normalizeKeyword(keyword),
                        parentCategoryId,
                        categoryId,
                        pageRequest
                );

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

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

            boolean bookmarked =
                    savedJobService.isSavedJob(
                            job.jobPostingId(),
                            authentication
                    );

            return new JobListItem(
                    job.jobPostingId(),
                    job.logoUrl(),
                    job.companyName(),
                    job.title(),
                    job.workRegion(),
                    job.careerType(),
                    job.employmentType(),
                    job.categoryName(),
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
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
