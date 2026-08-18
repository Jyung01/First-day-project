package kr.co.firstdayproject.dto.corp.dashboard;

import java.util.List;

public record CorpDashboardView(
        CorpDashboardStats stats,
        List<CorpDashboardApplicantItem> recentApplicants,
        List<CorpDashboardJobItem> recentJobs
) {
}
