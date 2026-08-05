package kr.co.firstdayproject.entity.job;

import jakarta.persistence.*;
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
 * 채용공고 원장; 공고 승인 상태와 승인 이력은 사용하지 않음
 * DB table: job_postings
 */
@Entity
@Table(name = "job_postings")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "job_category_id")
    private Long jobCategoryId;
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    @Column(name = "employment_type", length = 20)
    private String employmentType;
    @Column(name = "career_type", length = 20)
    private String careerType;
    @Column(name = "min_experience_years")
    private Integer minExperienceYears;
    @Column(name = "max_experience_years")
    private Integer maxExperienceYears;
    @Column(name = "education_level", length = 30)
    private String educationLevel;
    @Column(name = "work_region", length = 100)
    private String workRegion;
    @Column(name = "work_address", length = 500)
    private String workAddress;
    @Column(name = "salary_text", length = 100)
    private String salaryText;
    /** 만원 단위 */
    @Column(name = "salary_min")
    private Integer salaryMin;
    /** 만원 단위 */
    @Column(name = "salary_max")
    private Integer salaryMax;
    @Column(name = "headcount")
    private Integer headcount;
    @Column(name = "apply_start_at")
    private LocalDateTime applyStartAt;
    @Column(name = "apply_end_at")
    private LocalDateTime applyEndAt;
    @Column(name = "introduction", columnDefinition = "LONGTEXT")
    private String introduction;
    @Column(name = "main_tasks", columnDefinition = "LONGTEXT")
    private String mainTasks;
    @Column(name = "qualifications", columnDefinition = "LONGTEXT")
    private String qualifications;
    @Column(name = "preferred_conditions", columnDefinition = "LONGTEXT")
    private String preferredConditions;
    @Column(name = "benefits_json", columnDefinition = "json")
    private String benefitsJson;
    @Column(name = "process_text", columnDefinition = "TEXT")
    private String processText;
    @Column(name = "status", nullable = false, length = 10)
    private String status;
    @Column(name = "close_reason", length = 20)
    private String closeReason;
    /** 관리자가 공고를 숨긴 구체적인 사유 */
    @Column(name = "hidden_reason", length = 1000)
    private String hiddenReason;
    /** 공고 숨김을 처리한 관리자 회원 ID */
    @Column(name = "hidden_by")
    private Long hiddenBy;
    /** 공고를 숨김 상태로 변경한 시각 */
    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;
    @Column(name = "view_count", nullable = false)
    private Long viewCount;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
