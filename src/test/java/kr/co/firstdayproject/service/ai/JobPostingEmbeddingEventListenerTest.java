package kr.co.firstdayproject.service.ai;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobPostingEmbeddingEventListenerTest {

    @Mock
    private JobPostingEmbeddingService embeddingService;

    @InjectMocks
    private JobPostingEmbeddingEventListener listener;

    @Test
    void callsUpsertWhenShouldEmbedIsTrue() {
        listener.onJobPostingEmbeddingSync(
            new JobPostingEmbeddingSyncEvent(42L, true)
        );

        verify(embeddingService).upsert(42L);
        verify(embeddingService, never()).delete(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void callsDeleteWhenShouldEmbedIsFalse() {
        listener.onJobPostingEmbeddingSync(
            new JobPostingEmbeddingSyncEvent(42L, false)
        );

        verify(embeddingService).delete(42L);
        verify(embeddingService, never()).upsert(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void swallowsExceptionsSoAJobPostingSaveIsNeverAffected() {
        doThrow(new RuntimeException("OpenAI 호출 실패"))
            .when(embeddingService).upsert(42L);

        assertThatCode(() -> listener.onJobPostingEmbeddingSync(
            new JobPostingEmbeddingSyncEvent(42L, true)
        )).doesNotThrowAnyException();
    }
}
