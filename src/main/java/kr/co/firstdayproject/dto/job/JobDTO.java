package kr.co.firstdayproject.dto.job;

import lombok.Data;

import java.time.LocalDate;

@Data
public class JobDTO {

    private Long jobPostingId;
    private Long companyId;
    private Long jobCategoryId;

    private String title;
    private String employmentType;
    private String careerType;
    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    private String educationLevel;
    private String workAddress;
    private String workRegion;
    private String salaryText;
    private Integer salaryMin;
    private Integer salaryMax;
    private Integer headcount;

    private LocalDate applyStartAt;
    private LocalDate applyEndAt;

    private String introduction;
    private String mainTasks;
    private String qualifications;
    private String preferredConditions;
    private String benefitsJson;
    private String processText;

    private String status;
    private String closeReason;
    private String hiddenReason;

    private Long hiddenBy;
    private Integer viewCount;

    private LocalDate publishedAt;
    private LocalDate closedAt;
    private Integer rowVersion;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // JOIN용
    private String companyName;
    private String logoUrl;
    private String categoryName;
}