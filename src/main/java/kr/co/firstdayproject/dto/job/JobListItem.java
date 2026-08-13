package kr.co.firstdayproject.dto.job;

import java.util.List;

public record JobListItem(
        Long jobPostingId,
        String logoUrl,
        String companyName,
        String title,
        String workRegion,
        String careerType,
        String employmentType,
        String categoryName,

        List<String> skills,

        int remainingSkillCount,

        Long viewCount,

        long applicantCount,

        boolean newPosting,
        boolean hotPosting,

        String deadlineText,

        boolean bookmarked
) {
}
