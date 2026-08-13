package kr.co.firstdayproject.dto.ai;

import java.util.List;

/**
 * 자소서 AI 첨삭 결과 화면에서 문항 1개를 렌더링하기 위한 뷰.
 * question/answer는 첨삭 요청 당시 스냅샷이며, 이후 사용자가 원본 자소서를
 * 수정해도 이 화면에 표시되는 원문은 바뀌지 않는다.
 */
public record CoverLetterAiReviewItemView(
    String question,
    String answer,
    String summary,
    List<String> improvementPoints,
    String revisedAnswer
) {
}
