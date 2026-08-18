package kr.co.firstdayproject.exception;

/**
 * AI 첨삭 생성이 문항 전체에서 실패했을 때 사용.
 *
 * 문항 일부만 실패하면 성공한 문항을 살려 저장하므로 이 예외를 던지지 않는다.
 * 남길 결과가 하나도 없을 때만 던지며, 요청이 잘못된 것이 아니라 외부 호출이 실패한
 * 상황이므로 컨트롤러에서 502로 응답한다.
 */
public class AiReviewGenerationException extends RuntimeException {

    public AiReviewGenerationException(String message) {
        super(message);
    }
}
