package kr.co.firstdayproject.dto.corp.applicant;

import java.time.LocalDateTime;

public record CorpApplicationStatusHistoryItem(
        String status,
        String changeReason,
        String actorType,
        LocalDateTime changedAt
) {
}
