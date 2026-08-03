package kr.co.firstdayproject.entity.resume;

import jakarta.persistence.*;
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
 * 이력서 경력
 * DB table: resume_careers
 */
@Entity
@Table(name = "resume_careers")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ResumeCareer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "career_id", nullable = false)
    private Long careerId;
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;
    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;
    @Column(name = "department", length = 100)
    private String department;
    @Column(name = "position_title", length = 100)
    private String positionTitle;
    @Column(name = "employment_type", length = 50)
    private String employmentType;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
