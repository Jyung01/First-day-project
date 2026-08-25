package kr.co.firstdayproject.service.job;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import kr.co.firstdayproject.entity.job.SavedJob;
import kr.co.firstdayproject.entity.job.SavedJobId;
import kr.co.firstdayproject.exception.ResourceNotFoundException;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import kr.co.firstdayproject.repository.job.SavedJobRepository;
import kr.co.firstdayproject.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavedJobService {

    private static final String PERSONAL_ROLE = "ROLE_PERSONAL";

    private final SavedJobRepository savedJobRepository;
    private final JobPostingRepository jobPostingRepository;

    @Transactional
    public boolean toggleSavedJob(
            Long jobPostingId,
            Authentication authentication
    ) {
        Long userId = getPersonalUserId(authentication);

        SavedJobId savedJobId = new SavedJobId(
                userId,
                jobPostingId
        );

        if (savedJobRepository.existsById(savedJobId)) {
            savedJobRepository.deleteById(savedJobId);
            return false;
        }

        /*
         * 등록 전에 공고 존재 여부를 확인한다.
         * saved_jobs.job_posting_id에 외래키가 있어 없는 공고면 어차피 저장에 실패하지만,
         * 그대로 두면 DataIntegrityViolationException이 500으로 나간다.
         * 해제(위쪽 분기)는 이미 등록된 행을 지우는 것이라 이 확인이 필요 없다.
         */
        if (!jobPostingRepository.existsById(jobPostingId)) {
            throw new ResourceNotFoundException(
                    "조회할 수 없는 채용공고입니다."
            );
        }

        SavedJob savedJob = SavedJob.builder()
                .id(savedJobId)
                .createdAt(LocalDateTime.now())
                .build();

        savedJobRepository.save(savedJob);

        return true;
    }

    @Transactional(readOnly = true)
    public boolean isSavedJob(
            Long jobPostingId,
            Authentication authentication
    ) {
        if (!isPersonalMember(authentication)) {
            return false;
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        SavedJobId savedJobId = new SavedJobId(
                userDetails.getUserId(),
                jobPostingId
        );

        return savedJobRepository.existsById(savedJobId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getSavedJobPostingIds(
            Collection<Long> jobPostingIds,
            Authentication authentication
    ) {
        if (!isPersonalMember(authentication)
                || jobPostingIds == null
                || jobPostingIds.isEmpty()) {
            return Set.of();
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return new HashSet<>(
                savedJobRepository.findSavedJobPostingIds(
                        userDetails.getUserId(),
                        jobPostingIds
                )
        );
    }

    private Long getPersonalUserId(
            Authentication authentication
    ) {
        if (!isPersonalMember(authentication)) {
            throw new AccessDeniedException(
                    "개인회원 로그인이 필요한 기능입니다."
            );
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        return userDetails.getUserId();
    }

    private boolean isPersonalMember(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        PERSONAL_ROLE.equals(
                                authority.getAuthority()
                        )
                );
    }
}
