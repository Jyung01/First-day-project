package kr.co.firstdayproject.dto.company;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewReviewsDTO {

    private Long interviewReviewId;
    private Long companyId;
    private Long authorUserId;
    private Long jobPostingId;
    private Long applicationId;

    private String interviewMonth;
    private String interviewType;
    private String interviewResult;
    private String difficulty;

    private String processText;

    private String content;
    private String tips;

    private String status;

    private String hiddenReason;
    private Long hiddenBy;

    private Integer helpCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ===== JOIN =====
    private String title;
    private String careerType;
    private String companyName;

    // ===== 요약 =====
    private Integer reviewCount;

    private Integer passCount;
    private Integer failCount;
    private Integer waitingCount;

    private Double difficultyScore;

    // ===== 화면 출력용 =====
    /** 쉬움 / 보통 / 어려움 */
    private String difficultyText;

    /** 합격 비율 */
    private Integer passPercent;

    /** 불합격 비율 */
    private Integer failPercent;

    /** 대기중 비율 */
    private Integer waitingPercent;
}
