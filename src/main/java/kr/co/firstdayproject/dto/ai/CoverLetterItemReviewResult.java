package kr.co.firstdayproject.dto.ai;

import java.util.List;

/**
 * 자기소개서 문항 1개에 대한 AI 첨삭 결과.
 * ChatClient의 구조화 출력(entity) 대상 타입으로 사용된다.
 */
public record CoverLetterItemReviewResult(
    String summary,
    List<String> improvementPoints,
    String revisedAnswer
) {
}
