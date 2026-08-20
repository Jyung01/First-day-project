package kr.co.firstdayproject.dto.ai;

import java.util.Map;

/**
 * 자소서 AI 첨삭 요청 본문.
 *
 * additionalInfo는 문항 ID → 사용자가 직접 적은 추가 정보. 문항별로 나눠 받는 이유는,
 * 하나로 받으면 모든 문항 호출에 같은 값이 전달되어 어느 문항에 반영할지를 AI가 판단해야 하고
 * 주제가 넓은 문항이 관련 없는 내용까지 끌어다 쓰는 문제가 생기기 때문이다.
 * 사용자가 직접 지정하면 그 판단 자체가 필요 없어진다. 비워둔 문항은 키 자체가 오지 않는다.
 */
public record CoverLetterAiReviewRequest(
    Long jobPostingId,
    Map<Long, String> additionalInfo
) {
}
