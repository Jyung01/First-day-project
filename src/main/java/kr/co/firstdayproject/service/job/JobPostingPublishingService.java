package kr.co.firstdayproject.service.job;

import java.time.LocalDateTime;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobPostingPublishingService {

    private final JobPostingRepository jobPostingRepository;

    @Transactional
    public int publishScheduledPostings() {
        return jobPostingRepository.publishScheduledPostings(
            LocalDateTime.now()
        );
    }
}
