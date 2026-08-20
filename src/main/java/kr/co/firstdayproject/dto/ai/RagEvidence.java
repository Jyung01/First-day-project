package kr.co.firstdayproject.dto.ai;

/**
 * 자소서 첨삭 시 pgvector에서 검색해 프롬프트에 넣은 근거 문단 1건.
 * sourceId를 함께 남겨야 나중에 어느 채용공고에서 온 문단인지 되짚을 수 있다.
 */
public record RagEvidence(
    String sourceId,
    String text,
    Double score
) {
}
