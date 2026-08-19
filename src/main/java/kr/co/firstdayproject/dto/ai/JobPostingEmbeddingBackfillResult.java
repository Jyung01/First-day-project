package kr.co.firstdayproject.dto.ai;

import java.util.List;

/**
 * 채용공고 임베딩 백필 1회 실행 결과.
 *
 * @param processed        이번 호출에서 시도한 공고 수
 * @param succeeded        임베딩 반영에 성공한 수
 * @param failedJobPostingIds 실패한 공고 ID — 실패해도 나머지는 계속 진행한다
 * @param lastJobPostingId 이번에 처리한 마지막 공고 ID; 다음 호출의 after 값으로 넘기면 이어서 진행된다
 * @param remaining        lastJobPostingId 이후로 남은 공고 수; 0이면 백필 완료
 */
public record JobPostingEmbeddingBackfillResult(
    int processed,
    int succeeded,
    List<Long> failedJobPostingIds,
    Long lastJobPostingId,
    long remaining
) {
}
