package kr.co.firstdayproject.dto.job;

public record JobListItem(
        Long jobPostingId,
        String logoUrl,
        String companyName,
        String title,
        String workRegion,
        String careerType,
        String employmentType,
        String categoryName,

        Long viewCount,

        long applicantCount,

        boolean newPosting,
        boolean hotPosting,

        String deadlineText,

        boolean bookmarked
) {
}