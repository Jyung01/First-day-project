package kr.co.firstdayproject.dto.job;

public record MainJobListItem(
        Long jobPostingId,
        String logoUrl,
        String companyName,
        String title,
        String workRegion,
        String careerType,
        String employmentType,
        String categoryName,
        Long viewCount
) {
}