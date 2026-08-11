package kr.co.firstdayproject.dto.job;

import java.time.LocalDateTime;
import java.util.List;

public record JobDetailView(
        Long jobPostingId,
        String title,
        String companyName,
        String companyLogoUrl,
        String companyIndustry,
        String companyDescription,
        String companyIntroduction,
        String companyAddress,
        String category,
        String career,
        String educationLevel,
        String employmentType,
        String workRegion,
        String workAddress,
        String salary,
        Integer headcount,
        LocalDateTime applyStartAt,
        LocalDateTime applyEndAt,
        String introduction,
        String mainTasks,
        String qualifications,
        String preferredConditions,
        List<String> benefits,
        List<String> skills,
        long viewCount,
        String deadline,
        boolean newPosting,
        boolean hotPosting,
        boolean bookmarked
) {
}
