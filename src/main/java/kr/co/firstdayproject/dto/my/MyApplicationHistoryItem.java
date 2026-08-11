package kr.co.firstdayproject.dto.my;

import java.time.LocalDateTime;

public record MyApplicationHistoryItem(
        String status,
        String statusLabel,
        LocalDateTime changedAt,
        String changeReason
) {
}
