package kr.co.firstdayproject.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.co.firstdayproject.dto.report.JobPostingReportRequest;
import kr.co.firstdayproject.entity.report.Report;
import kr.co.firstdayproject.exception.DuplicateReportException;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.report.ReportRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(
                reportRepository,
                jobPostingRepository
        );
    }

    @Test
    void 개인회원이_채용공고를_신고한다() {
        Authentication authentication = personalAuthentication(7L);
        JobPostingReportRequest request = new JobPostingReportRequest(
                "허위 정보·사기 의심",
                "공고 내용과 실제 안내가 다릅니다."
        );

        when(jobPostingRepository.existsById(10L)).thenReturn(true);
        when(reportRepository
                .existsByReporterUserIdAndTargetTypeAndTargetId(
                        7L,
                        "채용공고",
                        10L
                )).thenReturn(false);
        when(reportRepository.saveAndFlush(any(Report.class)))
                .thenAnswer(invocation -> {
                    Report report = invocation.getArgument(0);
                    report.setReportId(100L);
                    return report;
                });

        Long reportId = reportService.reportJobPosting(
                10L,
                request,
                authentication
        );

        ArgumentCaptor<Report> captor =
                ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(captor.capture());

        Report savedReport = captor.getValue();
        assertThat(reportId).isEqualTo(100L);
        assertThat(savedReport.getReporterUserId()).isEqualTo(7L);
        assertThat(savedReport.getTargetType()).isEqualTo("채용공고");
        assertThat(savedReport.getTargetId()).isEqualTo(10L);
        assertThat(savedReport.getReasonCode())
                .isEqualTo("허위 정보·사기 의심");
        assertThat(savedReport.getStatus()).isEqualTo("미처리");
        assertThat(savedReport.getCreatedAt()).isNotNull();
    }

    @Test
    void 동일한_채용공고는_중복_신고할_수_없다() {
        Authentication authentication = personalAuthentication(7L);
        JobPostingReportRequest request = new JobPostingReportRequest(
                "개인정보 노출",
                "개인정보가 포함되어 있습니다."
        );

        when(jobPostingRepository.existsById(10L)).thenReturn(true);
        when(reportRepository
                .existsByReporterUserIdAndTargetTypeAndTargetId(
                        7L,
                        "채용공고",
                        10L
                )).thenReturn(true);

        assertThatThrownBy(() -> reportService.reportJobPosting(
                10L,
                request,
                authentication
        )).isInstanceOf(DuplicateReportException.class)
                .hasMessage("이미 신고한 채용공고입니다.");

        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 존재하지_않는_채용공고는_신고할_수_없다() {
        Authentication authentication = personalAuthentication(7L);
        JobPostingReportRequest request = new JobPostingReportRequest(
                "개인정보 노출",
                "상세 내용"
        );

        when(jobPostingRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> reportService.reportJobPosting(
                10L,
                request,
                authentication
        )).isInstanceOf(ResourceNotFoundException.class);

        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 개인회원이_아니면_신고할_수_없다() {
        Authentication authentication = mock(Authentication.class);
        JobPostingReportRequest request = new JobPostingReportRequest(
                "개인정보 노출",
                "상세 내용"
        );

        when(authentication.isAuthenticated()).thenReturn(false);

        assertThatThrownBy(() -> reportService.reportJobPosting(
                10L,
                request,
                authentication
        )).isInstanceOf(AccessDeniedException.class);

        verify(jobPostingRepository, never()).existsById(any());
        verify(reportRepository, never()).saveAndFlush(any());
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
