package kr.co.firstdayproject.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import kr.co.firstdayproject.service.ai.JobPostingEmbeddingSyncEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AdminJobModerationServiceTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminJobModerationService moderationService;

    @Test
    void hidingAPostingPublishesADeleteEmbeddingEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
            User.builder().userId(1L).userType("관리자").build()
        ));
        JobPosting posting = JobPosting.builder()
            .jobPostingId(50L)
            .status("모집중")
            .build();
        when(jobPostingRepository.findById(50L))
            .thenReturn(Optional.of(posting));

        moderationService.hideJobPosting(1L, 50L, "부적절한 표현 포함");

        assertThat(posting.getStatus()).isEqualTo("숨김");

        ArgumentCaptor<JobPostingEmbeddingSyncEvent> captor =
            ArgumentCaptor.forClass(JobPostingEmbeddingSyncEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().jobPostingId()).isEqualTo(50L);
        assertThat(captor.getValue().shouldEmbed()).isFalse();
    }

    @Test
    void releasingAnExpiredPostingClosesItWithoutPublishingAnEmbeddingEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
            User.builder().userId(1L).userType("관리자").build()
        ));
        JobPosting posting = JobPosting.builder()
            .jobPostingId(51L)
            .companyId(9L)
            .status("재검토요청")
            .applyEndAt(LocalDateTime.now().minusDays(1))
            .build();
        when(jobPostingRepository.findById(51L))
            .thenReturn(Optional.of(posting));

        String result = moderationService.releaseJobPosting(1L, 51L);

        assertThat(result).isEqualTo("마감");
        assertThat(posting.getStatus()).isEqualTo("마감");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void releasingANonExpiredPostingRepublishesAnEmbeddableEvent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(
            User.builder().userId(1L).userType("관리자").build()
        ));
        JobPosting posting = JobPosting.builder()
            .jobPostingId(52L)
            .companyId(9L)
            .status("재검토요청")
            .applyStartAt(LocalDateTime.now().minusDays(1))
            .applyEndAt(LocalDateTime.now().plusDays(10))
            .build();
        when(jobPostingRepository.findById(52L))
            .thenReturn(Optional.of(posting));
        when(companyRepository.findById(9L)).thenReturn(Optional.of(
            Company.builder()
                .companyId(9L)
                .approvalStatus("승인")
                .companyStatus("정상")
                .build()
        ));

        String result = moderationService.releaseJobPosting(1L, 52L);

        assertThat(result).isEqualTo("모집중");

        ArgumentCaptor<JobPostingEmbeddingSyncEvent> captor =
            ArgumentCaptor.forClass(JobPostingEmbeddingSyncEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().jobPostingId()).isEqualTo(52L);
        assertThat(captor.getValue().shouldEmbed()).isTrue();
    }
}
