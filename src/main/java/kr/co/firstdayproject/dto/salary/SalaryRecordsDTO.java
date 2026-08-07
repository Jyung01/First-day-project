package kr.co.firstdayproject.dto.salary;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SalaryRecordsDTO {
    private Long salaryRecordId;
    private Long companyId;
    private Long authorUserId;
    private Long jobCategoryId;
    private String employmentStatus;
    private String employmentType;
    private Integer careerYears;
    private Integer salaryYear;
    private Integer baseSalary;
    private Integer bonusAmount;
    private LocalDateTime consentedAt;
    private String status;
    private String hiddenReason;
    private Long hiddenBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // company JOIN
    private String companyName;
    private String categoryName;
    private String industryName;
    private String addressLine1;
    private String logoUrl;

    // 목록 집계
    private String companySize;
    private Long averageSalary;
    private Long newcomerAverageSalary;
    private Integer recordCount;
    private Integer sampleCount;
    private LocalDateTime latestUpdatedAt;
    private Long middleCareerAverageSalary;

    // 전체 요약
    private Long overallAverageSalary;
    private Long overallNewcomerAverageSalary;
    private Integer totalRecordCount;
}
