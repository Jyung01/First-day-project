package kr.co.firstdayproject.entity.resume;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 이력서 학력
 * DB table: resume_educations
 */
@Entity
@Table(name = "resume_educations")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ResumeEducation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_id", nullable = false)
    private Long educationId;
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;
    @Column(name = "school_name", nullable = false, length = 200)
    private String schoolName;
    @Column(name = "major", length = 200)
    private String major;
    @Column(name = "degree", length = 100)
    private String degree;
    @Column(name = "admission_date")
    private LocalDate admissionDate;
    @Column(name = "graduation_date")
    private LocalDate graduationDate;
    @Column(name = "graduation_status", length = 20)
    private String graduationStatus;
    @Column(name = "gpa_score", precision = 3, scale = 2)
    private BigDecimal gpaScore;
    @Column(name = "gpa_scale", precision = 3, scale = 2)
    private BigDecimal gpaScale;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
