package kr.co.firstdayproject.entity.application;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 공고당 1회 지원과 제출 당시 이력서 JSON; 취소 후에도 같은 공고 재지원 불가
 * DB table: applications
 */
@Entity
@Table(name = "applications")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;
    @Column(name = "applicant_user_id", nullable = false)
    private Long applicantUserId;
    /** 지원에 사용한 이력서 원본; 원본 삭제 후에는 NULL */
    @Column(name = "resume_id")
    private Long resumeId;
    /** 지원 완료 시점의 이력서·지원자 연락처 전체 스냅샷 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resume_snapshot_json", nullable = false, columnDefinition = "json")
    private String resumeSnapshotJson;
    /** 지원에 사용한 자기소개서 원본; 원본 삭제 후에는 NULL */
    @Column(name = "cover_letter_id")
    private Long coverLetterId;
    /** 지원 완료 시점의 자기소개서 제목·문항·답변 스냅샷 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cover_letter_snapshot_json", columnDefinition = "json")
    private String coverLetterSnapshotJson;
    @Column(name = "current_status", nullable = false, length = 20)
    private String currentStatus;
    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;
    /** 지원완료 단계에서 지원자가 취소한 시각 */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    /** 채용종료 원인; 현재는 기업탈퇴 */
    @Column(name = "termination_reason", length = 20)
    private String terminationReason;
    /** 지원 절차가 채용종료로 끝난 시각 */
    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
