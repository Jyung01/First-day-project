package kr.co.firstdayproject.service.admin;

import java.time.LocalDateTime;
import kr.co.firstdayproject.entity.company.Company;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.entity.member.User;
import kr.co.firstdayproject.repository.company.CompanyRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminJobModerationService {

    private final JobPostingRepository jobPostingRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Transactional
    public void hideJobPosting(
        Long adminUserId,
        Long jobPostingId,
        String reason
    ) {
        validateAdmin(adminUserId);

        String normalizedReason = normalizeHideReason(reason);
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
            .orElseThrow(() -> new IllegalArgumentException(
                "채용공고를 찾을 수 없습니다."
            ));

        if (!"모집중".equals(posting.getStatus())) {
            throw new IllegalArgumentException(
                "게시 중인 채용공고만 숨김 처리할 수 있습니다."
            );
        }

        updateHiddenState(posting, adminUserId, normalizedReason);
    }

    @Transactional
    public void keepJobPostingHidden(
        Long adminUserId,
        Long jobPostingId,
        String reason
    ) {
        validateAdmin(adminUserId);

        JobPosting posting = getReviewRequestedPosting(jobPostingId);
        updateHiddenState(
            posting,
            adminUserId,
            normalizeHideReason(reason)
        );
    }

    @Transactional
    public String releaseJobPosting(
        Long adminUserId,
        Long jobPostingId
    ) {
        validateAdmin(adminUserId);

        JobPosting posting = getReviewRequestedPosting(jobPostingId);
        LocalDateTime now = LocalDateTime.now();
        boolean expired = posting.getApplyEndAt() != null
            && !posting.getApplyEndAt().isAfter(now);

        if (expired) {
            posting.setStatus("마감");
            posting.setCloseReason("마감일도래");
            posting.setClosedAt(now);
        } else {
            validatePublishableCompany(posting.getCompanyId());
            posting.setStatus("모집중");
            posting.setCloseReason(null);
            posting.setClosedAt(null);
        }

        return posting.getStatus();
    }

    private void updateHiddenState(
        JobPosting posting,
        Long adminUserId,
        String reason
    ) {
        posting.setStatus("숨김");
        posting.setHiddenReason(reason);
        posting.setHiddenBy(adminUserId);
        posting.setHiddenAt(LocalDateTime.now());
    }

    private String normalizeHideReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("숨김 사유를 입력해 주세요.");
        }

        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 1000) {
            throw new IllegalArgumentException(
                "숨김 사유는 1,000자 이하로 입력해 주세요."
            );
        }
        return normalizedReason;
    }

    private void validateAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new IllegalArgumentException("관리자 로그인이 필요합니다.");
        }

        User admin = userRepository.findById(adminUserId)
            .orElseThrow(() -> new IllegalArgumentException(
                "관리자 정보를 찾을 수 없습니다."
            ));
        if (!"관리자".equals(admin.getUserType())) {
            throw new IllegalArgumentException("관리자 권한이 필요합니다.");
        }
    }

    private JobPosting getReviewRequestedPosting(Long jobPostingId) {
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
            .orElseThrow(() -> new IllegalArgumentException(
                "채용공고를 찾을 수 없습니다."
            ));

        if (!"재검토요청".equals(posting.getStatus())) {
            throw new IllegalArgumentException(
                "재검토 요청 상태의 공고만 처리할 수 있습니다."
            );
        }
        return posting;
    }

    private void validatePublishableCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException(
                "기업 정보를 찾을 수 없습니다."
            ));

        if (!"승인".equals(company.getApprovalStatus())
            || !"정상".equals(company.getCompanyStatus())) {
            throw new IllegalArgumentException(
                "승인된 정상 기업의 공고만 숨김 해제할 수 있습니다."
            );
        }
    }
}
