package kr.co.firstdayproject.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

@ExtendWith(MockitoExtension.class)
class SimilarJobPostingSearchServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @InjectMocks
    private SimilarJobPostingSearchService searchService;

    private Document posting(String sourceId, String text) {
        return Document.builder()
            .text(text)
            .metadata(Map.of("source_type", "job_posting", "source_id", sourceId))
            .build();
    }

    @Test
    void findSimilarPostingsFiltersByCategoryAndExcludesTargetPosting() {
        Document document = posting("100", "유사 공고 내용");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(document));
        when(jobPostingRepository.findVisibleIdsIn(anyCollection()))
            .thenReturn(List.of(100L));

        List<Document> result = searchService.findSimilarPostings("자소서 답변", 104L, 42L, 3);

        assertThat(result).containsExactly(document);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest captured = captor.getValue();
        assertThat(captured.getQuery()).isEqualTo("자소서 답변");
        // 노출 불가 공고가 걸러질 것을 감안해 topK보다 넉넉히 요청한다.
        assertThat(captured.getTopK()).isGreaterThan(3);

        Filter.Expression filterExpression = captured.getFilterExpression();
        assertThat(filterExpression).isNotNull();
        String filterText = filterExpression.toString();
        assertThat(filterText).contains("job_category_id").contains("104");
        assertThat(filterText).contains("source_id").contains("42");
    }

    @Test
    void findSimilarPostingsExcludesPostingsThatAreNoLongerVisible() {
        Document visible = posting("100", "정상 기업의 모집중 공고");
        Document suspended = posting("200", "이용정지 기업의 공고");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(visible, suspended));
        when(jobPostingRepository.findVisibleIdsIn(anyCollection()))
            .thenReturn(List.of(100L));

        List<Document> result = searchService.findSimilarPostings("자소서 답변", 104L, 42L, 3);

        assertThat(result).containsExactly(visible);
    }

    @Test
    void findSimilarPostingsKeepsSimilarityOrderAndCutsToTopK() {
        Document first = posting("100", "가장 유사한 공고");
        Document second = posting("200", "그다음 공고");
        Document third = posting("300", "세 번째 공고");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(first, second, third));
        // 조회 결과 순서를 뒤섞어도 유사도 순서가 유지되어야 한다.
        when(jobPostingRepository.findVisibleIdsIn(anyCollection()))
            .thenReturn(List.of(300L, 100L, 200L));

        List<Document> result = searchService.findSimilarPostings("자소서 답변", 104L, 42L, 2);

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void findSimilarPostingsReturnsEmptyWhenEveryMatchIsHidden() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(posting("200", "이용정지 기업의 공고")));
        when(jobPostingRepository.findVisibleIdsIn(anyCollection()))
            .thenReturn(List.of());

        List<Document> result = searchService.findSimilarPostings("자소서 답변", 104L, 42L, 3);

        assertThat(result).isEmpty();
    }

    @Test
    void findSimilarPostingsSkipsDocumentsWithBrokenSourceId() {
        Document broken = Document.builder()
            .text("source_id가 없는 문서")
            .metadata(Map.of("source_type", "job_posting"))
            .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(broken));

        List<Document> result = searchService.findSimilarPostings("자소서 답변", 104L, 42L, 3);

        assertThat(result).isEmpty();
        verify(jobPostingRepository, never()).findVisibleIdsIn(anyCollection());
    }

    @Test
    void findSimilarPostingsReturnsEmptyWhenJobCategoryIdIsNull() {
        List<Document> result = searchService.findSimilarPostings("자소서 답변", null, 42L, 3);

        assertThat(result).isEmpty();
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    void findSimilarPostingsReturnsEmptyWhenQueryTextIsBlank() {
        List<Document> result = searchService.findSimilarPostings("  ", 104L, 42L, 3);

        assertThat(result).isEmpty();
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }
}
