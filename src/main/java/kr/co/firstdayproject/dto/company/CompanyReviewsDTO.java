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

    private Double  careerGrowthRating;
    private Double  workSatisfactionRating;
    private Double  compensationRating;
    private Double  cultureRating;

    private BigDecimal overallRating;

    private String pros;
    private String cons;
    private String summary;

    private String status;
    private String hiddenReason;
    private Long hiddenBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 리뷰 개수
    private Integer reviewCount;

    // 평점 분포
    private Integer rating5Count;
    private Integer rating4Count;
    private Integer rating3Count;
    private Integer rating2Count;
    private Integer rating1Count;

    // 리뷰 도움순
    private Integer helpCount;
    private String jobCategoryName;
    private String companyName;
}
