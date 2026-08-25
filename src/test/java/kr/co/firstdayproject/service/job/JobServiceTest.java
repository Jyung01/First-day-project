package kr.co.firstdayproject.service.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kr.co.firstdayproject.dto.job.MainJobListItem;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.dto.job.JobCategoryGroup;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterRepository;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.JobPostingSkillRepository;
import kr.co.firstdayproject.repository.job.UserDesiredJobRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobCategoryRepository jobCategoryRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserDesiredJobRepository userDesiredJobRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private CoverLetterRepository coverLetterRepository;

    @Mock
    private JobPostingSkillRepository jobPostingSkillRepository;

    @InjectMocks
    private JobService jobService;

    @Test
    void groupsActiveSecondDepthJobsUnderTheirParents() {
        when(jobCategoryRepository
                .findAllByIsActiveTrueAndDepthOrderByDisplayOrderAscJobCategoryIdAsc(1))
                .thenReturn(List.of(
                        category(1L, null, "개발", 1),
                        category(2L, null, "디자인", 1),
                        category(3L, null, "하위 직무 없음", 1)
                ));
        when(jobCategoryRepository
                .findAllByIsActiveTrueAndDepthOrderByDisplayOrderAscJobCategoryIdAsc(2))
                .thenReturn(List.of(
                        category(11L, 1L, "백엔드 개발", 2),
                        category(12L, 1L, "프론트엔드 개발", 2),
                        category(21L, 2L, "UI·UX 디자인", 2),
                        category(99L, 999L, "부모 없음", 2)
                ));

        List<JobCategoryGroup> result = jobService.getActiveJobCategoryGroups();

        assertThat(result)
                .extracting(JobCategoryGroup::categoryName)
                .containsExactly("개발", "디자인");
        assertThat(result.getFirst().children())
                .extracting("jobCategoryId", "categoryName")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(11L, "백엔드 개발"),
                        org.assertj.core.groups.Tuple.tuple(12L, "프론트엔드 개발")
                );
    }

    @Test
    void excludesSuspendedCompanyJobsFromPersonalizedRecommendations() {
        Long userId = 1L;
        JobPosting normalCompanyJob = jobPosting(101L, 10L, "정상 기업 공고");
        JobPosting suspendedCompanyJob = jobPosting(102L, 20L, "이용정지 기업 공고");

        when(userDesiredJobRepository.findJobCategoriesByUserId(userId))
                .thenReturn(List.of());
        when(resumeRepository
                .findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId))
                .thenReturn(Optional.empty());
        when(coverLetterRepository
                .findFirstByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId))
                .thenReturn(Optional.empty());
        when(jobPostingRepository
                .findByStatusOrderByPublishedAtDescJobPostingIdDesc(
                        org.mockito.ArgumentMatchers.eq("모집중"), any(Pageable.class)
                ))
                .thenReturn(List.of(normalCompanyJob, suspendedCompanyJob));
        when(companyRepository.findAllById(List.of(10L, 20L)))
                .thenReturn(List.of(
                        company(10L, "정상 기업", "정상"),
                        company(20L, "이용정지 기업", "이용정지")
                ));
        when(jobCategoryRepository.findAllById(List.of()))
                .thenReturn(List.of());
        when(jobPostingSkillRepository.findAllByIdJobPostingIdIn(List.of(101L)))
                .thenReturn(List.of());

        List<MainJobListItem> result = jobService
                .getPersonalizedJobPostingList(userId, Map.of());

        assertThat(result)
                .extracting(MainJobListItem::jobPostingId)
                .containsExactly(101L);
        assertThat(result)
                .extracting(MainJobListItem::companyName)
                .containsExactly("정상 기업");
    }

    private JobCategory category(
            Long id,
            Long parentId,
            String name,
            int depth
    ) {
        return JobCategory.builder()
                .jobCategoryId(id)
                .parentId(parentId)
                .categoryName(name)
                .depth(depth)
                .isActive(true)
                .build();
    }

    private JobPosting jobPosting(Long id, Long companyId, String title) {
        return JobPosting.builder()
                .jobPostingId(id)
                .companyId(companyId)
                .title(title)
                .status("모집중")
                .publishedAt(LocalDateTime.of(2026, 8, 20, 9, 0))
                .viewCount(0L)
                .build();
    }

    private Company company(Long id, String name, String companyStatus) {
        return Company.builder()
                .companyId(id)
                .companyName(name)
                .approvalStatus("승인")
                .companyStatus(companyStatus)
                .build();
    }
}
