package kr.co.firstdayproject.dto.admin.job;

import java.time.LocalDateTime;
import java.util.List;

public record AdminJobDetailView(
    Long jobPostingId,
    String companyName,
    String companyStatus,
    String industryName,
    String companyAddress,
    String contactName,
    String contactDepartment,
    String contactPositionTitle,
    String contactEmail,
    String contactPhone,
    String title,
    String status,
    String category,
    String employmentType,
    String career,
    String educationLevel,
    String workRegion,
    String workAddress,
    String salary,
    Integer headcount,
    LocalDateTime createdAt,
    LocalDateTime publishedAt,
    LocalDateTime applyStartAt,
    LocalDateTime applyEndAt,
    LocalDateTime closedAt,
    LocalDateTime hiddenAt,
    String hiddenReason,
    long applicantCount,
    List<String> skills,
    List<String> benefits,
    String introduction,
    String mainTasks,
    String qualifications,
    String preferredConditions
) {
    public boolean suspendedCompany() {
        return "이용정지".equals(companyStatus);
    }
}
