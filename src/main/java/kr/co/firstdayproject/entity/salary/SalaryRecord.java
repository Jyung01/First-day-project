package kr.co.firstdayproject.entity.salary;

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
 * 지원 이력 검증 없는 자기신고 연봉; 회원·기업·연도당 1건, 동일 조건 3건 이상 집계 노출
 * DB table: salary_records
 */
@Entity
@Table(name = "salary_records")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_record_id", nullable = false)
    private Long salaryRecordId;
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;
    @Column(name = "job_category_id", nullable = false)
    private Long jobCategoryId;
    @Column(name = "employment_status", nullable = false, length = 10)
    private String employmentStatus;
    @Column(name = "employment_type", nullable = false, length = 30)
    private String employmentType;
    @Column(name = "career_years", nullable = false)
    private Integer careerYears;
    @Column(name = "salary_year", nullable = false)
    private Integer salaryYear;
    /** 세전 연봉, 만원 단위 */
    @Column(name = "base_salary", nullable = false)
    private Integer baseSalary;
    /** 성과급, 만원 단위 */
    @Column(name = "bonus_amount")
    private Integer bonusAmount;
    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;
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
