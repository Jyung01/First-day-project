package kr.co.firstdayproject.dto.my;

import java.time.LocalDateTime;

public record MyApplicationListItem(
        Long applicationId,
        String companyName,
        String companyLogoUrl,
        String companyInitial,
        String jobTitle,
        String currentStatus,
        String statusLabel,
        String statusVariant,
        LocalDateTime appliedAt,
        LocalDateTime latestChangedAt
) {
}
