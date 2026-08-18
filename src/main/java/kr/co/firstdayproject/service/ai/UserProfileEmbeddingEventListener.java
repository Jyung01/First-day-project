package kr.co.firstdayproject.service.ai;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class UserProfileEmbeddingEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserProfileEmbeddingEventListener.class);
    private final PersonalizedJobRecommendationService recommendationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserProfileEmbeddingSync(UserProfileEmbeddingSyncEvent event) {
        try {
            recommendationService.upsertProfileEmbedding(event.userId());
        } catch (Exception exception) {
            // 프로필 원본 저장은 pgvector/OpenAI 일시 장애에 영향받지 않아야 한다.
            log.error("개인회원 프로필 임베딩 동기화 실패: userId={}", event.userId(), exception);
        }
    }
}
