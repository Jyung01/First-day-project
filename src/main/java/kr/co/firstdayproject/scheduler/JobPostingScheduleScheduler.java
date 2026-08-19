package kr.co.firstdayproject.scheduler;

import kr.co.firstdayproject.service.job.JobPostingClosingService;
import kr.co.firstdayproject.service.job.JobPostingPublishingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 테스트 프로파일에서는 등록하지 않는다. 기동 직후 실행되는 catch-up이
 * 테스트 컨텍스트에서도 DB를 건드려 contextLoads를 깨뜨린다.
 * SchedulingConfig를 끄는 것만으로는 @EventListener가 막히지 않는다.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class JobPostingScheduleScheduler {

    private final JobPostingPublishingService publishingService;
    private final JobPostingClosingService closingService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void processJobPostingSchedules() {
        publishingService.publishScheduledPostings();
        closingService.closeExpiredPostings();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpJobPostingSchedulesOnStartup() {
        processJobPostingSchedules();
    }
}
