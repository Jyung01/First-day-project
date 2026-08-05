package kr.co.firstdayproject.service.job;

import java.time.LocalDateTime;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobPostingClosingService {

    private final JobPostingRepository jobPostingRepository;

    @Transactional
    public int closeExpiredPostings() {
        return jobPostingRepository.closeExpiredPostings(
            "마감일도래",
            LocalDateTime.now()
        );
    }
}
