package kr.co.firstdayproject.dto.company;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CompanyReviewsDTO {

    private Long companyReviewId;
    private Long companyId;
    private Long authorUserId;
    private Long eligibilityApplicationId;
    private String employmentStatus;
    private Long jobCategoryId;

    private Integer careerGrowthRating;
    private Integer workSatisfactionRating;
    private Integer compensationRating;
    private Integer cultureRating;

    private BigDecimal overallRating;

    private String pros;
    private String cons;
    private String summary;

    private String status;
    private String hiddenReason;
    private Long hiddenBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}