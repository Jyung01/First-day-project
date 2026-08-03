package kr.co.firstdayproject.entity.resume;

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
 * 지원용 이력서; 파일 업로드가 아닌 구조화 데이터
 * DB table: resumes
 */
@Entity
@Table(name = "resumes")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Column(name = "applicant_name", nullable = false, length = 100)
    private String applicantName;
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    @Column(name = "phone", nullable = false, length = 30)
    private String phone;
    @Column(name = "career_type", nullable = false, length = 20)
    private String careerType;
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;
    @Column(name = "status", nullable = false, length = 10)
    private String status;
    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
