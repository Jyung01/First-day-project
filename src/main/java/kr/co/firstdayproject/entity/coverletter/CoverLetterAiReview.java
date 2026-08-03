package kr.co.firstdayproject.entity.coverletter;

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
 * 자기소개서 AI 첨삭 1회 결과; 원문·수정본·피드백을 한 행에 보존
 * DB table: cover_letter_ai_reviews
 */
@Entity
@Table(name = "cover_letter_ai_reviews")
@Getter
@Setter
@Builder
@DynamicInsert
@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CoverLetterAiReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cover_letter_ai_review_id", nullable = false)
    private Long coverLetterAiReviewId;
    @Column(name = "cover_letter_id", nullable = false)
    private Long coverLetterId;
    /** 첨삭 요청 당시 문항·답변 전체 스냅샷 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_content", nullable = false, columnDefinition = "json")
    private String originalContent;
    /** AI가 제안한 문항별 수정 답변 전체 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "revised_content", nullable = false, columnDefinition = "json")
    private String revisedContent;
    /** 전체 첨삭 요약과 개선 이유 */
    @Column(name = "feedback", columnDefinition = "LONGTEXT")
    private String feedback;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
