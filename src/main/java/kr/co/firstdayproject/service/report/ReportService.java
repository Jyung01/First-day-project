package kr.co.firstdayproject.service.report;

import java.time.LocalDateTime;
import java.util.Set;
import kr.co.firstdayproject.dto.report.JobPostingReportRequest;
import kr.co.firstdayproject.entity.report.Report;
import kr.co.firstdayproject.exception.DuplicateReportException;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.report.ReportRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String PERSONAL_ROLE = "ROLE_PERSONAL";
    private static final String JOB_POSTING_TARGET = "채용공고";
    private static final String UNHANDLED_STATUS = "미처리";
    private static final Set<String> ALLOWED_REASONS = Set.of(
            "허위 정보·사기 의심",
            "개인정보 노출",
            "욕설·비방·차별 표현",
            "광고·스팸·중복 콘텐츠",
            "기타 운영정책 위반"
    );

    private final ReportRepository reportRepository;
    private final JobPostingRepository jobPostingRepository;

    @Transactional
    public Long reportJobPosting(
            Long jobPostingId,
            JobPostingReportRequest request,
            Authentication authentication
    ) {
        Long reporterUserId = getPersonalUserId(authentication);

        validateJobPosting(jobPostingId);
        validateReason(request.reasonCode());
        validateDuplicate(reporterUserId, jobPostingId);

        Report report = Report.builder()
                .reporterUserId(reporterUserId)
                .targetType(JOB_POSTING_TARGET)
                .targetId(jobPostingId)
                .reasonCode(request.reasonCode())
                .detail(request.detail().trim())
                .status(UNHANDLED_STATUS)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            return reportRepository.saveAndFlush(report).getReportId();
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateReportException(
                    "이미 신고한 채용공고입니다."
            );
        }
    }

    private void validateJobPosting(Long jobPostingId) {
        if (jobPostingId == null
                || !jobPostingRepository.existsById(jobPostingId)) {
            throw new ResourceNotFoundException(
                    "채용공고를 찾을 수 없습니다."
            );
        }
    }

    private void validateReason(String reasonCode) {
        if (!ALLOWED_REASONS.contains(reasonCode)) {
            throw new IllegalArgumentException(
                    "올바른 신고 사유를 선택해주세요."
            );
        }
    }

    private void validateDuplicate(
            Long reporterUserId,
            Long jobPostingId
    ) {
        boolean duplicate = reportRepository
                .existsByReporterUserIdAndTargetTypeAndTargetId(
                        reporterUserId,
                        JOB_POSTING_TARGET,
                        jobPostingId
                );

        if (duplicate) {
            throw new DuplicateReportException(
                    "이미 신고한 채용공고입니다."
            );
        }
    }

    private Long getPersonalUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)
                || authentication.getAuthorities().stream()
                .noneMatch(authority -> PERSONAL_ROLE.equals(
                        authority.getAuthority()
                ))) {
            throw new AccessDeniedException(
                    "개인회원 로그인이 필요한 기능입니다."
            );
        }

        return userDetails.getUserId();
    }
}
