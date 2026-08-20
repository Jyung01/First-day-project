package kr.co.firstdayproject.service.ai;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import kr.co.firstdayproject.repository.job.JobPostingRepository;
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
 *
 * 벡터 스토어는 "무엇과 비슷한가"에만 답하게 두고, "지금 참고해도 되는 공고인가"(마감·기업
 * 이용정지 여부)는 원본인 MySQL에 다시 물어 걸러낸다. 상태를 메타데이터로 복제하지 않는 이유는
 * 스케줄러가 매분 공고 상태를 뒤집고 관리자가 수시로 기업을 정지시켜, 복제본이 곧 낡기 때문이다.
 */
@Service
@Profile("!test")
@RequiredArgsConstructor
public class SimilarJobPostingSearchService {

    private static final String SOURCE_TYPE = "job_posting";

    /**
     * 노출 불가 공고가 걸러질 것을 감안해 벡터 검색에서 topK보다 넉넉히 가져온다.
     * 여유분으로도 모자라면 결과가 topK보다 적어지는데, 그대로 두는 게 맞다 —
     * 참고 컨텍스트가 부족한 것보다 부적격 공고를 끼워 넣는 쪽이 나쁘다.
     */
    private static final int OVER_FETCH_MULTIPLIER = 4;

    private final VectorStore vectorStore;
    private final JobPostingRepository jobPostingRepository;

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
            .topK(topK * OVER_FETCH_MULTIPLIER)
            .filterExpression(filterExpression)
            .build();

        List<Document> found = vectorStore.similaritySearch(searchRequest);
        if (found == null || found.isEmpty()) {
            return List.of();
        }

        Set<Long> visibleIds = findVisibleIds(found);

        // 원본 리스트를 filter해 유사도 순서를 그대로 유지한다.
        // visibleIds 쪽을 순회하면 순서가 깨진다.
        return found.stream()
            .filter(document -> {
                Long sourceId = parseSourceId(document);
                return sourceId != null && visibleIds.contains(sourceId);
            })
            .limit(topK)
            .toList();
    }

    private Set<Long> findVisibleIds(List<Document> documents) {
        List<Long> sourceIds = documents.stream()
            .map(this::parseSourceId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (sourceIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(jobPostingRepository.findVisibleIdsIn(sourceIds));
    }

    /** 메타데이터가 깨져 있으면 그 문서는 조용히 버린다 — 참고 컨텍스트라 첨삭을 막을 이유가 없다. */
    private Long parseSourceId(Document document) {
        Object sourceId = document.getMetadata().get("source_id");
        if (sourceId == null) {
            return null;
        }
        try {
            return Long.valueOf(sourceId.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
