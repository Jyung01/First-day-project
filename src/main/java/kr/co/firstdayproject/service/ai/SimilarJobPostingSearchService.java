package kr.co.firstdayproject.service.ai;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 자소서 첨삭용 RAG 컨텍스트 검색.
 * 대상 채용공고는 이미 사용자가 직접 선택했으므로 여기서 다시 찾지 않고,
 * 같은 직무군(job_category_id)에서 자소서 답변과 유사한 다른 공고들을
 * "이 직무군에서 흔히 요구되는 역량" 참고 컨텍스트로만 가져온다.
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
public class SimilarJobPostingSearchService {

    private static final String SOURCE_TYPE = "job_posting";

    private final VectorStore vectorStore;

    public List<Document> findSimilarPostings(
        String queryText,
        Long jobCategoryId,
        Long excludeJobPostingId,
        int topK
    ) {
        if (jobCategoryId == null || queryText == null || queryText.isBlank()) {
            return List.of();
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        Filter.Expression filterExpression = builder.and(
            builder.and(
                builder.eq("source_type", SOURCE_TYPE),
                builder.eq("job_category_id", String.valueOf(jobCategoryId))
            ),
            builder.ne("source_id", String.valueOf(excludeJobPostingId))
        ).build();

        SearchRequest searchRequest = SearchRequest.builder()
            .query(queryText)
            .topK(topK)
            .filterExpression(filterExpression)
            .build();

        return vectorStore.similaritySearch(searchRequest);
    }
}
