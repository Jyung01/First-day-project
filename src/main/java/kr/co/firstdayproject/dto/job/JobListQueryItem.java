package kr.co.firstdayproject.dto.job;

import java.time.LocalDateTime;

/**
 * Repository 목록 조회 결과.
 */
public record JobListQueryItem(
        Long jobPostingId,
        String logoUrl,
        String companyName,
        String title,
        String workRegion,
        String careerType,
        String employmentType,
        String categoryName,
        Long viewCount,
        Long applicantCount,
        LocalDateTime publishedAt,
        LocalDateTime applyEndAt
) {
}