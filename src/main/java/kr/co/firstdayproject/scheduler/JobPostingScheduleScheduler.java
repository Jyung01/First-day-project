package kr.co.firstdayproject.scheduler;

import kr.co.firstdayproject.service.job.JobPostingClosingService;
import kr.co.firstdayproject.service.job.JobPostingPublishingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobPostingScheduleScheduler {

    private final JobPostingPublishingService publishingService;
    private final JobPostingClosingService closingService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void processJobPostingSchedules() {
        publishingService.publishScheduledPostings();
        closingService.closeExpiredPostings();
    }
}
