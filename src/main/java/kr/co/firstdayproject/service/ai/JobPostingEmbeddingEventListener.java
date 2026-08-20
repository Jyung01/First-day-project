package kr.co.firstdayproject.service.ai;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * MySQL 트랜잭션 커밋 후에만 임베딩을 반영한다.
 * pgvector는 별도 DataSource라 같은 트랜잭션으로 묶이지 않으며,
 * 임베딩 실패가 원본 채용공고 저장을 막으면 안 되므로 예외를 삼키고 로그만 남긴다.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class JobPostingEmbeddingEventListener {

    private static final Logger log =
        LoggerFactory.getLogger(JobPostingEmbeddingEventListener.class);

    private final JobPostingEmbeddingService embeddingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobPostingEmbeddingSync(JobPostingEmbeddingSyncEvent event) {
        try {
            if (event.shouldEmbed()) {
                embeddingService.upsert(event.jobPostingId());
            } else {
                embeddingService.delete(event.jobPostingId());
            }
        } catch (Exception exception) {
            log.error(
                "채용공고 임베딩 동기화 실패: jobPostingId={}, shouldEmbed={}",
                event.jobPostingId(),
                event.shouldEmbed(),
                exception
            );
        }
    }
}
