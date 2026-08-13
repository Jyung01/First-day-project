package kr.co.firstdayproject.service.corp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.service.ai.JobPostingEmbeddingSyncEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CorpJobManagementServiceTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CorpJobManagementService managementService;

    @Test
    void deletingAPostingPublishesADeleteEmbeddingEvent() {
        JobPosting posting = JobPosting.builder()
            .jobPostingId(10L)
            .companyId(5L)
            .status("모집예정")
            .build();
        when(jobPostingRepository.findByJobPostingIdAndCompanyId(10L, 5L))
            .thenReturn(Optional.of(posting));

        managementService.deleteJobPosting(5L, 10L);

        assertThat(posting.getStatus()).isEqualTo("삭제");

        ArgumentCaptor<JobPostingEmbeddingSyncEvent> captor =
            ArgumentCaptor.forClass(JobPostingEmbeddingSyncEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().jobPostingId()).isEqualTo(10L);
        assertThat(captor.getValue().shouldEmbed()).isFalse();
    }

    @Test
    void deletingANonDeletablePostingNeverPublishesAnEvent() {
        JobPosting posting = JobPosting.builder()
            .jobPostingId(11L)
            .companyId(5L)
            .status("모집중")
            .build();
        when(jobPostingRepository.findByJobPostingIdAndCompanyId(11L, 5L))
            .thenReturn(Optional.of(posting));

        assertThatThrownBy(() -> managementService.deleteJobPosting(5L, 11L))
            .isInstanceOf(IllegalArgumentException.class);

        verify(eventPublisher, never())
            .publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void closingAPostingDoesNotPublishAnEmbeddingEventBecauseClosedVectorsAreKept() {
        JobPosting posting = JobPosting.builder()
            .jobPostingId(12L)
            .companyId(5L)
            .status("모집중")
            .build();
        when(jobPostingRepository.findByJobPostingIdAndCompanyId(12L, 5L))
            .thenReturn(Optional.of(posting));

        managementService.closeJobPosting(5L, 12L);

        assertThat(posting.getStatus()).isEqualTo("마감");
        verify(eventPublisher, never())
            .publishEvent(org.mockito.ArgumentMatchers.any());
    }
}
