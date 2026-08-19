package kr.co.firstdayproject.service.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import kr.co.firstdayproject.dto.job.JobApplicationDocuments;
import kr.co.firstdayproject.entity.application.Application;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.repository.coverletter.CoverLetterRepository;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.my.CoverLetterService;
import kr.co.firstdayproject.service.my.ResumeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private CoverLetterRepository coverLetterRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ResumeService resumeService;

    @Mock
    private CoverLetterService coverLetterService;

    @Mock
    private ApplicationSnapshotService applicationSnapshotService;

    @InjectMocks
    private JobApplicationService jobApplicationService;

    @Test
    void 로그인한_개인회원의_지원_문서를_조회한다() {
        Authentication authentication = personalAuthentication(7L);
        LocalDateTime resumeUpdatedAt =
                LocalDateTime.of(2026, 8, 10, 10, 30);
        LocalDateTime coverLetterUpdatedAt =
                LocalDateTime.of(2026, 8, 9, 15, 20);

        Resume resume = Resume.builder()
                .resumeId(11L)
                .title("백엔드 개발자 이력서")
                .updatedAt(resumeUpdatedAt)
                .build();
        CoverLetter coverLetter = CoverLetter.builder()
                .coverLetterId(21L)
                .title("성장 경험과 지원 동기")
                .updatedAt(coverLetterUpdatedAt)
                .build();

        when(resumeRepository
                .findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(7L))
                .thenReturn(List.of(resume));
        when(coverLetterRepository
                .findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(7L))
                .thenReturn(List.of(coverLetter));

        JobApplicationDocuments documents =
                jobApplicationService.getMyDocuments(authentication);

        assertThat(documents.resumes()).hasSize(1);
        assertThat(documents.resumes().get(0).id()).isEqualTo(11L);
        assertThat(documents.resumes().get(0).title())
                .isEqualTo("백엔드 개발자 이력서");
        assertThat(documents.resumes().get(0).updatedAt())
                .isEqualTo(resumeUpdatedAt);
        assertThat(documents.coverLetters()).hasSize(1);
        assertThat(documents.coverLetters().get(0).id()).isEqualTo(21L);
        assertThat(documents.coverLetters().get(0).updatedAt())
                .isEqualTo(coverLetterUpdatedAt);

        verify(resumeRepository)
                .findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(7L);
        verify(coverLetterRepository)
                .findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(7L);
    }

    @Test
    void 개인회원이_아니면_지원_문서를_조회할_수_없다() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThatThrownBy(() ->
                jobApplicationService.getMyDocuments(authentication)
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void 동일한_공고의_기존_지원_여부를_확인한다() {
        Authentication authentication = personalAuthentication(7L);
        when(applicationRepository
                .existsByApplicantUserIdAndJobPostingIdAndCurrentStatusNot(
                        7L,
                        10L,
                        "지원취소"
                ))
                .thenReturn(true);

        boolean alreadyApplied = jobApplicationService.hasApplied(
                10L,
                authentication
        );

        assertThat(alreadyApplied).isTrue();
        verify(applicationRepository)
                .existsByApplicantUserIdAndJobPostingIdAndCurrentStatusNot(
                        7L,
                        10L,
                        "지원취소"
                );
    }

    @Test
    void 지원취소한_공고는_다시_지원할_수_있다() {
        Authentication authentication = personalAuthentication(7L);
        when(applicationRepository
                .existsByApplicantUserIdAndJobPostingIdAndCurrentStatusNot(
                        7L,
                        10L,
                        "지원취소"
                ))
                .thenReturn(false);

        boolean alreadyApplied = jobApplicationService.hasApplied(
                10L,
                authentication
        );

        assertThat(alreadyApplied).isFalse();
    }

    @Test
    void 선택한_이력서와_자기소개서_스냅샷으로_지원한다() {
        Authentication authentication = personalAuthentication(7L);
        JobPosting posting = JobPosting.builder()
                .jobPostingId(10L)
                .companyId(30L)
                .status("모집중")
                .applyStartAt(LocalDateTime.now().minusDays(1))
                .applyEndAt(LocalDateTime.now().plusDays(7))
                .build();
        Company company = Company.builder()
                .companyId(30L)
                .approvalStatus("승인")
                .companyStatus("정상")
                .build();
        Resume resume = Resume.builder()
                .resumeId(11L)
                .userId(7L)
                .build();
        CoverLetter coverLetter = CoverLetter.builder()
                .coverLetterId(21L)
                .userId(7L)
                .build();

        when(jobPostingRepository.findById(10L))
                .thenReturn(java.util.Optional.of(posting));
        when(companyRepository.findById(30L))
                .thenReturn(java.util.Optional.of(company));
        when(applicationRepository
                .existsByApplicantUserIdAndJobPostingIdAndCurrentStatusNot(
                        7L,
                        10L,
                        "지원취소"
                ))
                .thenReturn(false);
        when(resumeService.getMine(11L, 7L)).thenReturn(resume);
        when(coverLetterService.getMine(21L, 7L))
                .thenReturn(coverLetter);
        when(applicationSnapshotService.createResumeSnapshot(resume))
                .thenReturn("{\"resume\":true}");
        when(applicationSnapshotService.createCoverLetterSnapshot(
                coverLetter,
                7L
        )).thenReturn("{\"coverLetter\":true}");
        when(applicationRepository.saveAndFlush(any(Application.class)))
                .thenAnswer(invocation -> {
                    Application application = invocation.getArgument(0);
                    application.setApplicationId(100L);
                    return application;
                });

        Long applicationId = jobApplicationService.apply(
                10L,
                11L,
                21L,
                authentication
        );

        ArgumentCaptor<Application> captor =
                ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).saveAndFlush(captor.capture());

        Application savedApplication = captor.getValue();
        assertThat(applicationId).isEqualTo(100L);
        assertThat(savedApplication.getApplicantUserId()).isEqualTo(7L);
        assertThat(savedApplication.getJobPostingId()).isEqualTo(10L);
        assertThat(savedApplication.getResumeId()).isEqualTo(11L);
        assertThat(savedApplication.getResumeSnapshotJson())
                .isEqualTo("{\"resume\":true}");
        assertThat(savedApplication.getCoverLetterId()).isEqualTo(21L);
        assertThat(savedApplication.getCoverLetterSnapshotJson())
                .isEqualTo("{\"coverLetter\":true}");
        assertThat(savedApplication.getCurrentStatus())
                .isEqualTo("지원완료");
        assertThat(savedApplication.getAppliedAt()).isNotNull();
    }

    private Authentication personalAuthentication(Long userId) {
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        doReturn(List.of(
                new SimpleGrantedAuthority("ROLE_PERSONAL")
        )).when(authentication).getAuthorities();
        when(userDetails.getUserId()).thenReturn(userId);

        return authentication;
    }
}
