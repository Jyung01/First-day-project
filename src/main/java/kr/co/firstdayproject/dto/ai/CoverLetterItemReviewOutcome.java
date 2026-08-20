package kr.co.firstdayproject.dto.ai;

import java.util.List;

/**
 * 문항 1개 첨삭의 최종 산출물 — AI 결과와 그 근거로 쓰인 검색 문단을 함께 들고 올라간다.
 *
 * 근거 필드를 {@link CoverLetterItemReviewResult}에 직접 넣지 않고 이렇게 분리하는 이유는,
 * 그 레코드가 {@code .entity(CoverLetterItemReviewResult.class)}로 OpenAI 응답을 직접 매핑하는
 * 타입이기 때문이다. 필드를 추가하면 Spring AI가 그것까지 JSON 스키마에 넣어 모델에게 채우라고
 * 요구하고, 결과적으로 AI가 근거 문단을 지어내게 된다.
 * 따라서 이 타입은 절대 {@code .entity()}에 넘기지 않는다.
 */
public record CoverLetterItemReviewOutcome(
    CoverLetterItemReviewResult result,
    List<RagEvidence> evidence
) {
}
