package kr.co.firstdayproject.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.co.firstdayproject.entity.job.JobPosting;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class JobPostingEmbeddingServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @InjectMocks
    private JobPostingEmbeddingService embeddingService;

    private String vectorId(Long jobPostingId) {
        String name = "job_posting:" + jobPostingId;
        return UUID.nameUUIDFromBytes(
            name.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }

    @Test
    void upsertDeletesThenAddsADocumentBuiltFromPostingContent() {
        JobPosting posting = JobPosting.builder()
            .jobPostingId(42L)
            .jobCategoryId(7L)
            .title("백엔드 신입 개발자")
            .mainTasks("REST API 설계 및 운영")
            .qualifications("Spring Boot 경험")
            .preferredConditions("AWS 경험")
            .introduction("함께 성장할 동료를 찾습니다")
            .build();
        when(jobPostingRepository.findById(42L))
            .thenReturn(Optional.of(posting));

        embeddingService.upsert(42L);

        verify(vectorStore).delete(List.of(vectorId(42L)));

        ArgumentCaptor<List<Document>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        Document document = captor.getValue().get(0);

        assertThat(document.getId()).isEqualTo(vectorId(42L));
        assertThat(document.getText())
            .contains("제목: 백엔드 신입 개발자")
            .contains("주요업무: REST API 설계 및 운영")
            .contains("자격요건: Spring Boot 경험")
            .contains("우대사항: AWS 경험")
            .contains("소개: 함께 성장할 동료를 찾습니다");
        assertThat(document.getMetadata())
            .containsEntry("source_type", "job_posting")
            .containsEntry("source_id", "42")
            .containsEntry("job_category_id", "7");
    }

    @Test
    void upsertOmitsJobCategoryIdFromMetadataWhenNull() {
        JobPosting posting = JobPosting.builder()
            .jobPostingId(43L)
            .jobCategoryId(null)
            .title("직무 미지정 공고")
            .build();
        when(jobPostingRepository.findById(43L))
            .thenReturn(Optional.of(posting));

        embeddingService.upsert(43L);

        ArgumentCaptor<List<Document>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());
        assertThat(captor.getValue().get(0).getMetadata())
            .doesNotContainKey("job_category_id");
    }

    @Test
    void upsertDeletesWithoutAddingWhenPostingNoLongerExists() {
        when(jobPostingRepository.findById(99L))
            .thenReturn(Optional.empty());

        embeddingService.upsert(99L);

        verify(vectorStore).delete(List.of(vectorId(99L)));
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void upsertDeletesWithoutAddingWhenAllContentFieldsAreBlank() {
        JobPosting posting = JobPosting.builder()
            .jobPostingId(44L)
            .title(null)
            .mainTasks(null)
            .qualifications(null)
            .preferredConditions(null)
            .introduction(null)
            .build();
        when(jobPostingRepository.findById(44L))
            .thenReturn(Optional.of(posting));

        embeddingService.upsert(44L);

        verify(vectorStore).delete(List.of(vectorId(44L)));
        verify(vectorStore, never()).add(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void deleteRemovesTheDeterministicVectorId() {
        embeddingService.delete(7L);

        verify(vectorStore).delete(eq(List.of(vectorId(7L))));
    }
}
