package kr.co.firstdayproject.service.job;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import kr.co.firstdayproject.dto.job.JobApplicationCompleteView;
import kr.co.firstdayproject.dto.job.JobApplicationConfirmView;
import kr.co.firstdayproject.dto.job.JobApplicationDocuments;
import kr.co.firstdayproject.dto.job.JobApplicationDocuments.DocumentItem;
import kr.co.firstdayproject.entity.application.Application;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.coverletter.CoverLetter;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.entity.resume.Resume;
import kr.co.firstdayproject.exception.DuplicateApplicationException;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.coverletter.CoverLetterRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.resume.ResumeRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import kr.co.firstdayproject.service.my.CoverLetterService;
import kr.co.firstdayproject.service.my.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobApplicationService {

    private static final String PERSONAL_ROLE = "ROLE_PERSONAL";
    private static final String RECRUITING_STATUS = "모집중";
    private static final String APPLIED_STATUS = "지원완료";
    private static final String CANCELLED_STATUS = "지원취소";

    private final ResumeRepository resumeRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final CompanyRepository companyRepository;
    private final ResumeService resumeService;
    private final CoverLetterService coverLetterService;
    private final ApplicationSnapshotService applicationSnapshotService;

    public JobApplicationDocuments getMyDocuments(
            Authentication authentication
    ) {
        Long userId = getPersonalUserId(authentication);

        var resumes = resumeRepository
                .findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .stream()
                .map(resume -> new DocumentItem(
                        resume.getResumeId(),
                        resume.getTitle(),
                        resume.getUpdatedAt()
                ))
                .toList();

        var coverLetters = coverLetterRepository
                .findByUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(userId)
                .stream()
                .map(coverLetter -> new DocumentItem(
                        coverLetter.getCoverLetterId(),
                        coverLetter.getTitle(),
                        coverLetter.getUpdatedAt()
                ))
                .toList();

        return new JobApplicationDocuments(resumes, coverLetters);
    }

    public boolean hasApplied(
            Long jobPostingId,
            Authentication authentication
    ) {
        Long userId = getPersonalUserId(authentication);

        return applicationRepository
                .existsByApplicantUserIdAndJobPostingIdAndCurrentStatusNot(
                        userId,
                        jobPostingId,
                        CANCELLED_STATUS
                );
    }

    public JobApplicationConfirmView getConfirmView(
            Long jobPostingId,
            Long resumeId,
            Long coverLetterId,
            Authentication authentication
    ) {
        Long userId = getPersonalUserId(authentication);
        JobPosting posting = getApplicablePosting(jobPostingId);

        validateDuplicate(userId, jobPostingId);

        Resume resume = resumeService.getMine(resumeId, userId);
        CoverLetter coverLetter = getCoverLetter(
                coverLetterId,
                userId
        );
        Company company = getCompany(posting.getCompanyId());

        return new JobApplicationConfirmView(
                posting.getJobPostingId(),
                company.getCompanyName(),
                company.getLogoUrl(),
                posting.getTitle(),
                formatDeadline(posting.getApplyEndAt()),
                resume.getResumeId(),
                resume.getTitle(),
                resume.getUpdatedAt(),
                coverLetter == null
                        ? null
                        : coverLetter.getCoverLetterId(),
                coverLetter == null
                        ? null
                        : coverLetter.getTitle(),
                coverLetter == null
                        ? null
                        : coverLetter.getUpdatedAt()
        );
    }

    @Transactional
    public Long apply(
            Long jobPostingId,
            Long resumeId,
            Long coverLetterId,
            Authentication authentication
    ) {
        Long userId = getPersonalUserId(authentication);
        getApplicablePosting(jobPostingId);
        validateDuplicate(userId, jobPostingId);

        Resume resume = resumeService.getMine(resumeId, userId);
        CoverLetter coverLetter = getCoverLetter(
                coverLetterId,
                userId
        );
        LocalDateTime now = LocalDateTime.now();

        Application application = Application.builder()
                .jobPostingId(jobPostingId)
                .applicantUserId(userId)
                .resumeId(resume.getResumeId())
                .resumeSnapshotJson(
                        applicationSnapshotService
                                .createResumeSnapshot(resume)
                )
                .coverLetterId(coverLetter == null
                        ? null
                        : coverLetter.getCoverLetterId())
                .coverLetterSnapshotJson(coverLetter == null
                        ? null
                        : applicationSnapshotService
                                .createCoverLetterSnapshot(
                                        coverLetter,
                                        userId
                                ))
                .currentStatus(APPLIED_STATUS)
                .appliedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            return applicationRepository
                    .saveAndFlush(application)
                    .getApplicationId();
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateApplicationException(
                    "이미 지원한 채용공고입니다."
            );
        }
    }

    public JobApplicationCompleteView getCompleteView(
            Long applicationId,
            Authentication authentication
    ) {
        Long userId = getPersonalUserId(authentication);
        Application application = applicationRepository
                .findByApplicationIdAndApplicantUserId(
                        applicationId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "지원 내역을 찾을 수 없습니다."
                ));
        JobPosting posting = jobPostingRepository
                .findById(application.getJobPostingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "채용공고를 찾을 수 없습니다."
                ));
        Company company = getCompany(posting.getCompanyId());

        return new JobApplicationCompleteView(
                application.getApplicationId(),
                company.getCompanyName(),
                posting.getTitle(),
                application.getAppliedAt(),
                application.getCurrentStatus()
        );
    }

    private JobPosting getApplicablePosting(Long jobPostingId) {
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "채용공고를 찾을 수 없습니다."
                ));
        LocalDateTime now = LocalDateTime.now();

        if (!RECRUITING_STATUS.equals(posting.getStatus())) {
            throw new IllegalArgumentException(
                    "현재 지원할 수 없는 채용공고입니다."
            );
        }

        Company company = getCompany(posting.getCompanyId());
        if (!"승인".equals(company.getApprovalStatus())
                || !"정상".equals(company.getCompanyStatus())) {
            throw new IllegalArgumentException(
                    "기업 계정 이용 제한으로 신규 입사지원이 "
                            + "일시 중지되었습니다."
            );
        }

        if (posting.getApplyStartAt() != null
                && posting.getApplyStartAt().isAfter(now)) {
            throw new IllegalArgumentException(
                    "아직 접수가 시작되지 않은 채용공고입니다."
            );
        }
        if (posting.getApplyEndAt() != null
                && posting.getApplyEndAt().isBefore(now)) {
            throw new IllegalArgumentException(
                    "접수가 마감된 채용공고입니다."
            );
        }

        return posting;
    }

    private CoverLetter getCoverLetter(
            Long coverLetterId,
            Long userId
    ) {
        if (coverLetterId == null) {
            return null;
        }

        return coverLetterService.getMine(coverLetterId, userId);
    }

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "기업 정보를 찾을 수 없습니다."
                ));
    }

    private void validateDuplicate(
            Long userId,
            Long jobPostingId
    ) {
        if (applicationRepository
                .existsByApplicantUserIdAndJobPostingIdAndCurrentStatusNot(
                        userId,
                        jobPostingId,
                        CANCELLED_STATUS
                )) {
            throw new DuplicateApplicationException(
                    "이미 지원한 채용공고입니다."
            );
        }
    }

    private String formatDeadline(LocalDateTime deadline) {
        if (deadline == null) {
            return "상시";
        }

        long remainingDays = ChronoUnit.DAYS.between(
                LocalDate.now(),
                deadline.toLocalDate()
        );

        if (remainingDays == 0) {
            return "D-Day";
        }

        return "D-" + remainingDays;
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
