package kr.co.firstdayproject.entity.review;

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
 * 면접완료 이력 기반 익명 면접후기; 지원 건당 1건
 * DB table: interview_reviews
 */
@Entity
@Table(name = "interview_reviews")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InterviewReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "interview_review_id", nullable = false)
    private Long interviewReviewId;
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;
    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    /** YYYY-MM */
    @Column(name = "interview_month", nullable = false, length = 7)
    private String interviewMonth;
    @Column(name = "interview_type", nullable = false, length = 30)
    private String interviewType;
    @Column(name = "interview_result", nullable = false, length = 10)
    private String interviewResult;
    @Column(name = "difficulty", nullable = false, length = 10)
    private String difficulty;
    @Column(name = "process_text", nullable = false, columnDefinition = "TEXT")
    private String processText;
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;
    @Column(name = "tips", columnDefinition = "TEXT")
    private String tips;
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
