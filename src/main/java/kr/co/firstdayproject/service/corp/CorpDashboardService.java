package kr.co.firstdayproject.service.corp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import kr.co.firstdayproject.dto.corp.dashboard.CorpDashboardApplicantItem;
import kr.co.firstdayproject.dto.corp.dashboard.CorpDashboardJobItem;
import kr.co.firstdayproject.dto.corp.dashboard.CorpDashboardStats;
import kr.co.firstdayproject.dto.corp.dashboard.CorpDashboardView;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.repository.application.ApplicationRepository;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CorpDashboardService {

    private static final int DASHBOARD_ITEM_COUNT = 4;

    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;

    public CorpDashboardView getDashboard(Long companyId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);

        var stats = new CorpDashboardStats(
                jobPostingRepository.countByCompanyIdAndStatus(
                        companyId,
                        "모집중"
                ),
                jobPostingRepository.countCompanyJobsClosingSoon(
                        companyId,
                        now,
                        now.plusDays(7)
                ),
                applicationRepository.countCompanyApplicantsBetween(
                        companyId,
                        now.minusDays(7),
                        now
                ),
                applicationRepository.countCompanyApplicantsBetween(
                        companyId,
                        today,
                        tomorrow
                ),
                applicationRepository.countCompanyApplicantsByStatus(
                        companyId,
                        "지원완료"
                ),
                jobPostingRepository.sumViewCountByCompanyId(companyId)
        );

        var applicants = applicationRepository.findRecentCompanyApplicants(
                        companyId,
                        PageRequest.of(0, DASHBOARD_ITEM_COUNT)
                ).stream()
                .map(item -> new CorpDashboardApplicantItem(
                        item.getApplicationId(),
                        item.getApplicantName(),
                        item.getJobTitle(),
                        item.getAppliedAt(),
                        item.getCurrentStatus(),
                        statusVariant(item.getCurrentStatus())
                ))
                .toList();

        var jobs = jobPostingRepository
                .findDashboardJobs(
                        companyId,
                        PageRequest.of(0, DASHBOARD_ITEM_COUNT)
                ).stream()
                .map(job -> new CorpDashboardJobItem(
                        job.getJobPostingId(),
                        job.getTitle(),
                        job.getStatus(),
                        jobStatusVariant(job.getStatus()),
                        job.getApplyEndAt(),
                        deadlineLabel(job, now)
                ))
                .toList();

        return new CorpDashboardView(stats, applicants, jobs);
    }

    private String deadlineLabel(JobPosting job, LocalDateTime now) {
        if ("마감".equals(job.getStatus())) {
            LocalDateTime closedAt = job.getClosedAt();
            return closedAt == null ? "-" : closedAt.toLocalDate().toString()
                    .substring(5).replace('-', '.');
        }
        if (job.getApplyEndAt() == null) return "-";

        long days = ChronoUnit.DAYS.between(
                now.toLocalDate(),
                job.getApplyEndAt().toLocalDate()
        );
        return days >= 0 ? "D-" + days : "마감일 경과";
    }

    private String statusVariant(String status) {
        return switch (status) {
            case "서류합격", "면접예정", "면접완료", "최종합격", "입사완료" -> "green";
            case "불합격", "지원취소", "입사포기", "채용종료" -> "gray";
            default -> "default";
        };
    }

    private String jobStatusVariant(String status) {
        return switch (status) {
            case "모집중" -> "open";
            case "모집예정" -> "scheduled";
            case "숨김" -> "hidden";
            case "재검토요청" -> "review";
            default -> "closed";
        };
    }
}
