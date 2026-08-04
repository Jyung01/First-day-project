package kr.co.firstdayproject.service.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.co.firstdayproject.dto.job.JobCategoryGroup;
import kr.co.firstdayproject.entity.job.JobCategory;
import kr.co.firstdayproject.repository.job.JobCategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobCategoryRepository jobCategoryRepository;

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
}
