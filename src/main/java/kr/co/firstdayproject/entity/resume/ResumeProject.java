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
 * 이력서 프로젝트
 * DB table: resume_projects
 */
@Entity
@Table(name = "resume_projects")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ResumeProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;
    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;
    @Column(name = "role_text", length = 300)
    private String roleText;
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;
    @Column(name = "start_date")
    private LocalDate startDate;
    @Column(name = "end_date")
    private LocalDate endDate;
    @Column(name = "project_url", length = 1000)
    private String projectUrl;
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
