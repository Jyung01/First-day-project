package kr.co.firstdayproject.dto.admin.job;

import java.time.LocalDateTime;

public record AdminJobListItem(
    Long jobPostingId,
    String companyName,
    String title,
    String categoryName,
    LocalDateTime createdAt,
    LocalDateTime applyEndAt,
    String status
) {
}
