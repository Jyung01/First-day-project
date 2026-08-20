package kr.co.firstdayproject.dto.corp.dashboard;

public record CorpDashboardStats(
        long recruitingJobCount,
        long closingSoonJobCount,
        long recentApplicantCount,
        long todayApplicantCount,
        long pendingReviewCount,
        long totalViewCount
) {
}
