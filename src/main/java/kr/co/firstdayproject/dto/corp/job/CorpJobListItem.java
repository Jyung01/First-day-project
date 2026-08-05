package kr.co.firstdayproject.dto.corp.job;

import java.time.LocalDateTime;

public record CorpJobListItem(
    Long jobPostingId,
    String title,
    String status,
    long applicantCount,
    LocalDateTime applyEndAt,
    LocalDateTime updatedAt,
    String hiddenReason
) {
}
