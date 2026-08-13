package kr.co.firstdayproject.dto.my;

import java.time.LocalDateTime;

public record MyApplicationProgressStage(
        String status,
        String label,
        LocalDateTime changedAt,
        String state
) {
    public boolean isPlanned() {
        return "planned".equals(state);
    }
}
