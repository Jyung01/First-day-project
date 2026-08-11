package kr.co.firstdayproject.dto.corp.dashboard;

import java.time.LocalDateTime;

public record CorpDashboardApplicantItem(
        Long applicationId,
        String applicantName,
        String jobTitle,
        LocalDateTime appliedAt,
        String status,
        String statusVariant
) {
}
