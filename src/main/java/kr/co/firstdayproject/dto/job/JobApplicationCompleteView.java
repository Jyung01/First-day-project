package kr.co.firstdayproject.dto.job;

import java.time.LocalDateTime;

public record JobApplicationCompleteView(
        Long applicationId,
        String companyName,
        String jobTitle,
        LocalDateTime appliedAt,
        String currentStatus
) {
}
