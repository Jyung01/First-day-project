package kr.co.firstdayproject.service.corp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import kr.co.firstdayproject.dto.corp.job.JobPostingCreateRequest;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.JobPostingSkillRepository;
import kr.co.firstdayproject.service.ai.JobPostingEmbeddingSyncEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CorpJobServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private JobPostingSkillRepository jobPostingSkillRepository;

    @Mock
    private CorpJobRequestValidator requestValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CorpJobService corpJobService;

    @Test
    void publishingOnCreatePublishesAnEmbeddableEmbeddingEvent() {
        when(companyRepository.findById(5L)).thenReturn(Optional.of(
            Company.builder()
                .companyId(5L)
                .approvalStatus("승인")
                .companyStatus("정상")
                .build()
        ));
        when(requestValidator.normalizeAndValidateSkills(any()))
            .thenReturn(List.of());
        when(jobPostingRepository.save(any(JobPosting.class)))
            .thenAnswer(invocation -> {
                JobPosting posting = invocation.getArgument(0);
                posting.setJobPostingId(100L);
                return posting;
            });

        JobPostingCreateRequest request = new JobPostingCreateRequest();
        request.setTitle("백엔드 신입 개발자");
        request.setSubmitType("PUBLISH");
        request.setApplyStartDate(LocalDate.now());
        request.setApplyEndDate(LocalDate.now().plusDays(30));

        Long jobPostingId = corpJobService.createJobPosting(5L, request);

        assertThat(jobPostingId).isEqualTo(100L);

        ArgumentCaptor<JobPostingEmbeddingSyncEvent> captor =
            ArgumentCaptor.forClass(JobPostingEmbeddingSyncEvent.class);
        org.mockito.Mockito.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().jobPostingId()).isEqualTo(100L);
        assertThat(captor.getValue().shouldEmbed()).isTrue();
    }

    @Test
    void savingAsDraftOnCreatePublishesANonEmbeddableEvent() {
        when(companyRepository.findById(5L)).thenReturn(Optional.of(
            Company.builder()
                .companyId(5L)
                .approvalStatus("승인")
                .companyStatus("정상")
                .build()
        ));
        when(requestValidator.normalizeAndValidateSkills(any()))
            .thenReturn(List.of());
        when(jobPostingRepository.save(any(JobPosting.class)))
            .thenAnswer(invocation -> {
                JobPosting posting = invocation.getArgument(0);
                posting.setJobPostingId(101L);
                return posting;
            });

        JobPostingCreateRequest request = new JobPostingCreateRequest();
        request.setTitle("임시저장 공고");
        request.setSubmitType("DRAFT");

        corpJobService.createJobPosting(5L, request);

        ArgumentCaptor<JobPostingEmbeddingSyncEvent> captor =
            ArgumentCaptor.forClass(JobPostingEmbeddingSyncEvent.class);
        org.mockito.Mockito.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().jobPostingId()).isEqualTo(101L);
        assertThat(captor.getValue().shouldEmbed()).isFalse();
    }

    @Test
    void requestingReviewOnUpdatePublishesANonEmbeddableEvent() {
        JobPosting existing = JobPosting.builder()
            .jobPostingId(200L)
            .companyId(5L)
            .status("숨김")
            .title("기존 공고")
            .build();
        when(jobPostingRepository.findByJobPostingIdAndCompanyId(200L, 5L))
            .thenReturn(Optional.of(existing));
        when(companyRepository.findById(5L)).thenReturn(Optional.of(
            Company.builder()
                .companyId(5L)
                .approvalStatus("승인")
                .companyStatus("정상")
                .build()
        ));
        when(requestValidator.normalizeAndValidateSkills(any()))
            .thenReturn(List.of());

        JobPostingCreateRequest request = new JobPostingCreateRequest();
        request.setTitle("숨김 해제 재검토 요청");
        request.setSubmitType("REVIEW");
        request.setApplyStartDate(LocalDate.now());
        request.setApplyEndDate(LocalDate.now().plusDays(30));

        corpJobService.updateJobPosting(5L, 200L, request);

        assertThat(existing.getStatus()).isEqualTo("재검토요청");

        ArgumentCaptor<JobPostingEmbeddingSyncEvent> captor =
            ArgumentCaptor.forClass(JobPostingEmbeddingSyncEvent.class);
        org.mockito.Mockito.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().jobPostingId()).isEqualTo(200L);
        assertThat(captor.getValue().shouldEmbed()).isFalse();
    }

    @Test
    void publishingADraftOnUpdatePublishesAnEmbeddableEvent() {
        JobPosting existing = JobPosting.builder()
            .jobPostingId(201L)
            .companyId(5L)
            .status("임시저장")
            .title("기존 임시저장 공고")
            .build();
        when(jobPostingRepository.findByJobPostingIdAndCompanyId(201L, 5L))
            .thenReturn(Optional.of(existing));
        when(companyRepository.findById(5L)).thenReturn(Optional.of(
            Company.builder()
                .companyId(5L)
                .approvalStatus("승인")
                .companyStatus("정상")
                .build()
        ));
        when(requestValidator.normalizeAndValidateSkills(any()))
            .thenReturn(List.of());

        JobPostingCreateRequest request = new JobPostingCreateRequest();
        request.setTitle("발행할 공고");
        request.setSubmitType("PUBLISH");
        request.setApplyStartDate(LocalDate.now());
        request.setApplyEndDate(LocalDate.now().plusDays(30));

        corpJobService.updateJobPosting(5L, 201L, request);

        assertThat(existing.getStatus()).isEqualTo("모집중");

        ArgumentCaptor<JobPostingEmbeddingSyncEvent> captor =
            ArgumentCaptor.forClass(JobPostingEmbeddingSyncEvent.class);
        org.mockito.Mockito.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().jobPostingId()).isEqualTo(201L);
        assertThat(captor.getValue().shouldEmbed()).isTrue();
    }
}
