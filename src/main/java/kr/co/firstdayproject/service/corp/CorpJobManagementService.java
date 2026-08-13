package kr.co.firstdayproject.service.corp;

import java.time.LocalDateTime;
import java.util.Set;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.service.ai.JobPostingEmbeddingSyncEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorpJobManagementService {

    private static final Set<String> DELETABLE_STATUSES = Set.of(
        "임시저장",
        "모집예정",
        "마감"
    );

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void closeJobPosting(Long companyId, Long jobPostingId) {
        JobPosting posting = getCompanyJobPosting(companyId, jobPostingId);

        if (!"모집중".equals(posting.getStatus())) {
            throw new IllegalArgumentException(
                "모집 중인 채용공고만 마감할 수 있습니다."
            );
        }

        posting.setStatus("마감");
        posting.setCloseReason("기업마감");
        posting.setClosedAt(LocalDateTime.now());
    }

    @Transactional
    public void deleteJobPosting(Long companyId, Long jobPostingId) {
        JobPosting posting = getCompanyJobPosting(companyId, jobPostingId);

        if (!DELETABLE_STATUSES.contains(posting.getStatus())) {
            throw new IllegalArgumentException(
                "임시저장, 모집예정 또는 마감된 채용공고만 삭제할 수 있습니다."
            );
        }

        posting.setStatus("삭제");

        eventPublisher.publishEvent(new JobPostingEmbeddingSyncEvent(
            jobPostingId,
            false
        ));
    }

    private JobPosting getCompanyJobPosting(
        Long companyId,
        Long jobPostingId
    ) {
        if (companyId == null) {
            throw new IllegalArgumentException("기업회원 로그인이 필요합니다.");
        }

        return jobPostingRepository
            .findByJobPostingIdAndCompanyId(jobPostingId, companyId)
            .orElseThrow(() -> new IllegalArgumentException(
                "채용공고를 찾을 수 없습니다."
            ));
    }
}
