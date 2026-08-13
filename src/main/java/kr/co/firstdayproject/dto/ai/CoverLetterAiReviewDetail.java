package kr.co.firstdayproject.dto.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자소서 AI 첨삭 이력 1건(cover_letter_ai_reviews 1행)을 화면에 표시하기 위한 뷰.
 */
public record CoverLetterAiReviewDetail(
    Long coverLetterAiReviewId,
    String targetJobPostingTitle,
    String targetCompanyName,
    LocalDateTime createdAt,
    List<CoverLetterAiReviewItemView> items
) {
}
