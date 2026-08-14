package kr.co.firstdayproject.dto.ai;

import java.util.List;

/**
 * 자소서 AI 첨삭 결과 화면에서 문항 1개를 렌더링하기 위한 뷰.
 *
 * coverLetterItemId는 "이 내용으로 적용" 시 실제로 갱신할 CoverLetterItem을
 * 찾기 위한 것으로, 첨삭 요청 이후 해당 문항이 삭제됐다면 null일 수 있다.
 *
 * answer는 기본적으로 첨삭 요청 당시 스냅샷이지만, 이후 실제 문항 답변이 스냅샷과
 * 달라졌다면(적용 버튼을 눌렀거나 자소서를 직접 수정한 경우) 최신 답변으로 대체되고
 * resolved가 true가 된다 — "적용 여부"를 별도 컬럼 없이 이 비교로만 판단한다.
 */
public record CoverLetterAiReviewItemView(
    Long coverLetterItemId,
    String question,
    String answer,
    boolean resolved,
    String summary,
    List<String> improvementPoints,
    String revisedAnswer
) {
}
