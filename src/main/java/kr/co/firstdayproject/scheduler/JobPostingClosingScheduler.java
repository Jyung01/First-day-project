package kr.co.firstdayproject.scheduler;

import kr.co.firstdayproject.service.job.JobPostingClosingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobPostingClosingScheduler {

    private final JobPostingClosingService jobPostingClosingService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void closeExpiredPostings() {
        jobPostingClosingService.closeExpiredPostings();
    }
}
