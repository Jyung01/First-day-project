package kr.co.firstdayproject.dto.my;

import java.time.LocalDateTime;

public record CoverLetterDashboardView(
        Long coverLetterId,
        String title,
        LocalDateTime updatedAt,
        boolean aiReviewed
) {
}
