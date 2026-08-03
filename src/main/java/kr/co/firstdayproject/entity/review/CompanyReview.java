package kr.co.firstdayproject.entity.review;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 입사완료 이력 기반 익명 기업리뷰; 회원·기업당 1건
 * DB table: company_reviews
 */
@Entity
@Table(name = "company_reviews")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompanyReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_review_id", nullable = false)
    private Long companyReviewId;
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;
    @Column(name = "eligibility_application_id", nullable = false)
    private Long eligibilityApplicationId;
    @Column(name = "employment_status", nullable = false, length = 10)
    private String employmentStatus;
    @Column(name = "job_category_id")
    private Long jobCategoryId;
    @Column(name = "career_growth_rating", nullable = false)
    private Integer careerGrowthRating;
    @Column(name = "work_satisfaction_rating", nullable = false)
    private Integer workSatisfactionRating;
    @Column(name = "compensation_rating", nullable = false)
    private Integer compensationRating;
    @Column(name = "culture_rating", nullable = false)
    private Integer cultureRating;
    @Column(name = "overall_rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal overallRating;
    @Column(name = "pros", nullable = false, columnDefinition = "TEXT")
    private String pros;
    @Column(name = "cons", nullable = false, columnDefinition = "TEXT")
    private String cons;
    @Column(name = "summary", nullable = false, length = 300)
    private String summary;
    @Column(name = "status", nullable = false, length = 10)
    private String status;
    @Column(name = "hidden_reason", length = 1000)
    private String hiddenReason;
    @Column(name = "hidden_by")
    private Long hiddenBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
