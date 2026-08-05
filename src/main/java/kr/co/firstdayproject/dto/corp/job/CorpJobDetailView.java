package kr.co.firstdayproject.dto.corp.job;

import java.time.LocalDateTime;
import java.util.List;

public record CorpJobDetailView(
    Long jobPostingId,
    String title,
    String status,
    String category,
    String employmentType,
    String career,
    String educationLevel,
    String workRegion,
    String workAddress,
    String salary,
    LocalDateTime applyEndAt,
    Integer headcount,
    List<String> skills,
    List<String> benefits,
    String introduction,
    String mainTasks,
    String qualifications,
    String preferredConditions,
    long applicantCount,
    String hiddenReason
) {
}
