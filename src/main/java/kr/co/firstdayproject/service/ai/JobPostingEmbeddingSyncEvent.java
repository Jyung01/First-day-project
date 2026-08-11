package kr.co.firstdayproject.service.ai;

/**
 * 채용공고의 pgvector 임베딩을 갱신해야 할 때 발행하는 이벤트.
 * shouldEmbed=true면 upsert, false면 delete.
 */
public record JobPostingEmbeddingSyncEvent(
    Long jobPostingId,
    boolean shouldEmbed
) {
}
