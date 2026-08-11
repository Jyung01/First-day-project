package kr.co.firstdayproject.dto.corp.dashboard;

import java.time.LocalDateTime;

public record CorpDashboardJobItem(
        Long jobPostingId,
        String title,
        String status,
        String statusVariant,
        LocalDateTime applyEndAt,
        String deadlineLabel
) {
}
