package kr.co.firstdayproject.dto.admin.config;

import java.time.LocalDateTime;

public record SiteVersionItem(
        Long siteVersionId,
        String versionName,
        String changeNotes,
        String creatorName,
        LocalDateTime createdAt
) {
}
