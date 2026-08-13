package kr.co.firstdayproject.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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

    @InjectMocks
    private SimilarJobPostingSearchService searchService;

    @Test
    void findSimilarPostingsFiltersByCategoryAndExcludesTargetPosting() {
        Document document = new Document("유사 공고 내용");
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
            .thenReturn(List.of(document));

        List<Document> result = searchService.findSimilarPostings("자소서 답변", 104L, 42L, 3);

        assertThat(result).containsExactly(document);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest captured = captor.getValue();
        assertThat(captured.getQuery()).isEqualTo("자소서 답변");
        assertThat(captured.getTopK()).isEqualTo(3);

        Filter.Expression filterExpression = captured.getFilterExpression();
        assertThat(filterExpression).isNotNull();
        String filterText = filterExpression.toString();
        assertThat(filterText).contains("job_category_id").contains("104");
        assertThat(filterText).contains("source_id").contains("42");
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
