package kr.co.firstdayproject.dto.ai;

import java.time.LocalDateTime;

/**
 * 자소서 AI 첨삭 이력 드롭다운에 표시할 항목 1개.
 */
public record CoverLetterAiReviewHistoryItem(
    Long coverLetterAiReviewId,
    String targetJobPostingTitle,
    LocalDateTime createdAt
) {
}
